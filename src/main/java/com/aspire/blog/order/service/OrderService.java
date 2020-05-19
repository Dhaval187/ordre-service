package com.aspire.blog.order.service;

import java.util.List;

import org.springframework.core.io.Resource;

import com.aspire.blog.order.service.dto.OrderDTO;

/**
 * Service Interface for managing {@link com.aspire.blog.order.domain.Order}.
 */
public interface OrderService {

	/**
	 * Save a order.
	 *
	 * @param orderDTO the entity to save.
	 * @return the persisted entity.
	 */
	OrderDTO save(OrderDTO orderDTO);

	/**
	 * Get all the orders.
	 *
	 * @return the list of entities.
	 */
	List<OrderDTO> findAll();

	/**
	 * Export orders
	 * 
	 * @param type
	 */
	Resource exportAll(String type);

	/**
	 * Delete order by id
	 * 
	 * @param id
	 */
	void delete(Long id);

	/**
	 * Complete order transaction
	 * 
	 * @param id
	 * @return
	 */
	OrderDTO completeTransaction(Long id);
}
