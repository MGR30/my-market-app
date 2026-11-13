package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.repository.UserRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
public class BuyFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private UserRepository userRepository;

    private Long itemId;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();
        userRepository.deleteAll().block();

        userRepository.save(User.builder()
                .username("user1").password("x").role("USER").build()).block();

        itemId = Objects.requireNonNull(itemRepository.save(
                Item.builder()
                        .title("Мяч")
                        .description("Футбольный")
                        .imgPath("i.jpg")
                        .price(1000L)
                        .build()
        ).block()).getId();
        stubPaymentSuccess();
    }

    @Test
    void anonymous_cannotAddToCart() {
        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", itemId.toString())
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }

    @Test
    void anonymous_canViewItemsAndItem() {
        webTestClient.get().uri("/items")
                .exchange()
                .expectStatus().isOk();

        webTestClient.get().uri("/items/" + itemId)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void anonymous_cannotAccessCart() {
        webTestClient.get().uri("/cart/items")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().value("Location", loc -> assertThat(loc).contains("/login"));
    }

    @Test
    @WithMockUser(username = "user1")
    void fullBuyFlow() {
        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", itemId.toString())
                        .with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection();

        webTestClient.get()
                .uri("/cart/items")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Мяч"));

        String location = Objects.requireNonNull(webTestClient.post()
                        .uri("/buy")
                        .exchange()
                        .expectStatus().is3xxRedirection()
                        .returnResult(Void.class)
                        .getResponseHeaders()
                        .getLocation())
                .toString();

        assertThat(location).matches(".*/orders/\\d+\\?newOrder=true");

        webTestClient.get()
                .uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Сумма"));
    }
}