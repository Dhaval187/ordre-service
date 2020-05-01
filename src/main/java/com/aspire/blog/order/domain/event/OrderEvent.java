package com.aspire.blog.order.domain.event;

import org.springframework.context.ApplicationEvent;

import com.aspire.blog.order.domain.Order;

public class OrderEvent extends ApplicationEvent {

	private final Order order;

	public OrderEvent(Order order) {
		super(order);
		this.order = order;
	}

	public Order getOrder() {
		return order;
	}

	@Override
	public String toString() {
		return "OrderEvent [order=" + order + "]";
	}
}
