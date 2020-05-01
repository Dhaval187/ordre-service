package com.aspire.blog.order.web.rest;

import static com.aspire.blog.order.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import com.aspire.blog.order.OrderApp;
import com.aspire.blog.order.domain.Order;
import com.aspire.blog.order.repository.OrderRepository;
import com.aspire.blog.order.service.OrderService;
import com.aspire.blog.order.service.dto.OrderDTO;
import com.aspire.blog.order.service.mapper.OrderMapper;
import com.aspire.blog.order.web.rest.errors.ExceptionTranslator;

/**
 * Integration tests for the {@link OrderResource} REST controller.
 */
@EmbeddedKafka
@SpringBootTest(classes = OrderApp.class)
public class OrderResourceIT {

	private static final String DEFAULT_NAME = "AAAAAAAAAA";
	private static final String UPDATED_NAME = "BBBBBBBBBB";

	private static final Double DEFAULT_AMOUNT = 1D;
	private static final Double UPDATED_AMOUNT = 2D;
	private static final Double SMALLER_AMOUNT = 1D - 1D;

	private static final Long DEFAULT_INVENTORY = 1L;
	private static final Long UPDATED_INVENTORY = 2L;
	private static final Long SMALLER_INVENTORY = 1L - 1L;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private OrderMapper orderMapper;

	@Autowired
	private OrderService orderService;

	@Autowired
	private MappingJackson2HttpMessageConverter jacksonMessageConverter;

	@Autowired
	private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

	@Autowired
	private ExceptionTranslator exceptionTranslator;

	@Autowired
	private EntityManager em;

	@Autowired
	private Validator validator;

	private MockMvc restOrderMockMvc;

