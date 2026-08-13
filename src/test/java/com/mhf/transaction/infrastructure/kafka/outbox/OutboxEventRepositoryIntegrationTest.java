package com.mhf.transaction.infrastructure.kafka.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OutboxEventRepositoryIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    @Transactional
    void shouldPersistOutboxEvent() {
        OutboxEvent event = new OutboxEvent();

        event.setEventType("TransactionCompletedEvent");
        event.setAggregateId("42");
        event.setPayload("""
                {
                  "transactionId": 42,
                  "sourceAccountId": 100,
                  "destinationAccountId": 200,
                  "amount": 150.00,
                  "currency": "EUR"
                }
                """);
        event.setCreatedAt(Instant.now());

        OutboxEvent savedEvent =
                outboxEventRepository.save(event);

        assertThat(savedEvent.getId()).isNotNull();
        assertThat(savedEvent.getEventType())
                .isEqualTo("TransactionCompletedEvent");
        assertThat(savedEvent.getAggregateId())
                .isEqualTo("42");
        assertThat(savedEvent.getPayload())
                .contains("\"transactionId\": 42");
        assertThat(savedEvent.getPublishedAt())
                .isNull();
    }

}
