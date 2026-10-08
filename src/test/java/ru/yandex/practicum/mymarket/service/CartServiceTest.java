package ru.yandex.practicum.mymarket.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;


public class CartServiceTest extends AbstractIntegrationTest {
    @Autowired
    private CartService cartService;
    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void plus_shouldCreateEntry() {
        cartService.changeCount(100L, ActionType.PLUS);

        CartItem item = cartItemRepository.findByItemId(100L).orElseThrow();
        assertThat(item.getCount()).isEqualTo(1);
    }

    @Test
    void plus_increments() {
        cartService.changeCount(200L, ActionType.PLUS);
        cartService.changeCount(200L, ActionType.PLUS);

        assertThat(cartItemRepository.findByItemId(200L).orElseThrow().getCount()).isEqualTo(2);
    }

    @Test
    void minus_toZero_shouldRemove() {
        cartService.changeCount(300L, ActionType.PLUS);
        cartService.changeCount(300L, ActionType.MINUS);

        assertThat(cartItemRepository.findByItemId(300L)).isEmpty();
    }

    @Test
    void delete_shouldRemove() {
        cartService.changeCount(400L, ActionType.PLUS);
        cartService.changeCount(400L, ActionType.DELETE);

        assertThat(cartItemRepository.findByItemId(400L)).isEmpty();
    }

    @Test
    void minus_onEmpty_shouldDoNothing() {
        cartService.changeCount(500L, ActionType.MINUS);
        assertThat(cartItemRepository.findByItemId(500L)).isEmpty();
    }
}
