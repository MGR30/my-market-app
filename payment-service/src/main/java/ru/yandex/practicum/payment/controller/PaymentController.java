package ru.yandex.practicum.payment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
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
    public Mono<ResponseEntity<BalanceResponse>> getBalance(ServerWebExchange exchange) {
        return Mono.fromSupplier(() -> ResponseEntity.ok(
                new BalanceResponse().balance(balanceService.getBalance())
        ));
    }

    @Override
    public Mono<ResponseEntity<PaymentResponse>> pay(Mono<PaymentRequest> body, ServerWebExchange exchange) {
        return body.flatMap(req -> {
            try {
                long remaining = balanceService.debit(req.getAmount());
                return Mono.just(ResponseEntity.ok(
                        new PaymentResponse().success(true).remainingBalance(remaining)
                ));
            } catch (InsufficientFundsException e) {
                return Mono.just(ResponseEntity.status(409).body(
                        new PaymentResponse().success(false).remainingBalance(e.getBalance())
                ));
            }
        });
    }
}
