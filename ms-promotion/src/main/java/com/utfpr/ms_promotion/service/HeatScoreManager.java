package com.utfpr.ms_promotion.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.utfpr.ms_promotion.config.RabbitMQConfig;
import com.utfpr.ms_promotion.dto.PromotionDTO;
import com.utfpr.ms_promotion.entity.Promotion;
import com.utfpr.ms_promotion.entity.PromotionMetric;
import com.utfpr.ms_promotion.repository.PromotionMetricRepository;
import com.utfpr.ms_promotion.repository.PromotionRepository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class HeatScoreManager {

    private static final Logger log = LoggerFactory.getLogger(HeatScoreManager.class);
    private static final int BATCH_INTERVAL_MS = 10000;
    private static final int HEAT_HOT_THRESHOLD = 10;

    private final PromotionMetricRepository metricRepository;
    private final PromotionRepository promotionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final Map<Long, AtomicInteger> pendingUpvotes = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HeatScoreManager(PromotionMetricRepository metricRepository, PromotionRepository promotionRepository, RabbitTemplate rabbitTemplate) {
        this.metricRepository = metricRepository;
        this.promotionRepository = promotionRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void start() {
        executor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(BATCH_INTERVAL_MS);
                    flush();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        flush();
    }

    public void recordUpvote(Long promotionId) {
        pendingUpvotes.computeIfAbsent(promotionId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void recordClick(Long promotionId) {
        pendingUpvotes.computeIfAbsent(promotionId, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public synchronized void flush() {
        if (pendingUpvotes.isEmpty()) {
            return;
        }
        Map<Long, AtomicInteger> snapshot = new ConcurrentHashMap<>(pendingUpvotes);
        pendingUpvotes.clear();

        for (Map.Entry<Long, AtomicInteger> entry : snapshot.entrySet()) {
            Long promotionId = entry.getKey();
            int count = entry.getValue().get();
            if (count <= 0) {
                continue;
            }
            try {
                PromotionMetric metric = metricRepository.findById(promotionId).orElse(null);
                if (metric == null) {
                    continue;
                }
                int oldUpvotes = metric.getUpvotes();
                int newUpvotes = oldUpvotes + count;
                metric.setUpvotes(newUpvotes);
                BigDecimal score = BigDecimal.valueOf(newUpvotes);
                metric.setHeatScore(score);
                metric.setLastCalculated(LocalDateTime.now());
                metricRepository.save(metric);

                if (newUpvotes >= HEAT_HOT_THRESHOLD && oldUpvotes < HEAT_HOT_THRESHOLD) {
                    log.info("Promotion {} reached hot threshold with {} upvotes", promotionId, newUpvotes);
                    publishHotDeal(promotionId);
                }
            } catch (Exception e) {
                log.error("Failed to flush heat score for promotion {}", promotionId, e);
                pendingUpvotes.computeIfAbsent(promotionId, k -> new AtomicInteger(0)).addAndGet(count);
            }
        }
    }

    private void publishHotDeal(Long promotionId) {
        Optional<Promotion> optPromotion = promotionRepository.findById(promotionId);
        if (optPromotion.isEmpty()) {
            log.warn("Cannot publish hot deal: promotion {} not found", promotionId);
            return;
        }
        Promotion p = optPromotion.get();
        PromotionDTO dto = new PromotionDTO(
            p.getId(), p.getTitle(), p.getDescription(),
            p.getPrice(), p.getOriginalPrice(),
            p.getCategory(), p.getStoreId(), p.getUrl(),
            p.getValidUntil(), 0
        );
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_HOT, dto);
        log.info("Published hot deal for promotion {}: {}", promotionId, p.getTitle());
    }

    @Scheduled(fixedDelay = BATCH_INTERVAL_MS)
    public void scheduledFlush() {
        flush();
    }
}
