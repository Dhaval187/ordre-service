package com.aspire.blog.order.domain.event.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.aspire.blog.order.config.Constants;
import com.aspire.blog.order.domain.event.OrderEvent;
import com.aspire.blog.order.service.OrderKafkaProducer;
import com.google.gson.Gson;

@Component
public class OrderEventListener implements ApplicationListener<OrderEvent> {

	private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

	@Autowired
	private OrderKafkaProducer orderKafkaProducer;

	/**
	 * To handle After Commit phase of transaction for order.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void processEventCommit(OrderEvent event) {
		log.debug("Order Event received AFTER_COMMIT: {}", event);
		orderKafkaProducer.sendMessage(Constants.TOPIC_ORDER_PLACED, new Gson().toJson(event.getOrder()));
	}

	/**
	 * To handle After Commit phase of transaction for order.
	 * 
	 * @param event
	 */
	@TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
	public void processEventRollback(OrderEvent event) {
		log.debug("Order Event received AFTER_ROLLBACK: {}", event);
		orderKafkaProducer.sendMessage(Constants.TOPIC_ORDER_FAILED, new Gson().toJson(event.getOrder()));
	}

	@Override
	public void onApplicationEvent(OrderEvent event) {
		log.debug("Received spring custom event for order: {}", event);
	}

}
