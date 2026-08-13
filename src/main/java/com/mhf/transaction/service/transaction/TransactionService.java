package com.mhf.transaction.service.transaction;

import com.mhf.transaction.dto.transaction.TransactionRequest;
import com.mhf.transaction.dto.transaction.TransactionResponse;
import com.mhf.transaction.exception.AccountNotFoundException;
import com.mhf.transaction.exception.InsufficientBalanceException;
import com.mhf.transaction.exception.InvalidTransferException;
import com.mhf.transaction.exception.TransactionNotFoundException;
import com.mhf.transaction.infrastructure.kafka.event.TransactionCompletedEvent;
import com.mhf.transaction.infrastructure.kafka.outbox.OutboxEvent;
import com.mhf.transaction.infrastructure.kafka.outbox.OutboxEventRepository;
import com.mhf.transaction.mapper.transaction.TransactionMapper;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.model.transaction.Transaction;
import com.mhf.transaction.model.transaction.TransactionStatus;
import com.mhf.transaction.repository.account.AccountRepository;
import com.mhf.transaction.repository.transaction.TransactionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              OutboxEventRepository outboxEventRepository,
                              TransactionMapper transactionMapper,
                              ObjectMapper objectMapper) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {

        validateRequest(request);

        Account sourceAccount = accountRepository.findById(request.getSourceAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getSourceAccountId()));

        Account destinationAccount = accountRepository.findById(request.getDestinationAccountId())
                .orElseThrow(() -> new AccountNotFoundException(request.getDestinationAccountId()));

        validateCurrency(sourceAccount, destinationAccount,request);

        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0)
            throw new InsufficientBalanceException(sourceAccount.getId());

        sourceAccount.setBalance(sourceAccount.getBalance().subtract(request.getAmount()));

        destinationAccount.setBalance(destinationAccount.getBalance().add(request.getAmount()));

        Transaction transaction = new Transaction();
        transaction.setSourceAccount(sourceAccount);
        transaction.setDestinationAccount(destinationAccount);
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency());
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setCompletedAt(Instant.now());

        Transaction savedTransaction = transactionRepository.save(transaction);

        TransactionCompletedEvent event = new TransactionCompletedEvent(
                savedTransaction.getId(),
                sourceAccount.getId(),
                destinationAccount.getId(),
                savedTransaction.getAmount(),
                savedTransaction.getCurrency()
        );

        String payload = serializeEvent(event);

        OutboxEvent outboxEvent = new OutboxEvent();

        outboxEvent.setEventType(TransactionCompletedEvent.class.getSimpleName());

        outboxEvent.setAggregateId(savedTransaction.getId().toString());

        outboxEvent.setPayload(payload);

        outboxEventRepository.save(outboxEvent);

        return transactionMapper.toTransferResponse(savedTransaction);
    }

    public TransactionResponse getById(Long id) {

        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        return transactionMapper.toTransferResponse(transaction);
    }

    private String serializeEvent(TransactionCompletedEvent event) {

        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Failed to serialize transaction completed event",
                    exception
            );
        }
    }

    private void validateRequest(TransactionRequest request) {

        if (request == null)
            throw new InvalidTransferException("Transfer request must not be null");

        if (request.getSourceAccountId() == null)
            throw new InvalidTransferException("Source account ID is required");

        if (request.getDestinationAccountId() == null)
            throw new InvalidTransferException("Destination account ID is required");

        if (request.getSourceAccountId().equals(request.getDestinationAccountId()))
            throw new InvalidTransferException("Source and destination accounts must be different");

        if (request.getAmount() == null || request.getAmount().signum() <= 0)
            throw new InvalidTransferException("Transfer amount must be greater than zero");

        if (request.getCurrency() == null || request.getCurrency().isBlank())
            throw new InvalidTransferException("Currency is required");

    }

    private void validateCurrency(Account sourceAccount,
                                  Account destinationAccount,
                                  TransactionRequest request) {

        if (!sourceAccount.getCurrency().equalsIgnoreCase(request.getCurrency()))
            throw new InvalidTransferException("Transfer currency does not match source account currency");

        if (!destinationAccount.getCurrency().equalsIgnoreCase(request.getCurrency()))
            throw new InvalidTransferException("Transfer currency does not match destination account currency");

    }

}
