package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.ItemRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;

    @Override
    public void run(ApplicationArguments args) {
        itemRepository.count()
                .flatMap(count -> {
                    if (count > 0) {
                        return Mono.empty();
                    }
                    return itemRepository.saveAll(List.of(
                            Item.builder()
                                    .title("Мяч")
                                    .description("Футбольный мяч")
                                    .imgPath("images/ball.jpg")
                                    .price(1000L)
                                    .build(),
                            Item.builder()
                                    .title("Кроссовки")
                                    .description("Беговые кроссовки")
                                    .imgPath("images/sneakers.jpg")
                                    .price(5000L)
                                    .build(),
                            Item.builder()
                                    .title("Футболка")
                                    .description("Спортивная футболка")
                                    .imgPath("images/tshirt.jpg")
                                    .price(1500L)
                                    .build()
                    )).then();
                })
                .block();
    }
}
