package com.mhf.transaction.controller.account;

import com.mhf.transaction.dto.account.AccountResponse;
import com.mhf.transaction.dto.account.CreateAccountRequest;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.repository.account.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
public class AccountControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldCreateAccount() {

        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountNumber("ACC-" + UUID.randomUUID());
        request.setOwnerName("John Doe");
        request.setBalance(new BigDecimal("1000.00"));
        request.setCurrency("USD");

        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/accounts",
                request,
                AccountResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        AccountResponse responseBody = response.getBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isNotNull();
        assertThat(responseBody.getAccountNumber())
                .isEqualTo(request.getAccountNumber());
        assertThat(responseBody.getOwnerName())
                .isEqualTo("John Doe");
        assertThat(responseBody.getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(responseBody.getCurrency())
                .isEqualTo("USD");
        assertThat(responseBody.getCreatedAt())
                .isNotNull();

        Account savedAccount = accountRepository.findById(responseBody.getId()).orElseThrow();

        assertThat(savedAccount.getAccountNumber())
                .isEqualTo(request.getAccountNumber());
        assertThat(savedAccount.getOwnerName())
                .isEqualTo("John Doe");
        assertThat(savedAccount.getBalance())
                .isEqualByComparingTo("1000.00");
        assertThat(savedAccount.getCurrency())
                .isEqualTo("USD");

    }

    @Test
    void shouldGetAccountById() {

        Account account = new Account();
        account.setAccountNumber("ACC-" + UUID.randomUUID());
        account.setOwnerName("Jane Doe");
        account.setBalance(new BigDecimal("2500.00"));
        account.setCurrency("EUR");

        Account savedAccount = accountRepository.save(account);

        ResponseEntity<AccountResponse> response = restTemplate.getForEntity(
                "/api/accounts/" + savedAccount.getId(),
                AccountResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AccountResponse responseBody = response.getBody();

        assertThat(responseBody).isNotNull();
        assertThat(responseBody.getId()).isEqualTo(savedAccount.getId());
        assertThat(responseBody.getAccountNumber())
                .isEqualTo("ACC-" + savedAccount.getAccountNumber().substring(4));
        assertThat(responseBody.getOwnerName())
                .isEqualTo("Jane Doe");
        assertThat(responseBody.getBalance())
                .isEqualByComparingTo("2500.00");
        assertThat(responseBody.getCurrency())
                .isEqualTo("EUR");
        assertThat(responseBody.getCreatedAt())
                .isNotNull();

    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() {
        Long nonExistingId = Long.MAX_VALUE;

        ResponseEntity<Void> response = restTemplate.getForEntity(
                "/api/accounts/" + nonExistingId,
                Void.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }


}
