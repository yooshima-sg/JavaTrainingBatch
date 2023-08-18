package com.s_giken.training.batch;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

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
		LocalDate targetYm = null;
		try {
			logger.info("-".repeat(40));

			try {
				targetYm = this.parseArgs(args);
			} catch(Exception e) {
				logger.error("コマンドライン引数が不正です: " + e.toString());
				return;
			}
			this.appendBillingData(targetYm);
		} finally {
			logger.info("-".repeat(40));
		}
	}

	@Transactional
	private void appendBillingData(LocalDate targetYm) {
		int processedCount = 0;
		String sql = "";
		String targetYmStr = targetYm.format(DateTimeFormatter.ofPattern("yyyy年MM月"));

		// 請求データを作成してよい対象年月か確認
		logger.info(String.format("%s分の請求情報を確認しています。", targetYmStr));
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM T_BILLING_STATUS WHERE billing_ym = ? and is_commit = true", Integer.class, targetYm);
		if (count != 0) {
			logger.info(String.format("%s分の請求情報はすでに確定しています。", targetYmStr));
			return;
		}

		// データ挿入前のクリーニング
		logger.info(String.format("データベースから%s分の未確定請求情報を削除しています。", targetYmStr));
		jdbcTemplate.update("DELETE FROM T_BILLING_DETAIL_DATA WHERE billing_ym = ?", targetYm);
		jdbcTemplate.update("DELETE FROM T_BILLING_DATA WHERE billing_ym = ?", targetYm);
		jdbcTemplate.update("DELETE FROM T_BILLING_STATUS WHERE billing_ym = ?", targetYm);
		logger.info("削除しました。");

		// 請求ステータスのデータの作成と挿入
		logger.info(String.format("%s分の請求ステータスを追加しています。", targetYmStr));
		processedCount = jdbcTemplate.update("INSERT INTO T_BILLING_STATUS(billing_ym, is_commit) VALUES (?, false)", targetYm);
		logger.info(String.format("%d件削除しました。", processedCount));
		
		// 請求データの作成と挿入
		logger.info(String.format("%s分の請求データを追加しています。", targetYmStr));
		sql = """
		  INSERT INTO T_BILLING_DATA (
			billing_ym,
			member_id,
			mail,
			name,
			address,
			start_date,
			end_date,
			payment_method,
			amount,
			tax_ratio,
			total)
		  SELECT 
			billing_ym,
			member_id,
			mail,
			name,
			address,
			start_date,
			end_date,
			payment_method,
			sum_amount,
			tax_ratio,
			sum_amount * (1 + tax_ratio) as total
		  FROM (
			SELECT 
			  DATE '@@BILLING_YM@@' as billing_ym,
			  M.member_id,
			  M.mail,
			  M.name,
			  M.address,
			  M.start_date,
			  M.end_date,
			  M.payment_method,
			  (SELECT SUM(amount) 
				 FROM T_CHARGE C 
				WHERE C.start_date < DATEADD(MONTH, 1, DATE '@@BILLING_YM@@' ) AND 
					  (C.end_date IS NULL OR C.end_date >= DATE '@@BILLING_YM@@')) as sum_amount,
			  0.1 as tax_ratio
			FROM T_MEMBER M
			WHERE
			  M.start_date < DATEADD(MONTH, 1, DATE '@@BILLING_YM@@') AND 
			  (M.end_date IS NULL OR M.end_date >= DATE '@@BILLING_YM@@')
		  )""";
		sql = sql.replace("@@BILLING_YM@@", targetYm.format(DateTimeFormatter.ISO_DATE));
		processedCount = jdbcTemplate.update(sql);
		logger.info(String.format("%d件追加しました。", processedCount));

		// 請求明細データの作成と挿入
		logger.info(String.format("%s分の請求明細データを追加しています。", targetYmStr));
		sql = """
			  INSERT INTO T_BILLING_DETAIL_DATA(
				billing_ym,
				member_id,
				charge_id,
				name,
				amount,
				start_date,
				end_date)
			  SELECT 
				DATE '@@BILLING_YM@@' as billng_ym,
				M.member_id,
				C.charge_id,
				C.name,
				C.amount,
				C.start_date,
				C.end_date,
			  FROM 
				(SELECT * FROM T_MEMBER WHERE start_date < DATEADD(MONTH, 1, DATE '@@BILLING_YM@@') AND 
						  (end_date IS NULL OR end_date >= DATE '@@BILLING_YM@@')) M,
				(SELECT * FROM T_CHARGE WHERE start_date < DATEADD(MONTH, 1, DATE '@@BILLING_YM@@') AND 
						  (end_date IS NULL OR end_date >= DATE '@@BILLING_YM@@')) C""";
		sql = sql.replace("@@BILLING_YM@@", targetYm.format(DateTimeFormatter.ISO_DATE));
		processedCount = jdbcTemplate.update(sql);
		logger.info(String.format("%d件追加しました。", processedCount));

	}

	private LocalDate parseArgs(String[] args) throws Exception {
		if(args.length != 1) {
			throw new Exception("コマンドライン引数が正しくありません。");
		}
		var formatter = new DateTimeFormatterBuilder()
							.appendPattern("yyyyMM")
		                    .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
							.toFormatter();
		return LocalDate.parse(args[0], formatter);
	}
}
