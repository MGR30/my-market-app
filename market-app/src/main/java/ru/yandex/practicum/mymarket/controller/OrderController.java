package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.service.OrderService;
import ru.yandex.practicum.mymarket.service.UserService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/orders")
    public Mono<Rendering> orders(Principal principal) {
        return userService.findByUsername(principal.getName())
                .flatMap(user -> orderService.findAllByUserId(user.getId())
                        .map(orders -> Rendering.view("orders")
                                .modelAttribute("orders", orders)
                                .modelAttribute("isAuthenticated", true)
                                .build()));
    }

    @GetMapping("/orders/{id}")
    public Mono<Rendering> order(
            @PathVariable long id,
            @RequestParam(defaultValue = "false") boolean newOrder,
            Principal principal) {

        return userService.findByUsername(principal.getName())
                .flatMap(user -> orderService.findByIdAndUserId(id, user.getId())
                        .map(order -> Rendering.view("order")
                                .modelAttribute("order", order)
                                .modelAttribute("newOrder", newOrder)
                                .modelAttribute("isAuthenticated", true)
                                .build()));
    }

    @PostMapping("/buy")
    public Mono<String> buy(Principal principal) {
        return userService.findByUsername(principal.getName())
                .flatMap(user -> orderService.createOrder(user.getId())
                        .map(id -> "redirect:/orders/" + id + "?newOrder=true"));
    }
}
