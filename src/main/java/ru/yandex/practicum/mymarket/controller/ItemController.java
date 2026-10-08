package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.SortType;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"/", "/items"})
    public String items(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") SortType sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            Model model) {

        var result = itemService.findItems(search, sort, pageNumber, pageSize);
        model.addAttribute("items", result.items());
        model.addAttribute("search", search);
        model.addAttribute("sort", sort.name());
        model.addAttribute("paging", result.paging());
        return "items";
    }

    @GetMapping("/items/{id}")
    public String item(@PathVariable long id, Model model) {
        model.addAttribute("item", itemService.findById(id));
        return "item";
    }

    @PostMapping("/items")
    public String changeCountFromList(
            @RequestParam long id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") SortType sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            @RequestParam ActionType action) {

        cartService.changeCount(id, action);

        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/items");
        if (search != null && !search.isBlank()) {
            builder.queryParam("search", search);
        }
        builder.queryParam("sort", sort.name())
                .queryParam("pageNumber", pageNumber)
                .queryParam("pageSize", pageSize);

        return "redirect:" + builder.build().encode().toUriString();
    }

    @PostMapping("/items/{id}")
    public String changeCountFromItem(
            @PathVariable long id,
            @RequestParam ActionType action,
            Model model) {
        cartService.changeCount(id, action);
        model.addAttribute("item", itemService.findById(id));
        return "item";
    }
}
