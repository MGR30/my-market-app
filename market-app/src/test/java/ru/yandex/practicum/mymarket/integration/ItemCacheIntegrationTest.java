package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

class ItemCacheIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ItemService itemService;
    @Autowired
    ItemRepository itemRepository;
    @Autowired
    CartItemRepository cartItemRepository;
    @Autowired
    ReactiveRedisTemplate<String, Object> redis;

    Long id;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        redis.keys("item:*").flatMap(redis::delete).blockLast();
        redis.keys("items:list:*").flatMap(redis::delete).blockLast();

        id = itemRepository.save(Item.builder()
                .title("Кеш-товар").description("d").imgPath("i.jpg").price(999L)
                .build()).block().getId();
    }

    @Test
    void findById_cachesResult() {
        StepVerifier.create(itemService.findById(id))
                .expectNextMatches(dto -> dto.title().equals("Кеш-товар"))
                .verifyComplete();

        Boolean exists = redis.hasKey("item:" + id).block();
        assertThat(exists).isTrue();

        itemRepository.findById(id).flatMap(it -> {
            it.setTitle("Изменено");
            return itemRepository.save(it);
        }).block();

        assertThat(itemService.findById(id).block().title()).isEqualTo("Кеш-товар");
    }
}
