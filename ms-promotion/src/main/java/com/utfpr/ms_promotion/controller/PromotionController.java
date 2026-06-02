package com.utfpr.ms_promotion.controller;

import com.utfpr.ms_promotion.dto.PromotionDTO;
import com.utfpr.ms_promotion.entity.Promotion;
import com.utfpr.ms_promotion.entity.PromotionMetric;
import com.utfpr.ms_promotion.repository.PromotionMetricRepository;
import com.utfpr.ms_promotion.repository.PromotionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private static final Logger log = LoggerFactory.getLogger(PromotionController.class);

    private final PromotionRepository promotionRepository;
    private final PromotionMetricRepository metricRepository;

    public PromotionController(PromotionRepository promotionRepository, PromotionMetricRepository metricRepository) {
        this.promotionRepository = promotionRepository;
        this.metricRepository = metricRepository;
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> getPromotions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<Promotion> promotions;
        if (category != null && !category.isBlank()) {
            promotions = promotionRepository.findByCategoryIgnoreCase(category);
        } else if (search != null && !search.isBlank()) {
            promotions = promotionRepository.findFiltered(null, search);
        } else {
            promotions = promotionRepository.findFiltered(null, null);
        }

        List<PromotionDTO> dtos = promotions.stream().map(p -> {
            Integer upvotes = metricRepository.findById(p.getId())
                    .map(PromotionMetric::getUpvotes)
                    .orElse(0);
            return new PromotionDTO(
                p.getId(), p.getTitle(), p.getDescription(),
                p.getPrice(), p.getOriginalPrice(),
                p.getCategory(), p.getStoreId(), p.getUrl(),
                p.getValidUntil(), upvotes
            );
        }).toList();

        log.info("GET /api/promotions returned {} results (category={}, search={})", dtos.size(), category, search);
        return ResponseEntity.ok(dtos);
    }
}