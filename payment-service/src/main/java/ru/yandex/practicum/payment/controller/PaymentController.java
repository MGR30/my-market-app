package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import ru.yandex.practicum.payment.api.PaymentApi;
import ru.yandex.practicum.payment.api.model.BalanceResponse;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.api.model.PaymentResponse;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;
import ru.yandex.practicum.payment.service.BalanceService;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {
    private final BalanceService balanceService;

    @Override
    public Mono<ResponseEntity<BalanceResponse>> getBalance(Long userId, ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> ResponseEntity.ok(
                new BalanceResponse().balance(balanceService.getBalance(userId))
        ));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> pay(Long userId,
                                                     Mono<PaymentRequest> body,
                                                     ServerWebExchange exchange) {
        return body.flatMap(req ->
                Mono.fromCallable(() -> balanceService.debit(userId, req.getAmount()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(remaining -> ResponseEntity.ok(
                                new PaymentResponse().success(true).remainingBalance(remaining)))
                        .onErrorResume(InsufficientFundsException.class, e ->
                                Mono.just(ResponseEntity.status(409).body(
                                        new PaymentResponse().success(false)
                                                .remainingBalance(e.getBalance()))))
        );
    }
}
