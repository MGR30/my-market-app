package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.support.AbstractR2dbcTest;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class CartItemRepositoryTest extends AbstractR2dbcTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;
    private Long anotherUserId;
    private Long itemId1;
    private Long itemId2;
    private Long itemId3;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        userId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("testuser").password("x").role("USER").build()).block()).getId();
        anotherUserId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("another").password("x").role("USER").build()).block()).getId();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(200L).build(),
                Item.builder().title("C").description("c").imgPath("c.jpg").price(300L).build()
        )).map(Item::getId).collectList().block();

        itemId1 = ids.get(0);
        itemId2 = ids.get(1);
        itemId3 = ids.get(2);

        cartItemRepository.saveAll(List.of(
                CartItem.builder().userId(userId).itemId(itemId1).quantity(2).build(),
                CartItem.builder().userId(userId).itemId(itemId2).quantity(3).build(),
                CartItem.builder().userId(userId).itemId(itemId3).quantity(4).build(),
                CartItem.builder().userId(anotherUserId).itemId(itemId1).quantity(7).build()
        )).blockLast();
    }

    @Test
    void findAllByUserIdAndItemIdIn_shouldReturnOnlyThisUserItems() {
        StepVerifier.create(
                        cartItemRepository.findAllByUserIdAndItemIdIn(userId, List.of(itemId1, itemId3))
                                .collectList()
                )
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items)
                            .extracting(CartItem::getItemId)
                            .containsExactlyInAnyOrder(itemId1, itemId3);
                    assertThat(items)
                            .allMatch(ci -> ci.getUserId().equals(userId));
                })
                .verifyComplete();
    }

    @Test
    void findByUserIdAndItemId_shouldReturnOnlyForOwner() {
        StepVerifier.create(cartItemRepository.findByUserIdAndItemId(userId, itemId1))
                .assertNext(ci -> {
                    assertThat(ci.getQuantity()).isEqualTo(2);
                    assertThat(ci.getUserId()).isEqualTo(userId);
                })
                .verifyComplete();

        StepVerifier.create(cartItemRepository.findByUserIdAndItemId(anotherUserId, itemId1))
                .assertNext(ci -> {
                    assertThat(ci.getQuantity()).isEqualTo(7);
                    assertThat(ci.getUserId()).isEqualTo(anotherUserId);
                })
                .verifyComplete();
    }

    @Test
    void sameItem_canBeInCartsOfDifferentUsers() {
        StepVerifier.create(cartItemRepository.findAllByUserId(userId).collectList())
                .assertNext(items -> assertThat(items).hasSize(3))
                .verifyComplete();

        StepVerifier.create(cartItemRepository.findAllByUserId(anotherUserId).collectList())
                .assertNext(items -> assertThat(items).hasSize(1))
                .verifyComplete();
    }

    @Test
    void deleteAllByUserId_shouldRemoveOnlyOwnItems() {
        cartItemRepository.deleteAllByUserId(userId).block();

        StepVerifier.create(cartItemRepository.findAllByUserId(userId).collectList())
                .assertNext(items -> assertThat(items).isEmpty())
                .verifyComplete();

        StepVerifier.create(cartItemRepository.findAllByUserId(anotherUserId).collectList())
                .assertNext(items -> assertThat(items).hasSize(1))
                .verifyComplete();
    }
}
