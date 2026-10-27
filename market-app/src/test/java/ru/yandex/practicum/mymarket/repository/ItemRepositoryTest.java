package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.support.AbstractR2dbcTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemRepositoryTest extends AbstractR2dbcTest {

    @Autowired
    private ItemRepository itemRepository;

    @BeforeEach
    void setUp() {
        itemRepository.deleteAll().block();
    }

    @Test
    void saveAndFindAll_shouldWork() {
        itemRepository.saveAll(List.of(
                Item.builder().title("Мяч").description("Футбольный").imgPath("i1").price(100L).build(),
                Item.builder().title("Мяч баскетбольный").description("Спортивный").imgPath("i2").price(200L).build()
        )).blockLast();

        StepVerifier.create(itemRepository.findAll().collectList())
                .assertNext(items -> {
                    assertThat(items).hasSize(2);
                    assertThat(items)
                            .extracting(Item::getTitle)
                            .containsExactlyInAnyOrder("Мяч", "Мяч баскетбольный");
                })
                .verifyComplete();
    }
}
