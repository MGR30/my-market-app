package ru.yandex.practicum.payment.service;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BalanceServiceTest {

    @Test
    void debit_decreasesBalance_forUser() {
        var svc = new BalanceService(1000);
        assertThat(svc.debit(1L, 300)).isEqualTo(700);
        assertThat(svc.getBalance(1L)).isEqualTo(700);
    }

    @Test
    void balances_areIndependentPerUser() {
        var svc = new BalanceService(1000);
        svc.debit(1L, 400);
        assertThat(svc.getBalance(1L)).isEqualTo(600);
        assertThat(svc.getBalance(2L)).isEqualTo(1000);
    }

    @Test
    void debit_insufficient() {
        var svc = new BalanceService(100);
        assertThatThrownBy(() -> svc.debit(1L, 200))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Недостаточно средств");
    }
}