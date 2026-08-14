package com.mhf.transaction.infrastructure.redis;

import com.mhf.transaction.dto.transaction.TransactionRequest;
import com.mhf.transaction.dto.transaction.TransactionResponse;
import com.mhf.transaction.infrastructure.kafka.outbox.OutboxEventRepository;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.repository.account.AccountRepository;
import com.mhf.transaction.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest (
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
public class TransactionCachingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @MockitoSpyBean
    private TransactionRepository transactionRepositorySpy;

    @BeforeEach
    void cleanDatabase() {

        outboxEventRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();

        clearRedis();
    }

    @Test
    void shouldReturnTransactionFromCacheOnSecondRequest() {

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
                new BigDecimal("250.00"),
                "USD"
        );

        ResponseEntity<TransactionResponse> createResponse =
                restTemplate.postForEntity(
                        "/api/transactions",
                        request,
                        TransactionResponse.class
                );

        assertThat(createResponse.getStatusCode())
                .isEqualTo(HttpStatus.CREATED);

        Long transactionId =
                createResponse.getBody().getTransactionId();

        // reset the spy so calls made during transaction creation
        // do not affect the caching assertion
        Mockito.clearInvocations(transactionRepositorySpy);

        ResponseEntity<TransactionResponse> firstResponse =
                restTemplate.getForEntity(
                        "/api/transactions/" + transactionId,
                        TransactionResponse.class
                );

        ResponseEntity<TransactionResponse> secondResponse =
                restTemplate.getForEntity(
                        "/api/transactions/" + transactionId,
                        TransactionResponse.class
                );

        assertThat(firstResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(secondResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        assertThat(secondResponse.getBody())
                .usingRecursiveComparison()
                .isEqualTo(firstResponse.getBody());

        verify(transactionRepositorySpy, times(1))
                .findById(transactionId);

    }


    private void clearRedis() {

        redisConnectionFactory
                .getConnection()
                .serverCommands()
                .flushDb();;
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
