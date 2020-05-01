package com.aspire.blog.order.service.mapper;

import org.mapstruct.Mapper;

import com.aspire.blog.order.domain.Order;
import com.aspire.blog.order.service.dto.OrderDTO;

/**
 * Mapper for the entity {@link Order} and its DTO {@link OrderDTO}.
 */
@Mapper(componentModel = "spring", uses = {})
public interface OrderMapper extends EntityMapper<OrderDTO, Order> {

	default Order fromId(Long id) {
		if (id == null) {
			return null;
		}
		Order order = new Order();
		order.setId(id);
		return order;
	}
}
