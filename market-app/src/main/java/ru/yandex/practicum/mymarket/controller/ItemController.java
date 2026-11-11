package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.result.view.Rendering;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.model.User;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;
import ru.yandex.practicum.mymarket.service.UserService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;
    private final UserService userService;

    @GetMapping({"/", "/items"})
    public Mono<Rendering> items(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") SortType sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            Principal principal) {

        boolean isAuth = isAuthenticated(principal);

        return userService.findOptionalByUsername(principal != null ? principal.getName() : null)
                .map(User::getId)
                .defaultIfEmpty(-1L)
                .flatMap(userId -> itemService.findItems(search, sort, pageNumber, pageSize, userId)
                        .map(result -> Rendering.view("items")
                                .modelAttribute("items", result.items())
                                .modelAttribute("search", search)
                                .modelAttribute("sort", sort.name())
                                .modelAttribute("paging", result.paging())
                                .modelAttribute("isAuthenticated", isAuth)
                                .build()));
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> item(@PathVariable long id, Principal principal) {
        boolean isAuth = isAuthenticated(principal);
        return userService.findOptionalByUsername(principal != null ? principal.getName() : null)
                .map(User::getId)
                .defaultIfEmpty(-1L)
                .flatMap(userId -> itemService.findById(id, userId)
                        .map(item -> Rendering.view("item")
                                .modelAttribute("item", item)
                                .modelAttribute("isAuthenticated", isAuth)
                                .build()));
    }

    @PostMapping("/items")
    public Mono<String> changeCountFromList(ServerWebExchange exchange, Principal principal) {
        if (!isAuthenticated(principal)) {
            return Mono.just("redirect:/login");
        }
        return userService.findByUsername(principal.getName())
                .flatMap(user -> exchange.getFormData()
                        .flatMap(form -> {
                            String idParam = form.getFirst("id");
                            String actionParam = form.getFirst("action");
                            String search = form.getFirst("search");
                            String sortParam = form.getFirst("sort");
                            String pageSizeParam = form.getFirst("pageSize");
                            String pageNumberParam = form.getFirst("pageNumber");

                            if (idParam == null || actionParam == null) {
                                return Mono.just(buildRedirect(search, sortParam, pageNumberParam, pageSizeParam));
                            }

                            long id = Long.parseLong(idParam);
                            ActionType action = ActionType.valueOf(actionParam);

                            return cartService.changeQuantity(user.getId(), id, action)
                                    .thenReturn(buildRedirect(search, sortParam, pageNumberParam, pageSizeParam));
                        }));
    }

    private String buildRedirect(String search, String sort, String pageNumber, String pageSize) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/items");
        if (search != null && !search.isBlank()) {
            b.queryParam("search", search);
        }
        b.queryParam("sort", sort != null ? sort : "NO");
        b.queryParam("pageNumber", pageNumber != null ? pageNumber : "1");
        b.queryParam("pageSize", pageSize != null ? pageSize : "5");
        return "redirect:" + b.build().encode().toUriString();
    }

    @PostMapping("/items/{id}")
    public Mono<Rendering> changeCountFromItem(
            @PathVariable long id,
            @RequestParam ActionType action,
            Principal principal) {

        if (!isAuthenticated(principal)) {
            return Mono.just(Rendering.redirectTo("/login").build());
        }
        return userService.findByUsername(principal.getName())
                .flatMap(user -> cartService.changeQuantity(user.getId(), id, action)
                        .then(itemService.findById(id, user.getId()))
                        .map(item -> Rendering.view("item")
                                .modelAttribute("item", item)
                                .modelAttribute("isAuthenticated", true)
                                .build()));
    }

    private static boolean isAuthenticated(Principal principal) {
        return principal != null && !"anonymousUser".equals(principal.getName());
    }
}
