package ru.yandex.practicum.mymarket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;
import java.util.Objects;

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
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReactiveRedisTemplate<String, Object> redis;

    private Long userId;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        userId = Objects.requireNonNull(userRepository.save(User.builder()
                .username("testuser").password("x").role("USER").build()).block()).getId();

        itemRepository.saveAll(List.of(
                Item.builder().title("A").description("a").imgPath("a.jpg").price(100L).build(),
                Item.builder().title("B").description("b").imgPath("b.jpg").price(300L).build(),
                Item.builder().title("C").description("c").imgPath("c.jpg").price(200L).build(),
                Item.builder().title("D").description("d").imgPath("d.jpg").price(400L).build()
        )).blockLast();
        redis.keys("*").flatMap(redis::delete).blockLast();
        stubPaymentSuccess();
    }

    @Test
    void findItems_shouldSortByPriceAsc() {
        var result = itemService.findItems(null, SortType.PRICE, 1, 10, userId).block();
        assertThat(result).isNotNull();

        List<ItemDto> flat = result.items().stream()
                .flatMap(List::stream)
                .filter(i -> i.id() != -1)
                .toList();

        assertThat(flat).extracting(ItemDto::price).containsExactly(100L, 200L, 300L, 400L);
    }

    @Test
    void findItems_shouldPaginate() {
        var first = itemService.findItems(null, SortType.NO, 1, 2, userId).block();
        assertThat(first).isNotNull();
        assertThat(first.paging().hasNext()).isTrue();
        assertThat(first.paging().hasPrevious()).isFalse();

        var second = itemService.findItems(null, SortType.NO, 2, 2, userId).block();
        assertThat(second).isNotNull();
        assertThat(second.paging().hasPrevious()).isTrue();
        assertThat(second.paging().hasNext()).isFalse();
    }

    @Test
    void findItems_shouldSearch() {
        var result = itemService.findItems("A", SortType.NO, 1, 10, userId).block();
        assertThat(result).isNotNull();

        List<ItemDto> flat = result.items().stream()
                .flatMap(List::stream)
                .filter(i -> i.id() != -1)
                .toList();

        assertThat(flat).hasSize(1);
        assertThat(flat.getFirst().title()).isEqualTo("A");
    }

    @Test
    void findItems_shouldReturnCartCount() {
        Long id = Objects.requireNonNull(itemRepository.findAll().collectList().block()).getFirst().getId();
        cartService.changeQuantity(userId, id, ActionType.PLUS).block();
        cartService.changeQuantity(userId, id, ActionType.PLUS).block();

        var result = itemService.findItems(null, SortType.NO, 1, 10, userId).block();
        assertThat(result).isNotNull();

        ItemDto dto = result.items().stream()
                .flatMap(List::stream)
                .filter(i -> i.id() == id)
                .findFirst()
                .orElseThrow();

        assertThat(dto.count()).isEqualTo(2);
    }

    @Test
    void findCartItems_shouldReturnOnlyExisting() {
        Long id = Objects.requireNonNull(itemRepository.findAll().collectList().block()).getFirst().getId();
        cartService.changeQuantity(userId, id, ActionType.PLUS).block();

        assertThat(itemService.findCartItems(userId).block()).hasSize(1);
    }
}