package ru.yandex.practicum.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class BalanceService {

    private final AtomicLong balance;

    public BalanceService(@Value("${payment.initial-balance:100000}") long initialBalance) {
        this.balance = new AtomicLong(initialBalance);
    }

    public long getBalance() {
        return balance.get();
    }

    public long debit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        while (true) {
            long current = balance.get();
            if (current < amount) {
                throw new InsufficientFundsException(current, amount);
            }
            long next = current - amount;
            if (balance.compareAndSet(current, next)) {
                log.info("Debited {} -> {}", amount, next);
                return next;
            }
        }
    }

    public void reset(long newBalance) {
        balance.set(newBalance);
    }
}