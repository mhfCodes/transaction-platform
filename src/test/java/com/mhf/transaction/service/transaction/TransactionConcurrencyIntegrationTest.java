package com.mhf.transaction.service.transaction;

import com.mhf.transaction.dto.transaction.TransactionRequest;
import com.mhf.transaction.model.account.Account;
import com.mhf.transaction.repository.account.AccountRepository;
import com.mhf.transaction.repository.transaction.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TransactionConcurrencyIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldPreventConcurrentTransfersFromUpdatingSameAccount() throws Exception {

        Account sourceAccount = createAccount(
                "ACC-SOURCE",
                "Source Account",
                new BigDecimal("1000.00")
        );

        Account destinationAccount1 = createAccount(
                "ACC-DEST-1",
                "Destination Account 1",
                BigDecimal.ZERO
        );

        Account destinationAccount2 = createAccount(
                "ACC-DEST-2",
                "Destination Account 2",
                BigDecimal.ZERO
        );

        Long sourceAccountId = sourceAccount.getId();
        Long destinationAccount1Id = destinationAccount1.getId();
        Long destinationAccount2Id = destinationAccount2.getId();

        CountDownLatch accountsLoaded = new CountDownLatch(2);
        CountDownLatch allowTransfers = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<TransactionResult> transaction1 =
                    executor.submit(() ->
                            executeConcurrentTransaction(
                                    sourceAccountId,
                                    destinationAccount1Id,
                                    accountsLoaded,
                                    allowTransfers
                            )
                    );

            Future<TransactionResult> transaction2 =
                    executor.submit(() ->
                            executeConcurrentTransaction(
                                    sourceAccountId,
                                    destinationAccount2Id,
                                    accountsLoaded,
                                    allowTransfers
                            )
                    );

            /*
            * Wait until both transactions have loaded
            * the source account with the same version
            */
            accountsLoaded.await();

            /*
            * Allow both transactions to perform the transfer
            */
            allowTransfers.countDown();

            TransactionResult result1 = transaction1.get();
            TransactionResult result2 = transaction2.get();


            /*
            * Exactly one transaction must succeed
            */
            assertThat(result1.successful() ^ result2.successful()).isTrue();

            /*
             * The other transfer must fail because of
             * optimistic locking.
             */
            assertThat(result1.optimisticLockFailure() || result2.optimisticLockFailure()).isTrue();

            Account finalSourceAccount =
                    accountRepository.findById(sourceAccountId)
                            .orElseThrow();

            Account finalDestinationAccount1 =
                    accountRepository.findById(destinationAccount1Id)
                            .orElseThrow();

            Account finalDestinationAccount2 =
                    accountRepository.findById(destinationAccount2Id)
                            .orElseThrow();

            /*
             * Only one $600 transfer was allowed.
             */
            assertThat(finalSourceAccount.getBalance())
                    .isEqualByComparingTo("400.00");

            /*
             * The destinations received exactly $600 in total.
             */
            assertThat(finalDestinationAccount1.getBalance()
                    .add(finalDestinationAccount2.getBalance())
            ).isEqualByComparingTo("600.00");

            /*
             * The source account was successfully updated once.
             */
            assertThat(finalSourceAccount.getVersion()).isEqualTo(1L);

            /*
             * The failed transfer must have been rolled back,
             * so only one transaction exists.
             */
            assertThat(transactionRepository.count()).isEqualTo(1);

        } finally {
            executor.shutdownNow();
        }

    }

    private TransactionResult executeConcurrentTransaction(Long sourceAccountId,
                                                           Long destinationAccountId,
                                                           CountDownLatch accountsLoaded,
                                                           CountDownLatch allowTransfers) {

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        try {

            transactionTemplate.execute(status -> {

                /*
                * Load the accounts inside this transaction
                *
                * Both transactions therefore obtain their own
                * Account instance with the same initial version
                */
                accountRepository.findById(sourceAccountId)
                        .orElseThrow();

                accountRepository.findById(destinationAccountId)
                        .orElseThrow();

                /*
                * Tell the test that this transaction has loaded
                * the accounts
                */
                accountsLoaded.countDown();

                try {
                    /*
                    * Wait until both transactions have loaded
                    * the accounts before continuing
                    */
                    allowTransfers.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Transaction thread was interrupted",
                            exception
                    );
                }

                TransactionRequest request = new TransactionRequest();

                request.setSourceAccountId(sourceAccountId);
                request.setDestinationAccountId(destinationAccountId);
                request.setAmount(new BigDecimal("600.00"));
                request.setCurrency("USD");

                /*
                * Execute the real business operation
                */
                transactionService.transfer(request);

                return null;
            });

            return new TransactionResult(true, false);

        } catch (ObjectOptimisticLockingFailureException exception) {

            /*
             * This is an expected outcome:
             * another transaction updated the account first.
             */
            return new TransactionResult(false, true);
        }

    }

    private Account createAccount(
            String accountNumber,
            String ownerName,
            BigDecimal balance) {

        Account account = new Account();

        account.setAccountNumber(accountNumber);
        account.setOwnerName(ownerName);
        account.setBalance(balance);
        account.setCurrency("USD");

        return accountRepository.save(account);
    }

    private record TransactionResult(boolean successful,
                                  boolean optimisticLockFailure) {
    }

}
