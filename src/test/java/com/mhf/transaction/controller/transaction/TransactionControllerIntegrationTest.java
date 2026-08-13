package com.mhf.transaction.controller.transaction;

import com.mhf.transaction.dto.transaction.TransactionRequest;
import com.mhf.transaction.dto.transaction.TransactionResponse;
import com.mhf.transaction.infrastructure.kafka.outbox.OutboxEvent;
import com.mhf.transaction.infrastructure.kafka.outbox.OutboxEventRepository;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.repository.account.AccountRepository;
import com.mhf.transaction.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class TransactionControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldTransferMoney() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest request = new TransactionRequest();

        request.setSourceAccountId(sourceAccount.getId());
        request.setDestinationAccountId(destinationAccount.getId());
        request.setAmount(new BigDecimal("250.00"));
        request.setCurrency("USD");

        ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                "/api/transactions",
                request,
                TransactionResponse.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        TransactionResponse responseBody = response.getBody();

        assertThat(responseBody).isNotNull();

        assertThat(responseBody.getTransactionId())
                .isNotNull();

        assertThat(responseBody.getSourceAccountId())
                .isEqualTo(sourceAccount.getId());

        assertThat(responseBody.getDestinationAccountId())
                .isEqualTo(destinationAccount.getId());

        assertThat(responseBody.getAmount())
                .isEqualByComparingTo("250.00");

        assertThat(responseBody.getCurrency())
                .isEqualTo("USD");

        assertThat(responseBody.getStatus())
                .hasToString("COMPLETED");

        assertThat(responseBody.getCreatedAt())
                .isNotNull();

        assertThat(responseBody.getCompletedAt())
                .isNotNull();

        Account updatedSourceAccount =
                accountRepository.findById(sourceAccount.getId())
                        .orElseThrow();

        Account updatedDestinationAccount =
                accountRepository.findById(destinationAccount.getId())
                        .orElseThrow();

        assertThat(updatedSourceAccount.getBalance())
                .isEqualByComparingTo("750.00");

        assertThat(updatedDestinationAccount.getBalance())
                .isEqualByComparingTo("750.00");

        assertThat(transactionRepository.count())
                .isEqualTo(1);

        assertThat(outboxEventRepository.count())
                .isEqualTo(1);

        OutboxEvent outboxEvent =
                outboxEventRepository.findAll()
                        .get(0);

        assertThat(outboxEvent.getEventType())
                .isEqualTo("TransactionCompletedEvent");

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(
                        responseBody.getTransactionId().toString()
                );

        assertThat(outboxEvent.getPayload())
                .contains("\"transactionId\"");

        assertThat(outboxEvent.getPayload())
                .contains("\"sourceAccountId\"");

        assertThat(outboxEvent.getPayload())
                .contains("\"destinationAccountId\"");

        assertThat(outboxEvent.getPayload())
                .contains("\"amount\"");

        assertThat(outboxEvent.getPayload())
                .contains("\"currency\"");

        /*
         * The event has not been published to Kafka yet.
         */
        assertThat(outboxEvent.getPublishedAt())
                .isNull();
    }

    @Test
    void shouldRejectTransferWhenBalanceIsInsufficient() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("100.00"),
                "USD"
        );

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest request = createTransferRequest(
                sourceAccount,
                destinationAccount,
                new BigDecimal("150.00"),
                "USD"
        );

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/transactions",
                request,
                Void.class
        );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);

        Account unchangedSource =
                accountRepository.findById(sourceAccount.getId())
                        .orElseThrow();

        Account unchangedDestination =
                accountRepository.findById(destinationAccount.getId())
                        .orElseThrow();

        assertThat(unchangedSource.getBalance())
                .isEqualByComparingTo("100.00");

        assertThat(unchangedDestination.getBalance())
                .isEqualByComparingTo("500.00");

        assertThat(transactionRepository.count())
                .isZero();

        assertThat(outboxEventRepository.count())
                .isZero();
    }

    @Test
    void shouldReturnNotFoundWhenSourceDoesNotExist() {

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest request = new TransactionRequest();

        request.setSourceAccountId(Long.MAX_VALUE);
        request.setDestinationAccountId(destinationAccount.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        Void.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        assertThat(transactionRepository.count()).isZero();

    }

    @Test
    void shouldReturnNotFoundWhenDestinationAccountDoesNotExist() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        TransactionRequest request = new TransactionRequest();

        request.setSourceAccountId(sourceAccount.getId());
        request.setDestinationAccountId(Long.MAX_VALUE);
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        Void.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        Account unchangedSource =
                accountRepository.findById(sourceAccount.getId())
                        .orElseThrow();

        assertThat(unchangedSource.getBalance())
                .isEqualByComparingTo("1000.00");

        assertThat(transactionRepository.count())
                .isZero();

    }

    @Test
    void shouldRejectTransactionBetweenSameAccount() {

        Account account = createAccount(
                "Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        TransactionRequest request = new TransactionRequest();

        request.setSourceAccountId(account.getId());
        request.setDestinationAccountId(account.getId());
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("USD");

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        Void.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Account unchangedAccount =
                accountRepository.findById(account.getId())
                        .orElseThrow();

        assertThat(unchangedAccount.getBalance())
                .isEqualByComparingTo("1000.00");

        assertThat(transactionRepository.count())
                .isZero();

    }

    @Test
    void shouldRejectTransferWithNonPositiveAmount() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest request = createTransferRequest(
                sourceAccount,
                destinationAccount,
                BigDecimal.ZERO,
                "USD"
        );

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        Void.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(transactionRepository.count())
                .isZero();
    }

    @Test
    void shouldRejectTransferWhenCurrencyDoesNotMatchSourceAccount() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest request = createTransferRequest(
                sourceAccount,
                destinationAccount,
                new BigDecimal("100.00"),
                "EUR"
        );

        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        Void.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        Account unchangedSource =
                accountRepository.findById(sourceAccount.getId())
                        .orElseThrow();

        assertThat(unchangedSource.getBalance())
                .isEqualByComparingTo("1000.00");

        assertThat(transactionRepository.count())
                .isZero();
    }

    @Test
    void shouldGetTransactionById() {

        Account sourceAccount = createAccount(
                "Source Account",
                new BigDecimal("1000.00"),
                "USD"
        );

        Account destinationAccount = createAccount(
                "Destination Account",
                new BigDecimal("500.00"),
                "USD"
        );

        TransactionRequest transactionRequest = createTransferRequest(
                sourceAccount,
                destinationAccount,
                new BigDecimal("250.00"),
                "USD"
        );

        ResponseEntity<TransactionResponse> transactionResponse =
                restTemplate.postForEntity(
                        "/api/transactions",
                        transactionRequest,
                        TransactionResponse.class
                );

        assertThat(transactionResponse.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        Long transactionId = transactionResponse.getBody().getTransactionId();

        ResponseEntity<TransactionResponse> response = restTemplate.getForEntity(
                "/api/transactions/" + transactionId,
                TransactionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        TransactionResponse responseBody = response.getBody();

        assertThat(responseBody).isNotNull();

        assertThat(responseBody.getTransactionId())
                .isEqualTo(transactionId);

        assertThat(responseBody.getSourceAccountId())
                .isEqualTo(sourceAccount.getId());

        assertThat(responseBody.getDestinationAccountId())
                .isEqualTo(destinationAccount.getId());

        assertThat(responseBody.getAmount())
                .isEqualByComparingTo("250.00");

        assertThat(responseBody.getCurrency())
                .isEqualTo("USD");

        assertThat(responseBody.getStatus())
                .hasToString("COMPLETED");

        assertThat(responseBody.getCreatedAt())
                .isNotNull();

        assertThat(responseBody.getCompletedAt())
                .isNotNull();

    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist() {
        Long nonExistingId = Long.MAX_VALUE;

        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/transactions/" + nonExistingId,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

    }


    private Account createAccount(
            String ownerName,
            BigDecimal balance,
            String currency) {

        Account account = new Account();

        account.setAccountNumber(
                "ACC-" + UUID.randomUUID()
        );
        account.setOwnerName(ownerName);
        account.setBalance(balance);
        account.setCurrency(currency);

        return accountRepository.save(account);
    }

    private TransactionRequest createTransferRequest(
            Account sourceAccount,
            Account destinationAccount,
            BigDecimal amount,
            String currency) {

        TransactionRequest request = new TransactionRequest();

        request.setSourceAccountId(sourceAccount.getId());
        request.setDestinationAccountId(destinationAccount.getId());
        request.setAmount(amount);
        request.setCurrency(currency);

        return request;
    }

}
