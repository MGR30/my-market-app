package ru.yandex.practicum.mymarket.config;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        userRepository.count()
                .flatMap(count -> {
                    if (count > 0) return Mono.empty();
                    return userRepository.saveAll(List.of(
                            User.builder()
                                    .username("user1")
                                    .password(passwordEncoder.encode("password1"))
                                    .role("USER")
                                    .build(),
                            User.builder()
                                    .username("user2")
                                    .password(passwordEncoder.encode("password2"))
                                    .role("USER")
                                    .build()
                    )).then();
                })
                .then(itemRepository.count())
                .flatMap(count -> {
                    if (count > 0) return Mono.empty();
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
