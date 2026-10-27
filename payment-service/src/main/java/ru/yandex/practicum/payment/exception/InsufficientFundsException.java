package ru.yandex.practicum.payment.exception;

import lombok.Getter;

@Getter
public class InsufficientFundsException extends RuntimeException {
    private final long balance;
    private final long required;

    public InsufficientFundsException(long balance, long required) {
        super("Недостаточно средств: баланс=" + balance + ", требуется=" + required);
        this.balance = balance;
        this.required = required;
    }
}
