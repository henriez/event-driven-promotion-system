package com.utfpr.ms_gateway.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "promotion.exchange";
    public static final String QUEUE_RECEIVED = "promotion.received.queue";
    public static final String ROUTING_KEY_RECEIVED = "promotion.received";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue receivedQueue() {
        return new Queue(QUEUE_RECEIVED, true);
    }

    @Bean
    public Binding bindingReceived(Queue receivedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(receivedQueue).to(exchange).with(ROUTING_KEY_RECEIVED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}