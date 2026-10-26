package ru.yandex.practicum.mymarket.controller;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.dto.PagingDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

@WebFluxTest(controllers = ItemController.class)
public class ItemControllerTest {
    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void items_returnsItemsView() {
        var dto = new ItemDto(1L, "Мяч", "d", "images/ball.jpg", 100L, 0);
        var result = new ItemService.ItemPageResult(
                List.of(List.of(dto)),
                new PagingDto(5, 1, false, false)
        );

        Mockito.when(itemService.findItems(any(), any(), anyInt(), anyInt()))
                .thenReturn(Mono.just(result));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void changeCountFromList_shouldRedirect() {
        Mockito.when(cartService.changeQuantity(1L, ActionType.PLUS))
                .thenReturn(Mono.empty());

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
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals(
                        "Location",
                        "/items?search=%D0%BC%D1%8F%D1%87&sort=ALPHA&pageNumber=2&pageSize=10"
                );
    }
}