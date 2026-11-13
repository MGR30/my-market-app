package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
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

        String username = principal != null ? principal.getName() : null;
        return userService.findOptionalByUsername(username)
                .map(User::getId)
                .flatMap(userId -> loadItems(search, sort, pageNumber, pageSize, userId))
                .switchIfEmpty(Mono.defer(() ->
                        loadItems(search, sort, pageNumber, pageSize, null)));
    }

    @GetMapping("/items/{id}")
    public Mono<Rendering> item(@PathVariable long id, Principal principal) {
        String username = principal != null ? principal.getName() : null;
        return userService.findOptionalByUsername(username)
                .map(User::getId)
                .flatMap(userId -> loadItem(id, userId))
                .switchIfEmpty(Mono.defer(() -> loadItem(id, null)));
    }

    @PostMapping("/items")
    public Mono<String> changeCountFromList(ServerWebExchange exchange, Principal principal) {
        return userService.findByUsername(principal.getName())
                .flatMap(user -> exchange.getFormData()
                        .flatMap(form -> {
                            String idParam = form.getFirst("id");
                            String actionParam = form.getFirst("action");
                            if (idParam == null || actionParam == null) {
                                return Mono.just(buildRedirect(form));
                            }
                            long id = Long.parseLong(idParam);
                            ActionType action = ActionType.valueOf(actionParam);
                            return cartService.changeQuantity(user.getId(), id, action)
                                    .thenReturn(buildRedirect(form));
                        }));
    }

    @PostMapping("/items/{id}")
    public Mono<Rendering> changeCountFromItem(
            @PathVariable long id,
            @RequestParam ActionType action,
            Principal principal) {
        return userService.findByUsername(principal.getName())
                .flatMap(user -> cartService.changeQuantity(user.getId(), id, action)
                        .then(loadItem(id, user.getId())));
    }

    private Mono<Rendering> loadItems(String search, SortType sort, int pageNumber,
                                      int pageSize, Long userId) {
        return itemService.findItems(search, sort, pageNumber, pageSize, userId)
                .map(result -> Rendering.view("items")
                        .modelAttribute("items", result.items())
                        .modelAttribute("search", search)
                        .modelAttribute("sort", sort.name())
                        .modelAttribute("paging", result.paging())
                        .modelAttribute("isAuthenticated", userId != null)
                        .build());
    }

    private Mono<Rendering> loadItem(long id, Long userId) {
        return itemService.findById(id, userId)
                .map(item -> Rendering.view("item")
                        .modelAttribute("item", item)
                        .modelAttribute("isAuthenticated", userId != null)
                        .build());
    }

    private String buildRedirect(MultiValueMap<String, String> form) {
        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/items");
        String search = form.getFirst("search");
        if (search != null && !search.isBlank()) {
            b.queryParam("search", search);
        }
        String sort = form.getFirst("sort");
        b.queryParam("sort", sort != null ? sort : "NO");
        String pageNumber = form.getFirst("pageNumber");
        b.queryParam("pageNumber", pageNumber != null ? pageNumber : "1");
        String pageSize = form.getFirst("pageSize");
        b.queryParam("pageSize", pageSize != null ? pageSize : "5");
        return "redirect:" + b.build().encode().toUriString();
    }
}
