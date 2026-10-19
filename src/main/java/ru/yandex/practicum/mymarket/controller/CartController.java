package ru.yandex.practicum.mymarket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.dto.ItemDto;
import ru.yandex.practicum.mymarket.service.CartService;
import ru.yandex.practicum.mymarket.service.ItemService;

import java.util.List;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping("/items")
    public String cart(Model model) {
        fillCart(model);
        return "cart";
    }

    @PostMapping("/items")
    public String change(@RequestParam long id, @RequestParam ActionType action, Model model) {
        cartService.changeQuantity(id, action);
        fillCart(model);
        return "cart";
    }

    private void fillCart(Model model) {
        List<ItemDto> items = itemService.findCartItems();
        long total = items.stream().mapToLong(i -> i.price() * i.count()).sum();
        model.addAttribute("items", items);
        model.addAttribute("total", total);
    }
}
