package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping("/items")
    public Mono<Rendering> cart() {
        return fillCart();
    }

    @PostMapping("/items")
    public Mono<Rendering> change(@RequestParam long id,
                                  @RequestParam ActionType action) {
        return cartService.changeQuantity(id, action)
                .then(fillCart());
    }

    private Mono<Rendering> fillCart() {
        return itemService.findCartItems()
                .map(items -> {
                    long total = items.stream()
                            .mapToLong(i -> i.price() * i.count())
                            .sum();

                    return Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("total", total)
                            .build();
                });
    }
}
