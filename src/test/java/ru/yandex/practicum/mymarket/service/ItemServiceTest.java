package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemServiceTest extends AbstractIntegrationTest {
    @Autowired
    private ItemService itemService;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        itemRepository.deleteAll();
        itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(300L).build(),
                Item.builder().title("C").description("c").imgPath("c.jpg").price(200L).build(),
                Item.builder().title("D").description("d").imgPath("d.jpg").price(400L).build()
        ));
    }

    @Test
    void findItems_shouldSortByPriceAsc() {
        var result = itemService.findItems(null, SortType.PRICE, 1, 10);
        List<ItemDto> flat = result.items().stream().flatMap(List::stream)
                .filter(i -> i.id() != -1).toList();
        assertThat(flat).extracting(ItemDto::price).containsExactly(100L, 200L, 300L, 400L);
    }

    @Test
    void findItems_shouldPaginate() {
        var result = itemService.findItems(null, SortType.NO, 1, 2);
        assertThat(result.paging().hasNext()).isTrue();
        assertThat(result.paging().hasPrevious()).isFalse();

        var second = itemService.findItems(null, SortType.NO, 2, 2);
        assertThat(second.paging().hasPrevious()).isTrue();
        assertThat(second.paging().hasNext()).isFalse();
    }

    @Test
    void findItems_shouldSearch() {
        var result = itemService.findItems("A", SortType.NO, 1, 10);
        List<ItemDto> flat = result.items().stream().flatMap(List::stream)
                .filter(i -> i.id() != -1).toList();
        assertThat(flat).hasSize(1);
        assertThat(flat.get(0).title()).isEqualTo("A");
    }

    @Test
    void findItems_shouldReturnCartCount() {
        Long id = itemRepository.findAll().get(0).getId();
        cartService.changeQuantity(id, ActionType.PLUS);
        cartService.changeQuantity(id, ActionType.PLUS);

        var result = itemService.findItems(null, SortType.NO, 1, 10);
        ItemDto dto = result.items().stream().flatMap(List::stream)
                .filter(i -> i.id() == id).findFirst().orElseThrow();
        assertThat(dto.count()).isEqualTo(2);
    }

    @Test
    void findCartItems_shouldReturnOnlyExisting() {
        Long id = itemRepository.findAll().get(0).getId();
        cartService.changeQuantity(id, ActionType.PLUS);
        assertThat(itemService.findCartItems()).hasSize(1);
    }
}
