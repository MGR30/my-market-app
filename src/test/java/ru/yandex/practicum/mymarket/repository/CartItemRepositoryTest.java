package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.support.AbstractR2dbcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CartItemRepositoryTest extends AbstractR2dbcTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Long itemId1;
    private Long itemId2;
    private Long itemId3;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(200L).build(),
                Item.builder().title("C").description("c").imgPath("c.jpg").price(300L).build()
        )).map(Item::getId).collectList().block();

        itemId1 = ids.get(0);
        itemId2 = ids.get(1);
        itemId3 = ids.get(2);

        cartItemRepository.saveAll(List.of(
                CartItem.builder().itemId(itemId1).quantity(2).build(),
                CartItem.builder().itemId(itemId2).quantity(3).build(),
                CartItem.builder().itemId(itemId3).quantity(4).build()
        )).blockLast();
    }

    @Test
    void findAllByItemIdIn_shouldReturnMatching() {
        StepVerifier.create(
                        cartItemRepository.findAllByItemIdIn(List.of(itemId1, itemId3))
                                .collectList()
                )
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items)
                            .extracting(CartItem::getItemId)
                            .containsExactlyInAnyOrder(itemId1, itemId3);
                })
                .verifyComplete();
    }
}
