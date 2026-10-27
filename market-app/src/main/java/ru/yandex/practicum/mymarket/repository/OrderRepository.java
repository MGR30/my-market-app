package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.mymarket.model.Order;
import ru.yandex.practicum.mymarket.model.OrderStatus;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    Flux<Order> findAllByStatusOrderByIdDesc(OrderStatus status);

    Mono<Order> findByIdAndStatus(Long id, OrderStatus status);
}
