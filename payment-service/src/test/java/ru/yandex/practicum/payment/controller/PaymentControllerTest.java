package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.service.BalanceService;

@WebFluxTest(controllers = PaymentController.class)
@Import(PaymentControllerTest.TestBeans.class)
class PaymentControllerTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        BalanceService balanceService() {
            return new BalanceService(1000);
        }
    }

    @Autowired
    private WebTestClient client;

    @Autowired
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService.reset(1000);
    }

    @Test
    void getBalance_ok() {
        client.get().uri("/balance")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(1000);
    }

    @Test
    void pay_success() {
        client.post().uri("/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest().amount(300L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.remainingBalance").isEqualTo(700);
    }

    @Test
    void pay_insufficientFunds() {
        client.post().uri("/pay")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest().amount(999_999L))
                .exchange()
                .expectStatus().isEqualTo(409);
    }
}