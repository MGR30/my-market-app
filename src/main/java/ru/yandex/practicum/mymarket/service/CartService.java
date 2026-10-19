package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public void changeQuantity(long itemId, ActionType action) {
        CartItem cartItem = cartItemRepository.findByItemId(itemId).orElse(null);

        switch (action) {
            case PLUS -> {
                if (cartItem == null) {
                    cartItem = CartItem.builder().itemId(itemId).quantity(1).build();
                } else {
                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                }
                cartItemRepository.save(cartItem);
            }
            case MINUS -> {
                if (cartItem == null) return;
                int newQuantity = cartItem.getQuantity() - 1;
                if (newQuantity <= 0) {
                    cartItemRepository.delete(cartItem);
                } else {
                    cartItem.setQuantity(newQuantity);
                    cartItemRepository.save(cartItem);
                }
            }
            case DELETE -> {
                if (cartItem != null) cartItemRepository.delete(cartItem);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CartItem> findAll() {
        return cartItemRepository.findAll();
    }

    @Transactional
    public void clear() {
        cartItemRepository.deleteAll();
    }
}
