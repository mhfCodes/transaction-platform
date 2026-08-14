package com.mhf.transaction.infrastructure.kafka.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@SpringBootTest(properties = {
        "outbox.publisher.initial-delay=3600000"
})
public class OutboxEventPublisherFailureIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {

        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldKeepOutboxEventUnpublishedWhenKafkaPublishingFails() {

        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setEventType("TransactionCompletedEvent");
        event.setAggregateId("123");
        event.setPayload("""
            {
                "transactionId": 123,
                "sourceAccountId": 10,
                "destinationAccountId": 20,
                "amount": 250.00,
                "currency": "USD"
            }
            """);
        OutboxEvent savedEvent =
                outboxEventRepository.save(event);

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();

        failedFuture.completeExceptionally(new RuntimeException("Kafka is unavailable"));

        given(
                kafkaTemplate.send(
                        "transaction-events",
                        savedEvent.getAggregateId(),
                        savedEvent.getPayload()
                )
        ).willReturn(failedFuture);

        // Verify that the event was NOT marked as published
        OutboxEvent unchangedEvent =
                outboxEventRepository.findById(savedEvent.getId())
                        .orElseThrow();
        assertThat(unchangedEvent.getPublishedAt())
                .isNull();

    }

    @Test
    void shouldRetryUnpublishedEventSuccessfully() {

        // Arrange
        OutboxEvent event = new OutboxEvent();
        event.setEventType(
                "TransactionCompletedEvent"
        );
        event.setAggregateId("123");
        event.setPayload("""
            {
                "transactionId": 123,
                "sourceAccountId": 10,
                "destinationAccountId": 20,
                "amount": 250.00,
                "currency": "USD"
            }
            """);

        OutboxEvent savedEvent = outboxEventRepository.save(event);

        // first kafka attempt fails
        CompletableFuture<SendResult<String, String>> failedFuture =
                new CompletableFuture<>();
        failedFuture.completeExceptionally(
                new RuntimeException("Kafka is temporarily unavailable")
        );

        // second kafka attempt succeeds
        CompletableFuture<SendResult<String, String>> successfulFuture =
                CompletableFuture.completedFuture(null);

        given(
                kafkaTemplate.send(
                        "transaction-events",
                        savedEvent.getAggregateId(),
                        savedEvent.getPayload()
                )
        )
                .willReturn(failedFuture)
                .willReturn(successfulFuture);

        // first attempt
        outboxEventPublisher.publishUnpublishedEvents();
        OutboxEvent afterFirstAttempt = outboxEventRepository.findById(savedEvent.getId())
                        .orElseThrow();
        assertThat(afterFirstAttempt.getPublishedAt()).isNull();

        // second attempt
        outboxEventPublisher.publishUnpublishedEvents();
        OutboxEvent afterSecondAttempt = outboxEventRepository.findById(savedEvent.getId())
                        .orElseThrow();
        assertThat(afterSecondAttempt.getPublishedAt()).isNotNull();

        // verify kafka was attempted twice
        then(kafkaTemplate)
                .should(times(2))
                .send(
                        "transaction-events",
                        savedEvent.getAggregateId(),
                        savedEvent.getPayload()
                );

    }

}
