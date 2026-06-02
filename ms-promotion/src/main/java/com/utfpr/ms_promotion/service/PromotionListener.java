package com.utfpr.ms_promotion.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utfpr.ms_promotion.config.RabbitMQConfig;
import com.utfpr.ms_promotion.dto.PromotionDTO;
import com.utfpr.ms_promotion.entity.Promotion;
import com.utfpr.ms_promotion.entity.PromotionMetric;
import com.utfpr.ms_promotion.repository.PromotionMetricRepository;
import com.utfpr.ms_promotion.repository.PromotionRepository;

@Service
public class PromotionListener {

    private static final Logger log = LoggerFactory.getLogger(PromotionListener.class);

    private final PromotionRepository promotionRepository;
    private final PromotionMetricRepository metricRepository;
    private final RabbitTemplate rabbitTemplate;

    public PromotionListener(PromotionRepository promotionRepository, PromotionMetricRepository metricRepository, RabbitTemplate rabbitTemplate) {
        this.promotionRepository = promotionRepository;
        this.metricRepository = metricRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = "promotion.received.queue")
    @Transactional
    public void receivePromotion(PromotionDTO dto) {
        log.info("Processing promotion: {}", dto.title());

        validatePromotion(dto);

        Promotion promotion = new Promotion(dto.title(), dto.description(), dto.price(), dto.originalPrice(), dto.category(), dto.storeId(), dto.url(), "ACTIVE", dto.validUntil());
        promotion = promotionRepository.save(promotion);

        PromotionMetric metric = new PromotionMetric();
        metric.setPromotionId(promotion.getId());
        metric.setUpvotes(0);
        metric.setHeatScore(BigDecimal.ZERO);
        metric.setLastCalculated(LocalDateTime.now());
        metricRepository.save(metric);

        log.info("Promotion {} saved with id {}", promotion.getTitle(), promotion.getId());

        PromotionDTO createdDto = new PromotionDTO(promotion.getId(), dto.title(), dto.description(), dto.price(), dto.originalPrice(), dto.category(), dto.storeId(), dto.url(), dto.validUntil(), 0);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_CREATED, createdDto);

        if (promotion.getPrice().compareTo(BigDecimal.ONE) < 0) {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_HOT, createdDto);
        }
    }

    private void validatePromotion(PromotionDTO dto) {
        if (dto.title() == null || dto.title().isBlank()) {
            reject("Title is required");
        }
        if (dto.price() == null || dto.price().compareTo(BigDecimal.ZERO) <= 0) {
            reject("Price must be positive");
        }
        if (dto.originalPrice() != null && dto.originalPrice().compareTo(dto.price()) <= 0) {
            reject("Original price must be greater than price");
        }
        if (dto.category() == null || dto.category().isBlank()) {
            reject("Category is required");
        }
        if (dto.storeId() == null || dto.storeId().isBlank()) {
            reject("Store ID is required");
        }
        if (dto.url() == null || dto.url().isBlank()) {
            reject("URL is required");
        }
        if (dto.validUntil() != null && dto.validUntil().isBefore(LocalDateTime.now())) {
            reject("validUntil must be in the future");
        }
    }

    private void reject(String reason) {
        log.warn("Promotion validation failed: {}", reason);
        throw new AmqpRejectAndDontRequeueException("Validation failed: " + reason);
    }
}
