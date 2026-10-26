package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;

    @Transactional
    public Mono<Long> createOrder() {
        return cartService.findAll().collectList()
                .flatMap(cartItems -> {
                    if (cartItems.isEmpty()) {
                        return Mono.error(new IllegalStateException("Корзина пуста"));
                    }

                    List<Long> itemIds = cartItems.stream()
                            .map(CartItem::getItemId)
                            .toList();

                    return itemRepository.findAllById(itemIds)
                            .collectMap(Item::getId)
                            .flatMap(items -> {
                                Order order = Order.builder()
                                        .createdAt(LocalDateTime.now())
                                        .build();

                                return orderRepository.save(order)
                                        .flatMap(savedOrder -> {
                                            List<OrderItem> orderItems = cartItems.stream()
                                                    .map(cartItem -> {
                                                        Item item = items.get(cartItem.getItemId());
                                                        if (item == null) {
                                                            log.warn("Товар {} отсутствует, пропущен", cartItem.getItemId());
                                                            return null;
                                                        }
                                                        return OrderItem.builder()
                                                                .orderId(savedOrder.getId())
                                                                .itemId(item.getId())
                                                                .title(item.getTitle())
                                                                .price(item.getPrice())
                                                                .quantity(cartItem.getQuantity())
                                                                .build();
                                                    })
                                                    .filter(Objects::nonNull)
                                                    .toList();

                                            if (orderItems.isEmpty()) {
                                                return Mono.error(new IllegalStateException("Все товары в корзине недоступны"));
                                            }

                                            return orderItemRepository.saveAll(orderItems)
                                                    .then(cartService.clear())
                                                    .thenReturn(savedOrder.getId());
                                        });
                            });
                });
    }

    public Mono<List<OrderDto>> findAll() {
        return orderRepository.findAllNewestFirst().collectList()
                .flatMap(orders -> {
                    if (orders.isEmpty()) {
                        return Mono.just(List.of());
                    }

                    List<Long> orderIds = orders.stream().map(Order::getId).toList();

                    return orderItemRepository.findAllByOrderIdIn(orderIds)
                            .collectMultimap(OrderItem::getOrderId)
                            .map(itemsByOrder -> orders.stream()
                                    .map(order -> toDto(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
                                    .toList());
                });
    }

    public Mono<OrderDto> findById(long id) {
        return orderRepository.findById(id)
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
