package com.utfpr.ms_promotion.listener;

import com.utfpr.ms_promotion.dto.UpvoteEvent;
import com.utfpr.ms_promotion.service.HeatScoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UpvoteListener {

    private static final Logger log = LoggerFactory.getLogger(UpvoteListener.class);
    private final HeatScoreManager heatScoreManager;

    public UpvoteListener(HeatScoreManager heatScoreManager) {
        this.heatScoreManager = heatScoreManager;
    }

    @RabbitListener(queues = "promotion.upvote.queue")
    public void onUpvote(UpvoteEvent event) {
        log.info("Recording upvote for promotion {}", event.promotionId());
        heatScoreManager.recordUpvote(event.promotionId());
    }
}
