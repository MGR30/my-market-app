package ru.yandex.practicum.mymarket.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import ru.yandex.practicum.mymarket.model.Item;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;
import ru.yandex.practicum.mymarket.repository.ItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderItemRepository;
import ru.yandex.practicum.mymarket.repository.OrderRepository;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

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

    private Long itemId;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll().block();
        orderRepository.deleteAll().block();
        cartItemRepository.deleteAll().block();
        itemRepository.deleteAll().block();

        itemId = itemRepository.save(
                Item.builder()
                        .title("Мяч")
                        .description("Футбольный")
                        .imgPath("i.jpg")
                        .price(1000L)
                        .build()
        ).block().getId();
        stubPaymentSuccess();
    }

    @Test
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

        String location = webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getLocation()
                .toString();

        assertThat(location).matches("/orders/\\d+\\?newOrder=true");
        assertThat(cartItemRepository.findAll().collectList().block()).isEmpty();
        assertThat(orderRepository.findAll().collectList().block()).hasSize(1);

        webTestClient.get()
                .uri(location)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertThat(html).contains("Сумма"));
    }
}