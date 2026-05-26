package com.utfpr.ms_gateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.utfpr.ms_gateway.config.RabbitMQConfig;
import com.utfpr.ms_gateway.dto.PromotionDTO;

import jakarta.validation.Valid;

@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);
    
    private final RabbitTemplate rabbitTemplate;

    public TestController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/test-publish")
    public String publishTest(@Valid @RequestBody PromotionDTO dto) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_RECEIVED, dto);
        log.info("Sent message to Promotion");
        return "Message sent to RabbitMQ!";
    }
}