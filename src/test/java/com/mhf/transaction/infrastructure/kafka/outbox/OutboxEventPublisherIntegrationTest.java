package com.mhf.transaction.infrastructure.kafka.outbox;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(
        topics = "transaction-events",
        partitions = 1
)
public class OutboxEventPublisherIntegrationTest {

    private static final String TOPIC = "transaction-events";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @BeforeEach
    void setUp() {

        outboxEventRepository.deleteAll();
    }

    @Test
    void shouldPublishOutboxEventToKafka() {

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

        OutboxEvent savedEvent = outboxEventRepository.save(event);

        var consumerProperties =
                KafkaTestUtils.consumerProps(
                        embeddedKafka,
                        "outbox-test-group",
                        false
                );

        consumerProperties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );

        try (Consumer<String, String> consumer =
                     new DefaultKafkaConsumerFactory<String, String>(
                             consumerProperties,
                             new StringDeserializer(),
                             new StringDeserializer()
                     ).createConsumer()) {

            consumer.subscribe(List.of(TOPIC));

            // Act
            outboxEventPublisher.publishUnpublishedEvents();

            //Assert
            ConsumerRecord<String, String> record =
                    KafkaTestUtils.getSingleRecord(
                            consumer,
                            TOPIC,
                            Duration.ofSeconds(5)
                    );

            assertThat(record.key())
                    .isEqualTo("123");

            assertThat(record.value())
                    .contains("\"transactionId\": 123");

            assertThat(record.value())
                    .contains("\"sourceAccountId\": 10");

            assertThat(record.value())
                    .contains("\"destinationAccountId\": 20");

            assertThat(record.value())
                    .contains("\"amount\": 250.00");

            assertThat(record.value())
                    .contains("\"currency\": \"USD\"");

        }

        OutboxEvent publishedEvent = outboxEventRepository.findById(savedEvent.getId())
                        .orElseThrow();

        assertThat(publishedEvent.getPublishedAt()).isNotNull();

    }

}
