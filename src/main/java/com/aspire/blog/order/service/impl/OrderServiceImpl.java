package com.aspire.blog.order.service.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aspire.blog.order.config.Constants;
import com.aspire.blog.order.domain.Order;
import com.aspire.blog.order.domain.event.OrderEvent;
import com.aspire.blog.order.repository.OrderRepository;
import com.aspire.blog.order.service.OrderKafkaProducer;
import com.aspire.blog.order.service.OrderService;
import com.aspire.blog.order.service.dto.OrderDTO;
import com.aspire.blog.order.service.mapper.OrderMapper;

/**
 * Service Implementation for managing {@link Order}.
 */
@Service
@Transactional
public class OrderServiceImpl implements OrderService {

	private final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

	private final OrderRepository orderRepository;

	private final OrderMapper orderMapper;

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;

	@Autowired
	private OrderKafkaProducer orderKafkaProducer;

	public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) {
		this.orderRepository = orderRepository;
		this.orderMapper = orderMapper;
	}

	/**
	 * Save a order.
	 *
	 * @param orderDTO the entity to save.
	 * @return the persisted entity.
	 */
	@Override
	public OrderDTO save(OrderDTO orderDTO) {
		log.debug("Request to save Order : {}", orderDTO);
		Order order = orderMapper.toEntity(orderDTO);
		order.setInProgress(true);
		order = orderRepository.save(order);

		// Publishing event
		OrderEvent orderEvent = new OrderEvent(order);
		applicationEventPublisher.publishEvent(orderEvent);

		return orderMapper.toDto(order);
	}

	/**
	 * Get all the orders.
	 *
	 * @return the list of entities.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<OrderDTO> findAll() {
		log.debug("Request to get all Orders");
		return orderRepository.findAll().stream().map(orderMapper::toDto)
				.collect(Collectors.toCollection(LinkedList::new));
	}

	/**
	 * Delete order by id
	 * 
	 * @param id
	 */
	@Override
	public void delete(Long id) {
		log.debug("Request to delete Order by id : {}", id);
		orderRepository.deleteById(id);
		orderKafkaProducer.sendMessage(Constants.TOPIC_ORDER_FAILED, id.toString());
	}

	/**
	 * Get one order by id.
	 *
	 * @param id the id of the entity.
	 * @return the entity.
	 */
	@Transactional(readOnly = true)
	private Optional<Order> findOne(Long id) {
		log.debug("Request to get Order : {}", id);
		return orderRepository.findById(id);
	}

	/**
	 * Complete order transaction
	 * 
	 * @param id
	 * @return
	 */
	@Override
	public OrderDTO completeTransaction(Long id) {
		log.debug("Request to complete Order trransaction: {}", id);
		findOne(id).map(order -> {
			order.setInProgress(false);
			order = orderRepository.save(order);
			return orderMapper.toDto(order);
		});
		return null;
	}

}
