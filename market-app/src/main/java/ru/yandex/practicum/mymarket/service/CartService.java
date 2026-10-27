package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.dto.ActionType;
import ru.yandex.practicum.mymarket.model.CartItem;
import ru.yandex.practicum.mymarket.repository.CartItemRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public Mono<Void> changeQuantity(long itemId, ActionType action) {
        return cartItemRepository.findByItemId(itemId)
                .flatMap(cartItem -> applyAction(cartItem, action).thenReturn(true))
                .switchIfEmpty(Mono.defer(() -> {
                    if (action == ActionType.PLUS) {
                        return create(itemId).thenReturn(true);
                    }
                    return Mono.just(false);
                }))
                .then();
    }

    private Mono<Void> applyAction(CartItem cartItem, ActionType action) {
        return switch (action) {
            case PLUS -> {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                yield cartItemRepository.save(cartItem).then();
            }
            case MINUS -> {
                int newQuantity = cartItem.getQuantity() - 1;
                if (newQuantity <= 0) {
                    yield cartItemRepository.delete(cartItem);
                }
                cartItem.setQuantity(newQuantity);
                yield cartItemRepository.save(cartItem).then();
            }
            case DELETE -> cartItemRepository.delete(cartItem);
        };
    }

    private Mono<CartItem> create(long itemId) {
        return cartItemRepository.save(
                CartItem.builder()
                        .itemId(itemId)
                        .quantity(1)
                        .build()
        );
    }

    public Flux<CartItem> findAll() {
        return cartItemRepository.findAll();
    }

    @Transactional
    public Mono<Void> clear() {
        return cartItemRepository.deleteAll();
    }
}
