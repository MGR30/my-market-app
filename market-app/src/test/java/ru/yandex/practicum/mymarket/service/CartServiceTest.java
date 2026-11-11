package ru.yandex.practicum.mymarket.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class CartServiceTest extends AbstractIntegrationTest {

    @Autowired private CartService cartService;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ItemRepository itemRepository;
    @Autowired private UserRepository userRepository;

    private Long userId;
    private Long itemId1;
    private Long itemId2;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        userId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("testuser").password("x").role("USER").build()).block()).getId();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(200L).build()
        )).map(Item::getId).collectList().block();

        assert ids != null;
        itemId1 = ids.get(0);
        itemId2 = ids.get(1);
    }

    @Test
    void plus_shouldCreateEntry() {
        StepVerifier.create(cartService.changeQuantity(userId, itemId1, ActionType.PLUS))
                .verifyComplete();

        var item = cartItemRepository.findByUserIdAndItemId(userId, itemId1).block();
        assertThat(item).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    void plus_increments() {
        cartService.changeQuantity(userId, itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(userId, itemId1, ActionType.PLUS).block();

        var item = cartItemRepository.findByUserIdAndItemId(userId, itemId1).block();
        assertThat(item).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void minus_toZero_shouldRemove() {
        cartService.changeQuantity(userId, itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(userId, itemId1, ActionType.MINUS).block();

        assertThat(cartItemRepository.findByUserIdAndItemId(userId, itemId1).block()).isNull();
    }

    @Test
    void delete_shouldRemove() {
        cartService.changeQuantity(userId, itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(userId, itemId1, ActionType.DELETE).block();

        assertThat(cartItemRepository.findByUserIdAndItemId(userId, itemId1).block()).isNull();
    }

    @Test
    void minus_onEmpty_shouldDoNothing() {
        StepVerifier.create(cartService.changeQuantity(userId, itemId2, ActionType.MINUS))
                .verifyComplete();

        assertThat(cartItemRepository.findByUserIdAndItemId(userId, itemId2).block()).isNull();
    }

    @Test
    void users_haveSeparateCarts() {
        Long userId2 = Objects.requireNonNull(userRepository.save(User.builder()
                .username("testuser2").password("x").role("USER").build()).block()).getId();

        cartService.changeQuantity(userId, itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(userId2, itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(userId2, itemId1, ActionType.PLUS).block();

        assertThat(Objects.requireNonNull(cartItemRepository.findByUserIdAndItemId(userId, itemId1).block()).getQuantity()).isEqualTo(1);
        assertThat(Objects.requireNonNull(cartItemRepository.findByUserIdAndItemId(userId2, itemId1).block()).getQuantity()).isEqualTo(2);
    }
}