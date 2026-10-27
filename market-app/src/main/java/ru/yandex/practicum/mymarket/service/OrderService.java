package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.model.OrderStatus;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;
    private final PaymentService paymentService;
    private final DatabaseClient databaseClient;
    private final TransactionalOperator txOperator;

    public Mono<Long> createOrder() {
        return cartService.findAll().collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }
                    List<Long> itemIds = cartItems.stream().map(CartItem::getItemId).toList();
                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .flatMap(items -> processOrder(cartItems, items));
                });
    }

    private Mono<Long> processOrder(List<CartItem> cartItems, Map<Long, Item> items) {
        long total = calculateTotal(cartItems, items);

        return paymentService.getBalance()
                .flatMap(balance -> {
                    if (balance < 0) {
                        return Mono.error(new IllegalStateException("Сервис платежей недоступен"));
                    }
                    if (balance < total) {
                        return Mono.error(new IllegalStateException("Недостаточно средств"));
                    }

                    return savePendingOrder(cartItems, items)
                            .flatMap(orderId -> paymentService.pay(total)
                                    .flatMap(paid -> {
                                        if (!paid) {
                                            return markOrderFailed(orderId)
                                                    .then(Mono.error(new IllegalStateException("Недостаточно средств")));
                                        }
                                        return markOrderPaidAndClearCart(orderId)
                                                .thenReturn(orderId);
                                    })
                                    .onErrorResume(PaymentService.PaymentUnavailableException.class, e ->
                                            markOrderFailed(orderId)
                                                    .then(Mono.error(new IllegalStateException(
                                                            "Сервис платежей недоступен")))));
                });
    }

    private Mono<Long> savePendingOrder(List<CartItem> cartItems, Map<Long, Item> items) {
        Order order = Order.builder()
                .createdAt(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();
        return orderRepository.save(order)
                .flatMap(saved -> {
                    List<OrderItem> orderItems = cartItems.stream()
                            .map(ci -> {
                                Item it = items.get(ci.getItemId());
                                if (it == null) return null;
                                return OrderItem.builder()
                                        .orderId(saved.getId())
                                        .itemId(it.getId())
                                        .title(it.getTitle())
                                        .price(it.getPrice())
                                        .quantity(ci.getQuantity())
                                        .build();
                            })
                            .filter(Objects::nonNull)
                            .toList();
                    if (orderItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Все товары в корзине недоступны"));
                    }
                    return orderItemRepository.saveAll(orderItems)
                            .then(Mono.just(saved.getId()));
                })
                .as(txOperator::transactional);
    }

    private Mono<Void> markOrderPaidAndClearCart(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.PAID)
                .then(cartService.clear())
                .as(txOperator::transactional);
    }

    private Mono<Void> markOrderFailed(Long orderId) {
        return updateOrderStatus(orderId, OrderStatus.FAILED)
                .as(txOperator::transactional);
    }

    private Mono<Void> updateOrderStatus(Long orderId, OrderStatus status) {
        return databaseClient.sql("UPDATE orders SET status = :status WHERE id = :id")
                .bind("status", status.name())
                .bind("id", orderId)
                .fetch().rowsUpdated()
                .then();
    }

    private long calculateTotal(List<CartItem> cartItems, Map<Long, Item> items) {
        return cartItems.stream()
                .filter(ci -> items.containsKey(ci.getItemId()))
                .mapToLong(ci -> items.get(ci.getItemId()).getPrice() * ci.getQuantity())
                .sum();
    }

    public Mono<List<OrderDto>> findAll() {
        return orderRepository.findAllByStatusOrderByIdDesc(OrderStatus.PAID).collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return Mono.just(List.of());
                    }
                    List<Long> orderIds = orders.stream().map(Order::getId).toList();
                    return orderItemRepository.findAllByOrderIdIn(orderIds)
                            .collectMultimap(OrderItem::getOrderId)
                            .map(itemsByOrder -> orders.stream()
                                    .map(order -> toDto(order,
                                            itemsByOrder.getOrDefault(order.getId(), List.of())))
                                    .toList());
                });
    }

    public Mono<OrderDto> findById(long id) {
        return orderRepository.findByIdAndStatus(id, OrderStatus.PAID)
                .switchIfEmpty(Mono.error(() -> new NoSuchElementException("Заказ не найден: " + id)))
                .flatMap(order -> orderItemRepository.findAllByOrderId(id)
                        .collectList()
                        .map(items -> toDto(order, items)));
    }

    private OrderDto toDto(Order order, Collection<OrderItem> orderItems) {
        List<ItemDto> items = orderItems.stream()
                .map(oi -> new ItemDto(
                        oi.getItemId(),
                        oi.getTitle(),
                        null,
                        null,
                        oi.getPrice(),
                        oi.getQuantity()
                ))
                .toList();

        long totalSum = orderItems.stream()
                .mapToLong(oi -> oi.getPrice() * oi.getQuantity())
                .sum();

        return new OrderDto(order.getId(), items, totalSum, order.getCreatedAt());
    }
}