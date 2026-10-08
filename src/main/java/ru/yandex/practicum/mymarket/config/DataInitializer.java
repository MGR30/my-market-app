package ru.yandex.practicum.mymarket.config;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Transactional
public class DataInitializer implements CommandLineRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(String @NonNull ... args) {
        if (itemRepository.count() > 0) return;
        itemRepository.saveAll(List.of(
                Item.builder().title("Мяч").description("Футбольный мяч").imgPath("images/ball.jpg").price(1000L).build(),
                Item.builder().title("Кроссовки").description("Беговые кроссовки").imgPath("images/sneakers.jpg").price(5000L).build(),
                Item.builder().title("Футболка").description("Спортивная футболка").imgPath("images/tshirt.jpg").price(1500L).build()
        ));
    }
}
