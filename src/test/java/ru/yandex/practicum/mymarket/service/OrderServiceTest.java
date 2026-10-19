package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Long item1;
    private Long item2;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("Мяч").description("d").imgPath("i.jpg").price(1000L).build(),
                Item.builder().title("Клюшка").description("d").imgPath("i.jpg").price(2000L).build()
        )).map(Item::getId).collectList().block();

        item1 = ids.get(0);
        item2 = ids.get(1);
    }

    @Test
    void createOrder_shouldComputeTotalAndClearCart() {
        cartService.changeQuantity(item1, ActionType.PLUS).block();
        cartService.changeQuantity(item1, ActionType.PLUS).block();
        cartService.changeQuantity(item2, ActionType.PLUS).block();

        Long id = orderService.createOrder().block();
        OrderDto dto = orderService.findById(id).block();

        assertThat(dto).isNotNull();
        assertThat(dto.totalSum()).isEqualTo(1000 * 2 + 2000);
        assertThat(dto.items()).hasSize(2);
        assertThat(cartItemRepository.findAll().collectList().block()).isEmpty();
    }

    @Test
    void createOrder_emptyCart_shouldThrow() {
        StepVerifier.create(orderService.createOrder())
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void findById_missing_shouldThrow() {
        StepVerifier.create(orderService.findById(99999L))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void findAll_shouldReturnNewestFirst() {
        cartService.changeQuantity(item1, ActionType.PLUS).block();
        Long id1 = orderService.createOrder().block();

        cartService.changeQuantity(item2, ActionType.PLUS).block();
        Long id2 = orderService.createOrder().block();

        List<OrderDto> orders = orderService.findAll().block();
        assertThat(orders).isNotNull();
        assertThat(orders).extracting(OrderDto::id).containsExactly(id2, id1);
    }
}