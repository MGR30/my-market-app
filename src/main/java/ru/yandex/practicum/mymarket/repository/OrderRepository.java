package ru.yandex.practicum.mymarket.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import ru.yandex.practicum.mymarket.model.Order;

public interface OrderRepository extends ReactiveCrudRepository<Order, Long> {
    @Query("SELECT * FROM orders ORDER BY id DESC")
    Flux<Order> findAllNewestFirst();
}
