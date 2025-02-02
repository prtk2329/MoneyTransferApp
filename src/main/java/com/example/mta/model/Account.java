package com.example.mta.model;

import java.math.BigDecimal;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Account {
    private Long accountId;
    private BigDecimal balance;

    public Account(Long accountId, BigDecimal balance) {
        this.accountId = accountId;
        this.balance = balance;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Account debit(BigDecimal amount) {
        balance = balance.subtract(amount);
        return this;
    }

    public Account credit(BigDecimal amount) {
        balance = balance.add(amount);
        return this;
    }
}
