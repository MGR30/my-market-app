package ru.yandex.practicum.mymarket.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.support.AbstractJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ItemRepositoryTest extends AbstractJpaTest {

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase_shouldFindByTitle() {
        itemRepository.saveAll(List.of(
                Item.builder().title("Мяч").description("Футбольный").imgPath("i1").price(100L).build(),
                Item.builder().title("Мяч баскетбольный").description("Спортивный").imgPath("i2").price(200L).build(),
                Item.builder().title("Клюшка").description("Хоккейная").imgPath("i3").price(300L).build()
        ));

        Page<Item> page = itemRepository
                .findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        "мяч", "мяч", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Item::getTitle)
                .containsExactlyInAnyOrder("Мяч", "Мяч баскетбольный");
    }
}
