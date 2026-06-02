package com.utfpr.ms_gateway.listener;

import com.utfpr.ms_gateway.dto.PromotionDTO;
import com.utfpr.ms_gateway.service.PromotionStore;
import com.utfpr.ms_gateway.service.SseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PromotionEventListener {

    private static final Logger log = LoggerFactory.getLogger(PromotionEventListener.class);
    private final SseService sseService;
    private final PromotionStore promotionStore;

    public PromotionEventListener(SseService sseService, PromotionStore promotionStore) {
        this.sseService = sseService;
        this.promotionStore = promotionStore;
    }

    @RabbitListener(queues = "promotion.created.queue")
    public void onPromotionCreated(PromotionDTO dto) {
        log.info("SSE broadcast promotion.created: {}", dto.title());
        promotionStore.add(dto);
        sseService.broadcast(null, dto);
    }

    @RabbitListener(queues = "notification.hot.queue")
    public void onNotificationHot(PromotionDTO dto) {
        log.info("SSE broadcast notification.hot as hot-deal: {}", dto.title());
        sseService.broadcast("hot-deal", dto);
    }
}
