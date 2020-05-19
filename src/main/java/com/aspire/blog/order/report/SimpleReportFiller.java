package com.aspire.blog.order.report;

import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.springframework.beans.factory.annotation.Autowired;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRSaver;

public class SimpleReportFiller {
	private String reportFileName;

	private JasperReport jasperReport;

	private JasperPrint jasperPrint;

	@Autowired
	private DataSource dataSource;

	private Map<String, Object> parameters;

	public SimpleReportFiller() {
		parameters = new HashMap<>();
	}

	public void prepareReport() {
		compileReport();
		fillReport();
	}

	public void compileReport() {
		try {
			InputStream reportStream = getClass().getResourceAsStream("/".concat(reportFileName));
			jasperReport = JasperCompileManager.compileReport(reportStream);
			JRSaver.saveObject(jasperReport, reportFileName.replace(".jrxml", ".jasper"));
		} catch (JRException ex) {
			System.out.println(ex);
		}
	}

	public void fillReport() {
		try {
			jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource.getConnection());
		} catch (JRException | SQLException ex) {
			System.out.println(ex);
		}
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, Object> parameters) {
		this.parameters = parameters;
	}

	public String getReportFileName() {
		return reportFileName;
	}

	public void setReportFileName(String reportFileName) {
		this.reportFileName = reportFileName;
	}

	public JasperPrint getJasperPrint() {
		return jasperPrint;
	}
}
