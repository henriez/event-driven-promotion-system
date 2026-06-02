package com.utfpr.ms_gateway.controller;

import com.utfpr.ms_gateway.config.RabbitMQConfig;
import com.utfpr.ms_gateway.dto.PromotionDTO;
import com.utfpr.ms_gateway.dto.UpvoteEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import java.util.List;

@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private static final Logger log = LoggerFactory.getLogger(PromotionController.class);

    private final RabbitTemplate rabbitTemplate;
    private final RestTemplate restTemplate;
    private final String promotionServiceUrl;

    public PromotionController(RabbitTemplate rabbitTemplate, RestTemplate restTemplate,
            @Value("${promotion.service.url:http://localhost:8081}") String promotionServiceUrl) {
        this.rabbitTemplate = rabbitTemplate;
        this.restTemplate = restTemplate;
        this.promotionServiceUrl = promotionServiceUrl;
    }

    @PostMapping
    public ResponseEntity<Void> createPromotion(@Valid @RequestBody PromotionDTO dto) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_RECEIVED, dto);
        log.info("Published promotion event to {} / {}", RabbitMQConfig.ROUTING_KEY_RECEIVED, dto.title());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> getPromotions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        String url = promotionServiceUrl + "/api/promotions";
        StringBuilder sb = new StringBuilder(url);
        String sep = "?";
        if (category != null && !category.isBlank()) {
            sb.append(sep).append("category=").append(category);
            sep = "&";
        }
        if (search != null && !search.isBlank()) {
            sb.append(sep).append("search=").append(search);
        }
        String target = sb.toString();
        log.info("Proxying GET /api/promotions to {}", target);
        List<PromotionDTO> result = restTemplate.exchange(
                target, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<PromotionDTO>>() {}
        ).getBody();
        log.info("GET /api/promotions returned {} results", result != null ? result.size() : 0);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<Void> upvotePromotion(@PathVariable Long id) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_UPVOTE, new UpvoteEvent(id));
        log.info("Published upvote event for promotion {}", id);
        return ResponseEntity.accepted().build();
    }
}
