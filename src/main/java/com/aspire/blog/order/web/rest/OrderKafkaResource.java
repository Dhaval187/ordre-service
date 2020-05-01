package com.aspire.blog.order.web.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aspire.blog.order.service.OrderKafkaProducer;

@RestController
@RequestMapping(value = "/api/order-kafka")
public class OrderKafkaResource {

	private final Logger log = LoggerFactory.getLogger(OrderKafkaResource.class);

	private OrderKafkaProducer kafkaProducer;

	public OrderKafkaResource(OrderKafkaProducer kafkaProducer) {
		this.kafkaProducer = kafkaProducer;
	}

	@PostMapping(value = "/publish")
	public void sendMessageToKafkaTopic(@RequestParam("topic") String topic, @RequestParam("message") String message) {
		log.debug("REST request to send to Kafka topic the message : {}", message);
		this.kafkaProducer.sendMessage(topic, message);
	}
}
