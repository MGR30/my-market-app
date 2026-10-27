package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceServiceTest {

    @Test
    void debit_decreasesBalance() {
        var svc = new BalanceService(1000);
        assertThat(svc.debit(300)).isEqualTo(700);
    }

    @Test
    void debit_insufficient() {
        var svc = new BalanceService(100);
        assertThatThrownBy(() -> svc.debit(200))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Недостаточно средств");
    }
}