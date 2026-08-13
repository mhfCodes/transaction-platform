package com.mhf.transaction.infrastructure.kafka.event;

import java.math.BigDecimal;

public record TransactionCompletedEvent(
        Long transactionId,
        Long sourceAccountId,
        Long destinationAccountId,
        BigDecimal amount,
        String currency
) {
}
