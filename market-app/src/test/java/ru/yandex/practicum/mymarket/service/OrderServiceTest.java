package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.*;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReactiveRedisTemplate<String, Object> redis;

    private Long userId;
    private Long item1;
    private Long item2;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        userId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("testuser").password("x").role("USER").build()).block()).getId();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("Мяч").description("d").imgPath("i.jpg").price(1000L).build(),
                Item.builder().title("Клюшка").description("d").imgPath("i.jpg").price(2000L).build()
        )).map(Item::getId).collectList().block();

        item1 = ids.get(0);
        item2 = ids.get(1);
        redis.keys("*").flatMap(redis::delete).blockLast();
        stubPaymentSuccess();
    }

    @Test
    void createOrder_shouldComputeTotalAndClearCart() {
        cartService.changeQuantity(userId, item1, ActionType.PLUS).block();
        cartService.changeQuantity(userId, item1, ActionType.PLUS).block();
        cartService.changeQuantity(userId, item2, ActionType.PLUS).block();

        Long id = orderService.createOrder(userId).block();
        OrderDto dto = orderService.findByIdAndUserId(id, userId).block();

        assertThat(dto).isNotNull();
        assertThat(dto.totalSum()).isEqualTo(1000 * 2 + 2000);
        assertThat(dto.items()).hasSize(2);
        assertThat(cartItemRepository.findAllByUserId(userId).collectList().block()).isEmpty();
    }

    @Test
    void createOrder_emptyCart_shouldThrow() {
        StepVerifier.create(orderService.createOrder(userId))
                .expectError(IllegalStateException.class)
                .verify();
    }

    @Test
    void findById_missing_shouldThrow() {
        StepVerifier.create(orderService.findByIdAndUserId(99999L, userId))
                .expectError(NoSuchElementException.class)
                .verify();
    }

    @Test
    void findAll_shouldReturnNewestFirst() {
        cartService.changeQuantity(userId, item1, ActionType.PLUS).block();
        Long id1 = orderService.createOrder(userId).block();

        cartService.changeQuantity(userId, item2, ActionType.PLUS).block();
        Long id2 = orderService.createOrder(userId).block();

        List<OrderDto> orders = orderService.findAllByUserId(userId).block();
        assertThat(orders).isNotNull();
        assertThat(orders).extracting(OrderDto::id).containsExactly(id2, id1);
    }

    @Test
    void otherUser_cannotSeeOrder() {
        cartService.changeQuantity(userId, item1, ActionType.PLUS).block();
        Long id = orderService.createOrder(userId).block();

        Long anotherUserId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("other").password("x").role("USER").build()).block()).getId();

        StepVerifier.create(orderService.findByIdAndUserId(id, anotherUserId))
                .expectError(NoSuchElementException.class)
                .verify();
    }
}