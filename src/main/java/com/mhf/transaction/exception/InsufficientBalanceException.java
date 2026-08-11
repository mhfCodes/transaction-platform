package com.mhf.transaction.exception;

public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(Long accountId) {
        super("Insufficient balance for account: " + accountId);
    }
}
