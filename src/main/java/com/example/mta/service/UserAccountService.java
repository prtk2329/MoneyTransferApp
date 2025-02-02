package com.example.mta.service;

import com.example.mta.exception.AccountNotFoundException;
import com.example.mta.model.Account;

import java.util.Map;

public class UserAccountService implements AccountService {

    private final Map<Long, Account> accounts;

    public UserAccountService(Map<Long, Account> accounts) {
        this.accounts = accounts;
    }

    @Override
    public Account getAccountById(Long accountId) {
        Account account = accounts.get(accountId);

        if(account == null) {
            throw new AccountNotFoundException(String.format("Account with id %d not found", accountId));
        }

        return account;
    }

}
