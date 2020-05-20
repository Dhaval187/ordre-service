package com.aspire.blog.order.service.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;

import com.aspire.blog.order.config.Constants;
import com.aspire.blog.order.domain.Order;
import com.aspire.blog.order.domain.event.OrderEvent;
import com.aspire.blog.order.report.SimpleReportExporter;
import com.aspire.blog.order.repository.OrderRepository;
import com.aspire.blog.order.service.OrderKafkaProducer;
import com.aspire.blog.order.service.OrderService;
import com.aspire.blog.order.service.dto.OrderDTO;
import com.aspire.blog.order.service.mapper.OrderMapper;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.util.JRSaver;

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

	private final Path fileStorageLocation;

//	@Autowired
//	private ApplicationProperties applicationProperties;

	public OrderServiceImpl(OrderRepository orderRepository, OrderMapper orderMapper) throws Exception {
		this.orderRepository = orderRepository;
		this.orderMapper = orderMapper;
//		this.fileStorageLocation = Paths.get(this.applicationProperties.getUploadDir()).toAbsolutePath().normalize();
		this.fileStorageLocation = Paths.get("../Docs").toAbsolutePath().normalize();

		try {
			Files.createDirectories(this.fileStorageLocation);
		} catch (Exception ex) {
			throw new Exception("Could not create the directory where the uploaded files will be stored.", ex);
		}
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
	 * Export orders
	 * 
	 * @param type
	 */
	@Override
	@Transactional(readOnly = true)
	public Resource exportAll(String type) {
		try {
			File file = ResourceUtils.getFile("classpath:example.jrxml");
			JasperReport jasperReport = JasperCompileManager.compileReport(file.getAbsolutePath());
			JRSaver.saveObject(jasperReport, "exampleReport.jasper");
			JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(orderRepository.findAll());
			Map<String, Object> parameters = new HashMap<>();
			parameters.put("userName", "Dhaval's Orders");
			JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);

			SimpleReportExporter simpleReportExporter = new SimpleReportExporter(jasperPrint);
			String fileName = "";
			switch (type) {
			case "PDF": 
			case "PRINT":
				fileName = "example.pdf";
//				JasperExportManager.exportReportToPdfFile(jasperPrint, this.fileStorageLocation + "/example.pdf");
				simpleReportExporter.exportToPdf(this.fileStorageLocation + "/" + fileName, "DHAVAL");
				break;
			case "XLSX":
				fileName = "Example.xlsx";
				simpleReportExporter.exportToXlsx(this.fileStorageLocation + "/" + fileName, "Example");
				break;
			case "CSV":
				fileName = "example.csv";
				simpleReportExporter.exportToCsv(this.fileStorageLocation + "/" + fileName);
				break;
			default:
				break;
			}

			return loadFileAsResource(fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JRException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private Resource loadFileAsResource(String fileName) throws IOException {
		try {
			Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
			Resource resource = new UrlResource(filePath.toUri());
			if (resource.exists()) {
				return resource;
			} else {
				throw new FileNotFoundException("File not found " + fileName);
			}
		} catch (MalformedURLException ex) {
			throw new FileNotFoundException("File not found " + fileName);
		}
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
