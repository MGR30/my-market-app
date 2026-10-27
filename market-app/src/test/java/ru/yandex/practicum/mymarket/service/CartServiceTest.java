package ru.yandex.practicum.mymarket.service;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CartServiceTest extends AbstractIntegrationTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ItemRepository itemRepository;

    private Long itemId1;
    private Long itemId2;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        List<Long> ids = itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(200L).build()
        )).map(Item::getId).collectList().block();

        itemId1 = ids.get(0);
        itemId2 = ids.get(1);
    }

    @Test
    void plus_shouldCreateEntry() {
        StepVerifier.create(cartService.changeQuantity(itemId1, ActionType.PLUS))
                .verifyComplete();

        var item = cartItemRepository.findByItemId(itemId1).block();
        assertThat(item).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(1);
    }

    @Test
    void plus_increments() {
        cartService.changeQuantity(itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(itemId1, ActionType.PLUS).block();

        var item = cartItemRepository.findByItemId(itemId1).block();
        assertThat(item).isNotNull();
        assertThat(item.getQuantity()).isEqualTo(2);
    }

    @Test
    void minus_toZero_shouldRemove() {
        cartService.changeQuantity(itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(itemId1, ActionType.MINUS).block();

        assertThat(cartItemRepository.findByItemId(itemId1).block()).isNull();
    }

    @Test
    void delete_shouldRemove() {
        cartService.changeQuantity(itemId1, ActionType.PLUS).block();
        cartService.changeQuantity(itemId1, ActionType.DELETE).block();

        assertThat(cartItemRepository.findByItemId(itemId1).block()).isNull();
    }

    @Test
    void minus_onEmpty_shouldDoNothing() {
        StepVerifier.create(cartService.changeQuantity(itemId2, ActionType.MINUS))
                .verifyComplete();

        assertThat(cartItemRepository.findByItemId(itemId2).block()).isNull();
    }
}