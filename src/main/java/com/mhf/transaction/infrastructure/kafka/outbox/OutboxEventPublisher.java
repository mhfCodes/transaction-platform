package com.mhf.transaction.infrastructure.kafka.outbox;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventPublisher {

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public void publishUnpublishedEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByPublishedAtIsNull();

        for (OutboxEvent event : events) {

            publish(event);
        }
    }

    private void publish(OutboxEvent event) {

        try {

            kafkaTemplate.send(
                    TRANSACTION_EVENTS_TOPIC,
                    event.getAggregateId(),
                    event.getPayload()
            ).get();

            event.setPublishedAt(Instant.now());

            outboxEventRepository.save(event);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Failed to publish outbox event " + event.getId(),
                    exception
            );
        }
    }

}
