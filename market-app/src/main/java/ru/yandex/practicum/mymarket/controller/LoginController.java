package ru.yandex.practicum.mymarket.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import reactor.core.publisher.Mono;

@Controller
public class LoginController {

    @GetMapping("/login")
    public Mono<Rendering> login(@RequestParam(required = false) String error,
                                 @RequestParam(required = false) String logout) {
        return Mono.just(Rendering.view("login")
                .modelAttribute("error", error != null)
                .modelAttribute("logout", logout != null)
                .build());
    }
}