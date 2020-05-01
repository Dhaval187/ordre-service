package com.aspire.blog.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.aspire.blog.order.security.SecurityUtils;
import com.google.gson.JsonObject;

@Service
public class OrderKafkaProducer {

	private static final Logger log = LoggerFactory.getLogger(OrderKafkaProducer.class);

	private KafkaTemplate<String, String> kafkaTemplate;

	public OrderKafkaProducer(KafkaTemplate<String, String> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	public void sendMessage(String topic, String message) {
		log.info("Producing message to {} : {}", topic, message);

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("data", message);
		SecurityUtils.getCurrentUserJWT()
				.ifPresent(s -> jsonObject.addProperty("authToken", String.format("%s %s", "Bearer", s)));
		this.kafkaTemplate.send(topic, jsonObject.toString());
	}
}
