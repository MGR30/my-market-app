package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.PaymentService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ItemService itemService;
    private final CartService cartService;
    private final PaymentService paymentService;

    @GetMapping("/items")
    public Mono<Rendering> cart() {
        return fillCart();
    }

    @PostMapping("/items")
    public Mono<Rendering> change(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(form -> {
                    String idParam = form.getFirst("id");
                    String actionParam = form.getFirst("action");

                    if (idParam == null || actionParam == null) {
                        return Mono.error(new IllegalArgumentException(
                                "Missing form fields: id=" + idParam + ", action=" + actionParam));
                    }

                    long id = Long.parseLong(idParam);
                    ActionType action = ActionType.valueOf(actionParam);

                    return cartService.changeQuantity(id, action)
                            .then(fillCart());
                });
    }

    private Mono<Rendering> fillCart() {
        return Mono.zip(itemService.findCartItems(), paymentService.getBalance())
                .map(tuple -> {
                    var items = tuple.getT1();
                    long balance = tuple.getT2();
                    long total = items.stream().mapToLong(i -> i.price() * i.count()).sum();
                    boolean canPay = balance >= 0 && balance >= total && !items.isEmpty();
                    return Rendering.view("cart")
                            .modelAttribute("items", items)
                            .modelAttribute("total", total)
                            .modelAttribute("balance", balance)
                            .modelAttribute("canPay", canPay)
                            .build();
                });
    }
}