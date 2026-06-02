package com.utfpr.ms_gateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "promotion.exchange";
    public static final String QUEUE_RECEIVED = "promotion.received.queue";
    public static final String ROUTING_KEY_RECEIVED = "promotion.received";
    public static final String DLX_NAME = "promotion.dlx";
    public static final String ROUTING_KEY_DLQ = "promotion.dlq";
    public static final String QUEUE_CREATED = "promotion.created.queue";
    public static final String ROUTING_KEY_CREATED = "promotion.created";
    public static final String QUEUE_NOTIFICATION_HOT = "notification.hot.queue";
    public static final String ROUTING_KEY_NOTIFICATION_HOT = "notification.hot";
    public static final String QUEUE_UPVOTE = "promotion.upvote.queue";
    public static final String ROUTING_KEY_UPVOTE = "promotion.upvote";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue receivedQueue() {
        return QueueBuilder.durable(QUEUE_RECEIVED)
                .withArgument("x-dead-letter-exchange", DLX_NAME)
                .withArgument("x-dead-letter-routing-key", ROUTING_KEY_DLQ)
                .build();
    }

    @Bean
    public Queue createdQueue() {
        return new Queue(QUEUE_CREATED, true);
    }

    @Bean
    public Queue notificationHotQueue() {
        return new Queue(QUEUE_NOTIFICATION_HOT, true);
    }

    @Bean
    public Queue upvoteQueue() {
        return new Queue(QUEUE_UPVOTE, true);
    }

    @Bean
    public Binding bindingReceived(Queue receivedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(receivedQueue).to(exchange).with(ROUTING_KEY_RECEIVED);
    }

    @Bean
    public Binding bindingCreated(Queue createdQueue, TopicExchange exchange) {
        return BindingBuilder.bind(createdQueue).to(exchange).with(ROUTING_KEY_CREATED);
    }

    @Bean
    public Binding bindingNotificationHot(Queue notificationHotQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationHotQueue).to(exchange).with(ROUTING_KEY_NOTIFICATION_HOT);
    }

    @Bean
    public Binding bindingUpvote(Queue upvoteQueue, TopicExchange exchange) {
        return BindingBuilder.bind(upvoteQueue).to(exchange).with(ROUTING_KEY_UPVOTE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
