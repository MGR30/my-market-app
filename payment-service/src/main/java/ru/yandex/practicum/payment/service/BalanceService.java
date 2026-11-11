package ru.yandex.practicum.payment.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.payment.exception.InsufficientFundsException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class BalanceService {

    private final ConcurrentHashMap<Long, AtomicLong> balances = new ConcurrentHashMap<>();
    private final long initialBalance;

    public BalanceService(@Value("${payment.initial-balance:100000}") long initialBalance) {
        this.initialBalance = initialBalance;
    }

    private AtomicLong holder(long userId) {
        return balances.computeIfAbsent(userId, k -> new AtomicLong(initialBalance));
    }

    public long getBalance(long userId) {
        return holder(userId).get();
    }

    public long debit(long userId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        AtomicLong balance = holder(userId);
        while (true) {
            long current = balance.get();
            if (current < amount) {
                throw new InsufficientFundsException(current, amount);
            }
            long next = current - amount;
            if (balance.compareAndSet(current, next)) {
                log.info("Debited userId={} amount={} -> {}", userId, amount, next);
                return next;
            }
        }
    }

    public void reset(long userId, long newBalance) {
        holder(userId).set(newBalance);
    }
}