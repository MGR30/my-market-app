package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.support.AbstractJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CartItemRepositoryTest extends AbstractJpaTest {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Test
    void findAllByItemIdIn_shouldReturnMatching() {
        cartItemRepository.saveAll(List.of(
                CartItem.builder().itemId(1L).count(2).build(),
                CartItem.builder().itemId(2L).count(3).build(),
                CartItem.builder().itemId(3L).count(4).build()
        ));

        assertThat(cartItemRepository.findAllByItemIdIn(List.of(1L, 3L)))
                .hasSize(2)
                .extracting(CartItem::getItemId)
                .containsExactlyInAnyOrder(1L, 3L);
    }
}
