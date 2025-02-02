package com.example.mta.service;

import com.example.mta.exception.AccountNotFoundException;
import com.example.mta.exception.InvalidAmountException;
import com.example.mta.exception.InvalidTransferException;
import com.example.mta.model.Account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MTAServiceTest {
    private MTAService mtaService;
    private AccountService accountService;

    @BeforeEach
    public void setup() {
        Account account1 = new Account(100L, BigDecimal.valueOf(300L));
        Account account2 = new Account(101L, BigDecimal.valueOf(190.90));
        Account account3 = new Account(102L, BigDecimal.valueOf(340.53));
        Account account4 = new Account(103L, BigDecimal.valueOf(-99));

        Map<Long, Account> accounts = new ConcurrentHashMap<>();
        accounts.put(account1.getAccountId(), account1);
        accounts.put(account2.getAccountId(), account2);
        accounts.put(account3.getAccountId(), account3);
        accounts.put(account4.getAccountId(), account4);

        accountService = new UserAccountService(accounts);
        mtaService = new MTAService(accountService);
    }

    @Test
    public void testShouldThrowExceptionWhenSenderAccountNotFound() {
        AccountNotFoundException error = assertThrows(AccountNotFoundException.class,
                () -> mtaService.transfer(300L, 100L, BigDecimal.valueOf(1L)));
        assertEquals("Account with id 300 not found", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenRecipientAccountNotFound() {
        AccountNotFoundException error = assertThrows(AccountNotFoundException.class,
                () -> mtaService.transfer(102L, 300L, BigDecimal.valueOf(1L)));

        assertEquals("Account with id 300 not found", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenTransferAmountIsNull() {
        InvalidAmountException error = assertThrows(InvalidAmountException.class,
                () -> mtaService.transfer(102L, 100L, null));

        assertEquals("amount should be set", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenTransferAmountIsZero() {
        InvalidAmountException error = assertThrows(InvalidAmountException.class,
                () -> mtaService.transfer(102L, 100L, BigDecimal.ZERO));

        assertEquals("amount must be above zero", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenTransferAmountIsNegative() {
        InvalidAmountException error = assertThrows(InvalidAmountException.class,
                () -> mtaService.transfer(102L, 100L, BigDecimal.valueOf(-10L)));

        assertEquals("amount must be above zero", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenTransferWithinSameAccount() {
        InvalidTransferException error = assertThrows(InvalidTransferException.class,
                () -> mtaService.transfer(102L, 102L, BigDecimal.valueOf(1L)));

        assertEquals("Transfer within same account is not allowed", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenInsufficientBalance() {
        InvalidTransferException error = assertThrows(InvalidTransferException.class,
                () -> mtaService.transfer(102L, 100L, BigDecimal.valueOf(500L)));

        assertEquals("User doesn't have sufficient balance for this transaction", error.getMessage());
    }

    @Test
    public void testShouldThrowExceptionWhenAccountBalanceIsNegative() {
        InvalidTransferException error = assertThrows(InvalidTransferException.class,
                () -> mtaService.transfer(103L, 100L, BigDecimal.valueOf(1L)));

        assertEquals("User account balance is negative", error.getMessage());
    }

    @Test
    public void testShouldTransferAmount() {
        mtaService.transfer(100L, 101L, BigDecimal.valueOf(10L));

        Account fromAccount = accountService.getAccountById(100L);
        Account toAccount = accountService.getAccountById(101L);

        assertAll("Transfer assertions",
                () -> assertEquals(BigDecimal.valueOf(290L), fromAccount.getBalance()),
                () -> assertEquals(BigDecimal.valueOf(200.90), toAccount.getBalance())
        );
    }

    @Test
    public void testShouldTransferAmountInDecimals() {
        mtaService.transfer(100L, 101L, BigDecimal.valueOf(10.6));

        Account fromAccount = accountService.getAccountById(100L);
        Account toAccount = accountService.getAccountById(101L);

        assertAll("Transfer assertions",
                () -> assertEquals(BigDecimal.valueOf(289.40), fromAccount.getBalance()),
                () -> assertEquals(BigDecimal.valueOf(201.50), toAccount.getBalance())
        );

    }

    @Test
    public void testShouldTransferExactSenderBalance() {
        mtaService.transfer(100L, 101L, BigDecimal.valueOf(300L));

        Account fromAccount = accountService.getAccountById(100L);
        Account toAccount = accountService.getAccountById(101L);

        assertAll("Transfer assertions",
                () -> assertEquals(BigDecimal.ZERO, fromAccount.getBalance()),
                () -> assertEquals(BigDecimal.valueOf(490.90), toAccount.getBalance())
        );
    }

    @Test
    public void testShouldTransferAmount_WhenConcurrentRequests() throws InterruptedException {
        int transferPerThread = 20;
        AtomicInteger errorCount = new AtomicInteger(0);

        Runnable task = () -> {
            for (int j = 0; j < transferPerThread; j++) {
                try {
                    mtaService.transfer(100L, 101L, BigDecimal.ONE);
                } catch (InvalidTransferException exception) {
                    errorCount.incrementAndGet();
                }
            }
        };

        executeConcurrentRequests(10, task);

        Account fromAccount = accountService.getAccountById(100L);
        Account toAccount = accountService.getAccountById(101L);

        assertAll("Transfer assertions",
                () -> assertEquals(0, errorCount.get()),
                () -> assertEquals(BigDecimal.valueOf(100L), fromAccount.getBalance()),
                () -> assertEquals(BigDecimal.valueOf(390.90), toAccount.getBalance())
        );
    }

    @Test
    public void testShouldTransferAmountAndThrowExceptionWhenInsufficientBalance_ConcurrentRequests() throws InterruptedException {
        int transferPerThread = 20;
        AtomicInteger errorCount = new AtomicInteger(0);

        Runnable task = () -> {
            for (int j = 0; j < transferPerThread; j++) {
                try {
                    mtaService.transfer(100L, 101L, BigDecimal.valueOf(2L));
                } catch (InvalidTransferException exception) {
                    errorCount.incrementAndGet();
                }
            }
        };

        executeConcurrentRequests(10, task);

        Account fromAccount = accountService.getAccountById(100L);
        Account toAccount = accountService.getAccountById(101L);

        assertAll("Transfer assertions",
                () -> assertEquals(50, errorCount.get()),
                () -> assertEquals(BigDecimal.ZERO, fromAccount.getBalance()),
                () -> assertEquals(BigDecimal.valueOf(490.90), toAccount.getBalance())
        );
    }

    private void executeConcurrentRequests(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    task.run();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();
    }
}
