package com.aspire.blog.order.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aspire.blog.order.service.OrderService;
import com.aspire.blog.order.service.dto.OrderDTO;
import com.aspire.blog.order.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.jsonwebtoken.io.IOException;

/**
 * REST controller for managing {@link com.aspire.blog.order.domain.Order}.
 */
@RestController
@RequestMapping("/api")
public class OrderResource {

	private final Logger log = LoggerFactory.getLogger(OrderResource.class);

	private static final String ENTITY_NAME = "orderOrder";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	private final OrderService orderService;

	public OrderResource(OrderService orderService) {
		this.orderService = orderService;
	}

	/**
	 * {@code POST  /orders} : Create a new order.
	 *
	 * @param orderDTO the orderDTO to create.
	 * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
	 *         body the new orderDTO, or with status {@code 400 (Bad Request)} if
	 *         the order has already an ID.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 */
	@PostMapping("/orders")
	public ResponseEntity<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) throws URISyntaxException {
		log.debug("REST request to save Order : {}", orderDTO);
		if (orderDTO.getId() != null) {
			throw new BadRequestAlertException("A new order cannot already have an ID", ENTITY_NAME, "idexists");
		}
		OrderDTO result = orderService.save(orderDTO);
		return ResponseEntity
				.created(new URI("/api/orders/" + result.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
				.body(result);
	}

	/**
	 * {@code PUT  /orders} : Updates an existing order.
	 *
	 * @param orderDTO the orderDTO to update.
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
	 *         the updated orderDTO, or with status {@code 400 (Bad Request)} if the
	 *         orderDTO is not valid, or with status
	 *         {@code 500 (Internal Server Error)} if the orderDTO couldn't be
	 *         updated.
	 * @throws URISyntaxException if the Location URI syntax is incorrect.
	 */
	@PutMapping("/orders")
	public ResponseEntity<OrderDTO> updateOrder(@RequestBody Long orderId) throws URISyntaxException {
		log.debug("REST request to update Order : {}", orderId);
		if (orderId == null) {
			throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
		}
		OrderDTO result = orderService.completeTransaction(orderId);
		return ResponseEntity.ok()
				.headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, orderId.toString()))
				.body(result);
	}

	/**
	 * {@code GET  /orders} : get all the orders.
	 *
	 * 
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
	 *         of orders in body.
	 */
	@GetMapping("/orders")
	public List<OrderDTO> getAllOrders() {
		log.debug("REST request to get all Orders");
		return orderService.findAll();
	}

	/**
	 * {@code DELETE  /orders/:id} : delete the "id" order.
	 *
	 * @param id the id of the orderDTO to delete.
	 * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
	 */
	@DeleteMapping("/orders/{id}")
	public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
		log.debug("REST request to delete Order : {}", id);
		orderService.delete(id);
		return ResponseEntity.noContent()
				.headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
				.build();
	}

	/**
	 * {@code GET  /orders} : get all the orders.
	 *
	 * 
	 * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
	 *         of orders in body.
	 * @throws java.io.IOException
	 */
	@GetMapping("/orders/{type}")
	public ResponseEntity<Resource> exportAllOrders(@PathVariable String type, HttpServletRequest request)
			throws IOException, java.io.IOException {
		log.debug("REST request to export all Orders");
		// Load file as Resource
		Resource resource = orderService.exportAll(type);
		// Try to determine file's content type
		String contentType = null;
		try {
			contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
		} catch (IOException ex) {
			log.info("Could not determine file type.");
		}

		// Fallback to the default content type if type could not be determined
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.contentLength(resource.getFile().length())
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + resource.getFilename())
				.headers(HeaderUtil.createAlert(applicationName, "Orders exported successfully", resource.toString()))
				.body(resource);
	}

}
