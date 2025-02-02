package com.example.mta.service;

import com.example.mta.exception.InvalidAmountException;
import com.example.mta.exception.InvalidTransferException;
import com.example.mta.model.Account;

import java.math.BigDecimal;

public class MTAService {

    private AccountService accountService;

    public MTAService(AccountService accountService) {
        this.accountService = accountService;
    }

    public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
        Account fromAccount = accountService.getAccountById(fromAccountId);
        Account toAccount = accountService.getAccountById(toAccountId);

        checkAmount(amount);

        if (fromAccount.equals(toAccount)) {
            throw new InvalidTransferException("Transfer within same account is not allowed");
        }

        if (fromAccount.getBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidTransferException("User account balance is negative");
        }

        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InvalidTransferException("User doesn't have sufficient balance for this transaction");
        }

        fromAccount.debit(amount);
        toAccount.credit(amount);

    }

    private void checkAmount(BigDecimal amount) {
        if (amount == null) {
            throw new InvalidAmountException("amount should be set");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("amount must be above zero");
        }
    }
}
