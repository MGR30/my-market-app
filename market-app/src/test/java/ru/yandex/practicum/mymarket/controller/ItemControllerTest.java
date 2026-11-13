package ru.yandex.practicum.mymarket.controller;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.UserService;
import ru.yandex.practicum.mymarket.support.AbstractIntegrationTest;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;

@AutoConfigureWebTestClient
public class ItemControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;
    @MockitoBean
    private CartService cartService;
    @MockitoBean
    private UserService userService;

    @Test
    void items_anonymous_returnsView() {
        var dto = new ItemDto(1L, "Мяч", "d", "images/ball.jpg", 100L, 0);
        var result = new ItemService.ItemPageResult(
                List.of(List.of(dto)),
                new PagingDto(5, 1, false, false)
        );

        Mockito.when(userService.findOptionalByUsername(Mockito.any())).thenReturn(Mono.empty());
        Mockito.when(itemService.findItems(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @WithMockUser(username = "user1")
    void changeCountFromList_shouldRedirect() {
        User user = User.builder().id(1L).username("user1").password("x").role("USER").build();
        Mockito.when(userService.findByUsername("user1")).thenReturn(Mono.just(user));
        Mockito.when(cartService.changeQuantity(1L, 1L, ActionType.PLUS)).thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1")
                        .with("action", "PLUS")
                        .with("search", "мяч")
                        .with("sort", "ALPHA")
                        .with("pageNumber", "2")
                        .with("pageSize", "10"))
                .exchange()
                .expectStatus().is3xxRedirection();
    }

    @Test
    void changeCountFromList_anonymous_redirectsToLogin() {
        webTestClient.post()
                .uri("/items")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("id", "1").with("action", "PLUS"))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*/login.*");
    }
}