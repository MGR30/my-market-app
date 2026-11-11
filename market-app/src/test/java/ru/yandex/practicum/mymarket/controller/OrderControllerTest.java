package ru.yandex.practicum.mymarket.controller;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.OrderDto;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.time.LocalDateTime;
import java.util.List;

@AutoConfigureWebTestClient
public class OrderControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private UserService userService;

    @Test
    void orders_anonymous_redirectsToLogin() {
        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }

    @Test
    @WithMockUser(username = "user1")
    void orders_returnsView() {
        User user = User.builder().id(1L).username("user1").password("x").role("USER").build();
        Mockito.when(userService.findByUsername("user1")).thenReturn(Mono.just(user));
        Mockito.when(orderService.findAllByUserId(1L)).thenReturn(Mono.just(List.of(
                new OrderDto(1L, List.of(), 0L, LocalDateTime.now())
        )));

        webTestClient.get()
                .uri("/orders")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    @WithMockUser(username = "user1")
    void order_returnsView() {
        User user = User.builder().id(1L).username("user1").password("x").role("USER").build();
        Mockito.when(userService.findByUsername("user1")).thenReturn(Mono.just(user));
        Mockito.when(orderService.findByIdAndUserId(1L, 1L))
                .thenReturn(Mono.just(new OrderDto(1L, List.of(), 0L, LocalDateTime.now())));

        webTestClient.get()
                .uri("/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    @WithMockUser(username = "user1")
    void buy_shouldRedirectWithNewOrder() {
        User user = User.builder().id(1L).username("user1").password("x").role("USER").build();
        Mockito.when(userService.findByUsername("user1")).thenReturn(Mono.just(user));
        Mockito.when(orderService.createOrder(1L)).thenReturn(Mono.just(42L));

        webTestClient.post()
                .uri("/buy")
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/orders/42?newOrder=true");
    }
}
