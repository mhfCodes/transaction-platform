package com.mhf.transaction.infrastructure.kafka.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private static final String TRANSACTION_EVENTS_TOPIC = "transaction-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxEventPublisher(OutboxEventRepository outboxEventRepository,
                                KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay:5000}",
            initialDelayString = "${outbox.publisher.initial-delay:5000}"
    )
    @Transactional
    public void publishUnpublishedEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByPublishedAtIsNull();

        for (OutboxEvent event : events) {

            try {
                publish(event);
            } catch (Exception exception) {
                log.error("Failed to publish outbox event {}", event.getId(), exception);
            }
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
