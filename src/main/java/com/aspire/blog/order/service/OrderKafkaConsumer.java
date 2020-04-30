package com.aspire.blog.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrderKafkaConsumer {

    private final Logger log = LoggerFactory.getLogger(OrderKafkaConsumer.class);
    private static final String TOPIC = "topic_order";

    @KafkaListener(topics = "topic_order", groupId = "group_id")
    public void consume(String message) throws IOException {
        log.info("Consumed message in {} : {}", TOPIC, message);
    }
}
