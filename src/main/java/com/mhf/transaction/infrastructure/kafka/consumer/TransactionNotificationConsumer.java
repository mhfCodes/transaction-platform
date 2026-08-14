package com.mhf.transaction.infrastructure.kafka.consumer;

import com.mhf.transaction.service.notification.NotificationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TransactionNotificationConsumer {

    private final NotificationService notificationService;

    public TransactionNotificationConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "transaction-events")
    public void consume(String payload) {
        notificationService.sendTransactionCompletedNotification(payload);
    }

}
