package com.utfpr.ms_notification.listener;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.utfpr.ms_notification.config.RabbitMQConfig;
import com.utfpr.ms_notification.dto.PromotionEvent;
import com.utfpr.ms_notification.entity.StoreContact;
import com.utfpr.ms_notification.entity.Subscriber;
import com.utfpr.ms_notification.entity.SubscriberPreference;
import com.utfpr.ms_notification.repository.StoreContactRepository;
import com.utfpr.ms_notification.repository.SubscriberPreferenceRepository;
import com.utfpr.ms_notification.repository.SubscriberRepository;
import com.utfpr.ms_notification.service.ResendClient;

@Component
public class HotDealListener {

    private static final Logger log = LoggerFactory.getLogger(HotDealListener.class);
    private final StoreContactRepository storeContactRepository;
    private final SubscriberPreferenceRepository subscriberPreferenceRepository;
    private final SubscriberRepository subscriberRepository;
    private final ResendClient resendClient;
    private final RabbitTemplate rabbitTemplate;

    public HotDealListener(StoreContactRepository storeContactRepository, SubscriberPreferenceRepository subscriberPreferenceRepository, SubscriberRepository subscriberRepository, ResendClient resendClient, RabbitTemplate rabbitTemplate) {
        this.storeContactRepository = storeContactRepository;
        this.subscriberPreferenceRepository = subscriberPreferenceRepository;
        this.subscriberRepository = subscriberRepository;
        this.resendClient = resendClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "promotion.hot.queue")
    public void onHotDeal(PromotionEvent event) {
        log.info("Hot deal received: {} from store: {}", event.title(), event.storeId());

        StoreContact contact = storeContactRepository.findByStoreId(event.storeId()).orElse(null);
        if (contact != null) {
            String storeSubject = "Your promotion is HOT: " + event.title();
            String storeText = "Your promotion \"" + event.title() + "\" in category \""
                    + event.category() + "\" is now a hot deal at $" + event.price() + ".\n"
                    + "URL: " + event.url();
            resendClient.sendEmail(contact.getEmail(), storeSubject, storeText, 0L);
            log.info("Store notification sent to {} for promotion: {}", contact.getEmail(), event.title());
        } else {
            log.warn("No store contact found for storeId: {} on promotion: {}", event.storeId(), event.title());
        }

        List<SubscriberPreference> prefs = subscriberPreferenceRepository.findByCategory(event.category());
        int notifiedCount = 0;
        for (SubscriberPreference pref : prefs) {
            Subscriber subscriber = subscriberRepository.findById(pref.getSubscriberId()).orElse(null);
            if (subscriber == null) {
                log.warn("Subscriber not found for subscriberPreference id: {}", pref.getId());
                continue;
            }
            String subSubject = "Hot Deal Alert: " + event.title();
            String subText = "A hot deal is available in category \"" + event.category() + "\":\n"
                    + event.title() + " - $" + event.price() + "\n"
                    + event.url();
            resendClient.sendEmail(subscriber.getEmail(), subSubject, subText, 0L);
            notifiedCount++;
        }
        log.info("Subscriber notifications sent: {} for category: {} on promotion: {}", notifiedCount, event.category(), event.title());

        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_NOTIFICATION_HOT, event);
        log.info("Published notification.hot for promotion: {}", event.title());
    }
}
