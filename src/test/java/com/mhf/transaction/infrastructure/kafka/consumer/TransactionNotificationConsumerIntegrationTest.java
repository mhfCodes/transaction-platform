package com.mhf.transaction.infrastructure.kafka.consumer;

import com.mhf.transaction.service.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@EmbeddedKafka(
        topics = "transaction-events",
        partitions = 1
)
public class TransactionNotificationConsumerIntegrationTest {

    private static final String TOPIC = "transaction-events";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void shouldConsumeTransactionCompletedEvent() {

        String payload = """
                {
                    "transactionId": 123,
                    "sourceAccountId": 10,
                    "destinationAccountId": 20,
                    "amount": 250.00,
                    "currency": "USD"
                }
                """;

        kafkaTemplate.send(TOPIC, "123", payload);

        verify(notificationService, timeout(5000)).sendTransactionCompletedNotification(payload);

    }


}
