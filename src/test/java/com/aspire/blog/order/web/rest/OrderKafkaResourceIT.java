package com.aspire.blog.order.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.aspire.blog.order.OrderApp;
import com.aspire.blog.order.service.OrderKafkaProducer;

@EmbeddedKafka
@SpringBootTest(classes = OrderApp.class)
public class OrderKafkaResourceIT {

	@Autowired
	private OrderKafkaProducer kafkaProducer;

	private MockMvc restMockMvc;

	@BeforeEach
	public void setup() {
		OrderKafkaResource kafkaResource = new OrderKafkaResource(kafkaProducer);

		this.restMockMvc = MockMvcBuilders.standaloneSetup(kafkaResource).build();
	}

	@Test
	public void sendMessageToKafkaTopic() throws Exception {
		restMockMvc.perform(post("/api/order-kafka/publish?topic=test&message=yolo")).andExpect(status().isOk());
	}
}
