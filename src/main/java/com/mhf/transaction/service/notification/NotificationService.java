package com.mhf.transaction.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    public void sendTransactionCompletedNotification(String payload) {
        log.info(
                "Sending transaction completed notification: {}",
                payload
        );
    }
}
