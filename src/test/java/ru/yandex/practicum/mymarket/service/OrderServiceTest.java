package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OrderServiceTest extends AbstractIntegrationTest {
    @Autowired
    private OrderService orderService;
    @Autowired
    private CartService cartService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderRepository orderRepository;

    private Long item1;
    private Long item2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        itemRepository.deleteAll();
        item1 = itemRepository.save(
                Item.builder().title("Мяч").description("d").imgPath("i.jpg").price(1000L).build()
        ).getId();
        item2 = itemRepository.save(
                Item.builder().title("Клюшка").description("d").imgPath("i.jpg").price(2000L).build()
        ).getId();
    }

    @Test
    void createOrder_shouldComputeTotalAndClearCart() {
        cartService.changeQuantity(item1, ActionType.PLUS);
        cartService.changeQuantity(item1, ActionType.PLUS);
        cartService.changeQuantity(item2, ActionType.PLUS);

        long id = orderService.createOrder();
        OrderDto dto = orderService.findById(id);

        assertThat(dto.totalSum()).isEqualTo(1000 * 2 + 2000);
        assertThat(dto.items()).hasSize(2);
        assertThat(cartItemRepository.findAll()).isEmpty();
    }

    @Test
    void createOrder_emptyCart_shouldThrow() {
        assertThatThrownBy(orderService::createOrder)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findById_missing_shouldThrow() {
        assertThatThrownBy(() -> orderService.findById(99999L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void findAll_shouldReturnNewestFirst() {
        cartService.changeQuantity(item1, ActionType.PLUS);
        long id1 = orderService.createOrder();
        cartService.changeQuantity(item2, ActionType.PLUS);
        long id2 = orderService.createOrder();

        List<OrderDto> orders = orderService.findAll();
        assertThat(orders).extracting(OrderDto::id).containsExactly(id2, id1);
    }
}
