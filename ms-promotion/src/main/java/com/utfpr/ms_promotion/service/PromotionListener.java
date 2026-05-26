package com.utfpr.ms_promotion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import com.utfpr.ms_promotion.dto.PromotionDTO;
import com.utfpr.ms_promotion.entity.Promotion;
import com.utfpr.ms_promotion.repository.PromotionRepository;

@Service
public class PromotionListener {
    private static final Logger log = LoggerFactory.getLogger(PromotionListener.class);

    private final PromotionRepository repository;

    public PromotionListener(PromotionRepository repository) {
        this.repository = repository;
    }

    @RabbitListener(queues = "promotion.received.queue")
    public void receivePromotion(PromotionDTO dto) {
        log.info("Received promotion", dto);
        Promotion promotion = new Promotion(dto.title(), dto.category(), dto.storeId(), "RECEIVED");
        repository.save(promotion);
        log.info("Saved promotion in database");
    }
}