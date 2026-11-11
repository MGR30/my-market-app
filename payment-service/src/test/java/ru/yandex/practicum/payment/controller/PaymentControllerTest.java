package ru.yandex.practicum.payment.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import ru.yandex.practicum.payment.api.model.PaymentRequest;
import ru.yandex.practicum.payment.config.SecurityConfig;
import ru.yandex.practicum.payment.service.BalanceService;

@WebFluxTest(controllers = PaymentController.class)
@Import({SecurityConfig.class, PaymentControllerTest.TestBeans.class})
class PaymentControllerTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        BalanceService balanceService() {
            return new BalanceService(1000);
        }
    }

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Autowired
    private WebTestClient client;

    @Autowired
    private BalanceService balanceService;

    @BeforeEach
    void setUp() {
        balanceService.reset(1L, 1000);
        balanceService.reset(2L, 1000);
    }

    @Test
    void getBalance_unauthorized() {
        client.get().uri("/balance/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @WithMockUser
    void getBalance_ok() {
        client.get().uri("/balance/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.balance").isEqualTo(1000);
    }

    @Test
    @WithMockUser
    void pay_success() {
        client.post().uri("/pay/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest().amount(300L))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.remainingBalance").isEqualTo(700);
    }

    @Test
    @WithMockUser
    void pay_insufficientFunds() {
        client.post().uri("/pay/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest().amount(999_999L))
                .exchange()
                .expectStatus().isEqualTo(409);
    }

    @Test
    void pay_unauthorized() {
        client.post().uri("/pay/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new PaymentRequest().amount(100L))
                .exchange()
                .expectStatus().isUnauthorized();
    }
}