package com.utfpr.ms_notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "promotion.exchange";
    public static final String QUEUE_HOT = "promotion.hot.queue";
    public static final String ROUTING_KEY_HOT = "promotion.hot";
    public static final String QUEUE_NOTIFICATION_HOT = "notification.hot.queue";
    public static final String ROUTING_KEY_NOTIFICATION_HOT = "notification.hot";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue hotQueue() {
        return new Queue(QUEUE_HOT, true);
    }

    @Bean
    public Queue notificationHotQueue() {
        return new Queue(QUEUE_NOTIFICATION_HOT, true);
    }

    @Bean
    public Binding bindingHot(Queue hotQueue, TopicExchange exchange) {
        return BindingBuilder.bind(hotQueue).to(exchange).with(ROUTING_KEY_HOT);
    }

    @Bean
    public Binding bindingNotificationHot(Queue notificationHotQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationHotQueue).to(exchange).with(ROUTING_KEY_NOTIFICATION_HOT);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
