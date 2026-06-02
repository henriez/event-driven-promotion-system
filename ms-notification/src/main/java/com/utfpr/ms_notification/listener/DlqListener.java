package com.utfpr.ms_notification.listener;

import com.utfpr.ms_notification.dto.PromotionEvent;
import com.utfpr.ms_notification.entity.StoreContact;
import com.utfpr.ms_notification.repository.StoreContactRepository;
import com.utfpr.ms_notification.service.ResendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DlqListener {

    private static final Logger log = LoggerFactory.getLogger(DlqListener.class);
    private final StoreContactRepository storeContactRepository;
    private final ResendClient resendClient;

    public DlqListener(StoreContactRepository storeContactRepository, ResendClient resendClient) {
        this.storeContactRepository = storeContactRepository;
        this.resendClient = resendClient;
    }

    @RabbitListener(queues = "promotion.dlq.queue")
    public void onDlqMessage(PromotionEvent event) {
        log.warn("DLQ message received for promotion: {} from store: {}", event.title(), event.storeId());

        StoreContact contact = storeContactRepository.findByStoreId(event.storeId()).orElse(null);
        if (contact == null) {
            log.warn("No contact found for store: {}", event.storeId());
            return;
        }

        String subject = "Promotion validation failed: " + event.title();
        String text = "Your promotion \"" + event.title() + "\" was rejected due to validation failure.\n"
                + "Store ID: " + event.storeId() + "\n"
                + "Category: " + event.category() + "\n"
                + "Price: " + event.price() + "\n"
                + "Please review and resubmit with corrected data.";

        resendClient.sendEmail(contact.getEmail(), subject, text, 0L);
    }
}
