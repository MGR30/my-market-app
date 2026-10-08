package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderItem;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final CartService cartService;

    @Transactional
    public long createOrder() {
        List<CartItem> cartItems = cartService.findAll();
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("Корзина пуста");
        }

        Map<Long, Item> items = itemRepository.findAllById(
                        cartItems.stream().map(CartItem::getItemId).toList())
                .stream().collect(Collectors.toMap(Item::getId, Function.identity()));

        Order order = Order.builder().items(new ArrayList<>()).build();
        for (CartItem cartItem : cartItems) {
            Item item = items.get(cartItem.getItemId());
            if (item == null) {
                log.warn("Товар {} отсутствует, пропущен при оформлении заказа", cartItem.getItemId());
                continue;
            }
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .itemId(item.getId())
                    .title(item.getTitle())
                    .price(item.getPrice())
                    .count(cartItem.getCount())
                    .build();
            order.getItems().add(oi);
        }

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Все товары в корзине недоступны");
        }

        Order saved = orderRepository.save(order);
        cartService.clear();
        log.info("Создан заказ id={}, позиций={}", saved.getId(), saved.getItems().size());
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findAll() {
        return orderRepository.findAllByOrderByIdDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDto findById(long id) {
        return orderRepository.findWithItemsById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NoSuchElementException("Заказ не найден: " + id));
    }

    private OrderDto toDto(Order order) {
        List<ItemDto> items = order.getItems().stream()
                .map(oi -> new ItemDto(oi.getItemId(), oi.getTitle(), null, null,
                        oi.getPrice(), oi.getCount()))
                .toList();
        return new OrderDto(order.getId(), items, order.getTotalSum());
    }
}
