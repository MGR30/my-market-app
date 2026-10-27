package ru.yandex.practicum.mymarket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.yandex.practicum.payment.client.api.PaymentApi;
import ru.yandex.practicum.payment.client.model.PaymentRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentApi paymentApi;

    public Mono<Long> getBalance() {
        return paymentApi.getBalance()
                .map(b -> b.getBalance())
                .doOnError(e -> log.warn("Payment service unavailable: {}", e.getMessage()))
                .onErrorReturn(-1L);
    }

    public Mono<Boolean> pay(long amount) {
        var req = new PaymentRequest().amount(amount);
        return paymentApi.pay(req)
                .map(resp -> Boolean.TRUE.equals(resp.getSuccess()))
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 409) {
                        return Mono.just(false);
                    }
                    return Mono.error(new PaymentUnavailableException("Сервис платежей недоступен", e));
                })
                .onErrorResume(WebClientRequestException.class,
                        e -> Mono.error(new PaymentUnavailableException("Сервис платежей недоступен", e)));
    }

    public static class PaymentUnavailableException extends RuntimeException {
        public PaymentUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}