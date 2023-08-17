package com.s_giken.training.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class BatchApplication implements CommandLineRunner {
	final Logger logger = LoggerFactory.getLogger(BatchApplication.class);
	final private JdbcTemplate jdbcTemplate;

	public BatchApplication(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public static void main(String[] args) {
		SpringApplication.run(BatchApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		logger.info("-".repeat(40));

		jdbcTemplate.execute("DROP TABLE IF EXISTS T_BILLING_DATA_DETAIL");
		logger.info("T_BILLING_DATA_DETAIL table is dropped.");

		jdbcTemplate.execute("DROP TABLE IF EXISTS T_BILLING_DATA");
		logger.info("T_BILLING_DATA table is dropped.");

		jdbcTemplate.execute("DROP TABLE IF EXISTS T_BILLING_DATA_STATUS");
		logger.info("T_BILLING_DATA_STATUS table is dropped.");

		logger.info("-".repeat(40));
	}
}
