package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.MoneyTransfer;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.exception.InvalidAccountException;
import com.db.awmd.challenge.repository.AccountsRepository;
import com.google.common.util.concurrent.Striped;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.locks.ReadWriteLock;

@Service
public class AccountsService {

    private static final String INVALID_ACCOUNT_MESSAGE = "Either Source or Destination account in the request does not exist.";

    @Getter
    private final AccountsRepository accountsRepository;

    @Autowired
    private NotificationService notificationService;

    private Striped<ReadWriteLock> rwLockStripes;

    @Autowired
    public AccountsService(AccountsRepository accountsRepository) {
        this.accountsRepository = accountsRepository;
        rwLockStripes = Striped.readWriteLock(10);
    }

    public void createAccount(Account account) {
        this.accountsRepository.createAccount(account);
    }

    public Account getAccount(String accountId) {
        return this.accountsRepository.getAccount(accountId);
    }

    public void transferAmount(MoneyTransfer moneyTransfer) {
        Account sourceAccount;
        Account destinationAccount;
        sourceAccount = this.accountsRepository.getAccount(moneyTransfer.getFromAccountId());
        destinationAccount = this.accountsRepository.getAccount(moneyTransfer.getToAccountId());
        this.validateThatAccountsAreValid(sourceAccount, destinationAccount);
        this.lockAndUpdateSourceAccount(moneyTransfer, sourceAccount);
        this.lockAndUpdateDestinationAccount(moneyTransfer, destinationAccount);
        this.notifyAccountHolders(sourceAccount, destinationAccount, moneyTransfer);
    }

    private void lockAndUpdateDestinationAccount(MoneyTransfer moneyTransfer, Account destinationAccount) {
        ReadWriteLock readWriteLock;
        readWriteLock = rwLockStripes.get(moneyTransfer.getToAccountId());
        try{
            readWriteLock.writeLock().lock();
            BigDecimal finalDestinationAccountBalance = destinationAccount.getBalance().add(moneyTransfer.getAmount());
            this.accountsRepository.updateAccount(moneyTransfer.getToAccountId(), finalDestinationAccountBalance);
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void lockAndUpdateSourceAccount(MoneyTransfer moneyTransfer, Account sourceAccount) {
        ReadWriteLock readWriteLock = rwLockStripes.get(moneyTransfer.getFromAccountId());
        try{
            readWriteLock.writeLock().lock();
            BigDecimal finalSourceAccountBalance = sourceAccount.getBalance().subtract(moneyTransfer.getAmount());
            this.checkForSufficientBalance(sourceAccount.getAccountId(), finalSourceAccountBalance);
            this.accountsRepository.updateAccount(moneyTransfer.getFromAccountId(), finalSourceAccountBalance);
        }finally {
            readWriteLock.writeLock().unlock();
        }
    }

    private void notifyAccountHolders(Account sourceAccount, Account destinationAccount, MoneyTransfer moneyTransfer) {
        this.notificationService.notifyAboutTransfer(sourceAccount, moneyTransfer.getToAccountId()
                + " : " + moneyTransfer.getAmount());
        this.notificationService.notifyAboutTransfer(destinationAccount, moneyTransfer.getFromAccountId()
                + " : " + moneyTransfer.getAmount());
    }

    private void checkForSufficientBalance(String sourceAccountId, BigDecimal finalSourceAccountBalance) {
        if (finalSourceAccountBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account : " + sourceAccountId
                    + " to complete this transfer.");
        }
    }

    private void validateThatAccountsAreValid(Account sourceAccount, Account destinationAccount) {
        if (null == sourceAccount || null == destinationAccount) {
            throw new InvalidAccountException(INVALID_ACCOUNT_MESSAGE);
        }
    }
}