	private Order order;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.initMocks(this);
		final OrderResource orderResource = new OrderResource(orderService);
		this.restOrderMockMvc = MockMvcBuilders.standaloneSetup(orderResource)
				.setCustomArgumentResolvers(pageableArgumentResolver).setControllerAdvice(exceptionTranslator)
				.setConversionService(createFormattingConversionService()).setMessageConverters(jacksonMessageConverter)
				.setValidator(validator).build();
	}

	/**
	 * Create an entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it, if
	 * they test an entity which requires the current entity.
	 */
	public static Order createEntity(EntityManager em) {
		Order order = new Order().name(DEFAULT_NAME).amount(DEFAULT_AMOUNT).inventory(DEFAULT_INVENTORY);
		return order;
	}

	/**
	 * Create an updated entity for this test.
	 *
	 * This is a static method, as tests for other entities might also need it, if
	 * they test an entity which requires the current entity.
	 */
	public static Order createUpdatedEntity(EntityManager em) {
		Order order = new Order().name(UPDATED_NAME).amount(UPDATED_AMOUNT).inventory(UPDATED_INVENTORY);
		return order;
	}

	@BeforeEach
	public void initTest() {
		order = createEntity(em);
	}

	@Test
	@Transactional
	public void createOrder() throws Exception {
		int databaseSizeBeforeCreate = orderRepository.findAll().size();

		// Create the Order
		OrderDTO orderDTO = orderMapper.toDto(order);
		restOrderMockMvc.perform(post("/api/orders").contentType(TestUtil.APPLICATION_JSON_UTF8)
				.content(TestUtil.convertObjectToJsonBytes(orderDTO))).andExpect(status().isCreated());

		// Validate the Order in the database
		List<Order> orderList = orderRepository.findAll();
		assertThat(orderList).hasSize(databaseSizeBeforeCreate + 1);
		Order testOrder = orderList.get(orderList.size() - 1);
		assertThat(testOrder.getName()).isEqualTo(DEFAULT_NAME);
		assertThat(testOrder.getAmount()).isEqualTo(DEFAULT_AMOUNT);
		assertThat(testOrder.getInventory()).isEqualTo(DEFAULT_INVENTORY);
	}

	@Test
	@Transactional
	public void createOrderWithExistingId() throws Exception {
		int databaseSizeBeforeCreate = orderRepository.findAll().size();

		// Create the Order with an existing ID
		order.setId(1L);
		OrderDTO orderDTO = orderMapper.toDto(order);

		// An entity with an existing ID cannot be created, so this API call must fail
		restOrderMockMvc.perform(post("/api/orders").contentType(TestUtil.APPLICATION_JSON_UTF8)
				.content(TestUtil.convertObjectToJsonBytes(orderDTO))).andExpect(status().isBadRequest());

		// Validate the Order in the database
		List<Order> orderList = orderRepository.findAll();
		assertThat(orderList).hasSize(databaseSizeBeforeCreate);
	}

	@Test
	@Transactional
	public void getAllOrders() throws Exception {
		// Initialize the database
		orderRepository.saveAndFlush(order);

		// Get all the orderList
		restOrderMockMvc.perform(get("/api/orders?sort=id,desc")).andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
				.andExpect(jsonPath("$.[*].id").value(hasItem(order.getId().intValue())))
				.andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())))
				.andExpect(jsonPath("$.[*].amount").value(hasItem(DEFAULT_AMOUNT.doubleValue())))
				.andExpect(jsonPath("$.[*].inventory").value(hasItem(DEFAULT_INVENTORY.intValue())));
	}

	@Test
	@Transactional
	public void updateOrder() throws Exception {
		// Initialize the database
		orderRepository.saveAndFlush(order);

		int databaseSizeBeforeUpdate = orderRepository.findAll().size();

		// Update the order
		Order updatedOrder = orderRepository.findById(order.getId()).get();
		// Disconnect from session so that the updates on updatedOrder are not directly
		// saved in db
		em.detach(updatedOrder);
		updatedOrder.name(UPDATED_NAME).amount(UPDATED_AMOUNT).inventory(UPDATED_INVENTORY);
		OrderDTO orderDTO = orderMapper.toDto(updatedOrder);

		restOrderMockMvc.perform(put("/api/orders").contentType(TestUtil.APPLICATION_JSON_UTF8)
				.content(TestUtil.convertObjectToJsonBytes(orderDTO.getId()))).andExpect(status().isOk());

		// Validate the Order in the database
		List<Order> orderList = orderRepository.findAll();
		assertThat(orderList).hasSize(databaseSizeBeforeUpdate);
		Order testOrder = orderList.get(orderList.size() - 1);
		assertThat(testOrder.getName()).isEqualTo(DEFAULT_NAME);
		assertThat(testOrder.getAmount()).isEqualTo(DEFAULT_AMOUNT);
		assertThat(testOrder.getInventory()).isEqualTo(DEFAULT_INVENTORY);
	}

	@Test
	@Transactional
	public void updateNonExistingOrder() throws Exception {
		int databaseSizeBeforeUpdate = orderRepository.findAll().size();

		// Create the Order
		OrderDTO orderDTO = orderMapper.toDto(order);

		// If the entity doesn't have an ID, it will throw BadRequestAlertException
		restOrderMockMvc.perform(put("/api/orders").contentType(TestUtil.APPLICATION_JSON_UTF8)
				.content(TestUtil.convertObjectToJsonBytes(orderDTO))).andExpect(status().isBadRequest());

		// Validate the Order in the database
		List<Order> orderList = orderRepository.findAll();
		assertThat(orderList).hasSize(databaseSizeBeforeUpdate);
	}

	@Test
	@Transactional
	public void deleteOrder() throws Exception {
		// Initialize the database
		orderRepository.saveAndFlush(order);

		int databaseSizeBeforeDelete = orderRepository.findAll().size();

		// Delete the order
		restOrderMockMvc.perform(delete("/api/orders/{id}", order.getId()).accept(TestUtil.APPLICATION_JSON_UTF8))
				.andExpect(status().isNoContent());

		// Validate the database contains one less item
		List<Order> orderList = orderRepository.findAll();
		assertThat(orderList).hasSize(databaseSizeBeforeDelete - 1);
	}

	@Test
	@Transactional
	public void equalsVerifier() throws Exception {
		TestUtil.equalsVerifier(Order.class);
		Order order1 = new Order();
		order1.setId(1L);
		Order order2 = new Order();
		order2.setId(order1.getId());
		assertThat(order1).isEqualTo(order2);
		order2.setId(2L);
		assertThat(order1).isNotEqualTo(order2);
		order1.setId(null);
		assertThat(order1).isNotEqualTo(order2);
	}

	@Test
	@Transactional
	public void dtoEqualsVerifier() throws Exception {
		TestUtil.equalsVerifier(OrderDTO.class);
		OrderDTO orderDTO1 = new OrderDTO();
		orderDTO1.setId(1L);
		OrderDTO orderDTO2 = new OrderDTO();
		assertThat(orderDTO1).isNotEqualTo(orderDTO2);
		orderDTO2.setId(orderDTO1.getId());
		assertThat(orderDTO1).isEqualTo(orderDTO2);
		orderDTO2.setId(2L);
		assertThat(orderDTO1).isNotEqualTo(orderDTO2);
		orderDTO1.setId(null);
		assertThat(orderDTO1).isNotEqualTo(orderDTO2);
	}

	@Test
	@Transactional
	public void testEntityFromId() {
		assertThat(orderMapper.fromId(42L).getId()).isEqualTo(42);
		assertThat(orderMapper.fromId(null)).isNull();
	}
}
