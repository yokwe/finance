package yokwe.finance.securities.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;

public final class RiskMetrics {
	private static final Logger logger = LoggerFactory.getLogger(RiskMetrics.class);

	static void valueAtRisk(Connection connection, LocalDate dateFrom, LocalDate dateTo, String symbol) {
		double data[] = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo).stream().mapToDouble(o -> o.close).toArray();
		double lr[] = DoubleUtil.logReturn(data).toArray();
		double sd[] = DoubleUtil.getStandardDeviation(DoubleUtil.DEFAULT_ALPHA, lr);

		logger.info("");
		for(int i = lr.length - 10; i < lr.length; i++) {
			double oneDay = 1000 * sd[i] * 0.01;
			
			logger.info("{}", String.format("%-5s %8.3f  %8.3f  %8.3f, %8.3f, %8.3f", symbol, data[i + 1], lr[i], sd[i], oneDay, oneDay * Math.sqrt(21)));
		}
	}
		
	public static void main(String args[]) {
		// Difference between relative return and log return
		{
			double[] data = {
				0.67654,
				0.67732,
				0.67422,
				0.67485,
				0.67604,
				0.67545,
				0.67449,
				0.67668,
				0.67033,
				0.66680,
				0.66609,
				0.66503,
			};
			
			double lr[] = DoubleUtil.logReturn(data).toArray();
			double sd[] = DoubleUtil.getStandardDeviation(DoubleUtil.DEFAULT_ALPHA, lr);

			logger.info("");
			for(int i = 0; i < lr.length; i++) {
				double oneDay = 1000 * sd[i] * 0.01;
				
				logger.info("SD {}", String.format("%8.3f  %8.3f  %8.3f, %8.3f, %8.3f", data[i + 1], lr[i], sd[i], oneDay, oneDay * Math.sqrt(21)));
			}
		}
		
		// Calculation of Value at Risk
		{
			try {
				Class.forName("org.sqlite.JDBC");
			} catch (ClassNotFoundException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
			final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/Securities/tmp/sqlite/securities.sqlite3";

			try (Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
				LocalDate dateFrom = LocalDate.now().minusYears(10);
				LocalDate dateTo   = LocalDate.now();
				
				logger.info("");
				logger.info("{}", String.format("Date range  %s - %s", dateFrom, dateTo));
				valueAtRisk(connection, dateFrom, dateTo, "QQQ");
				valueAtRisk(connection, dateFrom, dateTo, "VYM");
				valueAtRisk(connection, dateFrom, dateTo, "VCLT");
			} catch (SQLException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
			
			{
				Random random = new Random(System.currentTimeMillis());
				double data[] = new double[9999];
				for(int i = 0; i < data.length; i++) data[i] = random.nextGaussian();
				{
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Arrays.stream(data).forEach(o -> stats.addValue(o));
					
					logger.info("");
					logger.info("{}", String.format("gaus %4d  min %8.3f  max %8.3f  mean %8.3f  sd %8.3f  skew %8.3f  kurt %8.3f",
							stats.getN(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation(), stats.getSkewness(), stats.getKurtosis()));
				}
			}
			{
				Random random = new Random(System.currentTimeMillis());
				double data[] = new double[9999];
				for(int i = 0; i < data.length; i++) data[i] = random.nextDouble();
				{
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Arrays.stream(data).forEach(o -> stats.addValue(o));
					
					logger.info("");
					logger.info("{}", String.format("rand %4d  min %8.3f  max %8.3f  mean %8.3f  sd %8.3f  skew %8.3f  kurt %8.3f",
							stats.getN(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation(), stats.getSkewness(), stats.getKurtosis()));
				}
			}
		}
	}
}
