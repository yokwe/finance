package yokwe.finance.securities.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;

public final class RiskMetrics {
	private static final Logger logger = LoggerFactory.getLogger(RiskMetrics.class);

	public static double CONFIDENCE_95_PERCENT = 1.62;
	public static double CONFIDENCE_99_PERCENT = 2.33;

	public static double DEFAULT_DECAY_FACTOR = 0.94;
	public static double DEFAULT_CONFIDENCE   = CONFIDENCE_95_PERCENT;
	
	
	// TODO Until here
	
	
	final double              decayFactor;
	final DoubleUnaryOperator recursiveVariance;
	
	public RiskMetrics(double decayFactor) {
		this.decayFactor       = decayFactor;
		this.recursiveVariance = DoubleUtil.ema(DoubleUtil.getAlphaFromDecayFactor(decayFactor));
	}
	public RiskMetrics() {
		this(DEFAULT_DECAY_FACTOR);
	}
	
		
	
	// EWMA variance, covariance and correlation
	private DoubleStream getCovarianceDoubleStream(double data1[], double data2[]) {
		return Arrays.stream(DoubleUtil.multiply(data1, data2)).map(DoubleUtil.emaFromDecayFactor(decayFactor));
	}
	
	public double[] getCovariance(double data1[], double data2[]) {
		return getCovarianceDoubleStream(data1, data2).toArray();
	}
	public double[] getVariance(double data[]) {
		return getCovariance(data, data);
	}
	public double[] getStandardDeviation(double data[]) {
		return getCovarianceDoubleStream(data, data).map(DoubleUtil.sqrt()).toArray();
	}
	public double[] getCorrelation(double data1[], double data2[]) {
		double sd1[] = getStandardDeviation(data1);
		double sd2[] = getStandardDeviation(data2);
		double cov[] = getCovariance(data1, data2);
		
		return DoubleUtil.divide(cov, DoubleUtil.multiply(sd1, sd2));
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
			
			double rr[] = DoubleUtil.relativeReturn(data).toArray();
			double lr[] = DoubleUtil.logReturn(data).toArray();

			
			logger.info("");
			for(int i = 0; i < rr.length; i++) {
				logger.info("{}", String.format("111 %8.3f  %8.3f  %8.3f", data[i + 1], rr[i], lr[i]));
			}
		}
		
		// Calculation of variance, covariance and correlation
		{
			double[] data_a = {
				 0.634,
				 0.115,
				-0.460,
				 0.094,
				 0.176,
				-0.088,
				-0.142,
				 0.324,
				-0.943,
				-0.528,
				-0.107,
				-0.160,
				-0.445,
			 	 0.053,
				 0.152,
				-0.318,
				 0.424,
				-0.708,
				-0.105,
				-0.257,
			};

			double data_b[] = {
				  0.005,
				 -0.532,
				  1.267,
				  0.234,
				  0.095,
				 -0.003,
				 -0.144,
				 -1.643,
				 -0.319,
				 -1.362,
				 -0.367,
				  0.872,
				  0.904,
				  0.390,
				 -0.527,
				  0.311,
				  0.227,
				  0.436,
				  0.568,
				 -0.217,
			};

			RiskMetrics rm = new RiskMetrics();
			double rva[] = rm.getVariance(data_a);
			double rvb[] = rm.getVariance(data_b);
			double cov[] = rm.getCovariance(data_a, data_b);
			double cor[] = rm.getCorrelation(data_a, data_b);
			
			logger.info("");
			for(int i = 0; i < data_a.length; i++) {
				logger.info("{}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", data_a[i], data_b[i], rva[i], rvb[i], cov[i], cor[i]));
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
				double data[] = PriceTable.getAllBySymbolDateRange(connection, "QQQ", dateFrom, dateTo).stream().mapToDouble(o -> o.close).toArray();
				double lr[]   = DoubleUtil.logReturn(data).toArray();
				
				logger.info("");
				logger.info("QQQ {} - {}", dateFrom, dateTo);
				
				{
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Arrays.stream(data).forEach(o -> stats.addValue(o));
					
					logger.info("{}", String.format("data %4d  min %8.3f  max %8.3f  mean %8.3f  sd %8.3f  skew %8.3f  kurt %8.3f",
							stats.getN(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation(), stats.getSkewness(), stats.getKurtosis()));
				}
				{
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Arrays.stream(lr).forEach(o -> stats.addValue(o));
					
					logger.info("{}", String.format("lr   %4d  min %8.3f  max %8.3f  mean %8.3f  sd %8.3f  skew %8.3f  kurt %8.3f",
							stats.getN(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation(), stats.getSkewness(), stats.getKurtosis()));
				}
				
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
					
					logger.info("{}", String.format("rand %4d  min %8.3f  max %8.3f  mean %8.3f  sd %8.3f  skew %8.3f  kurt %8.3f",
							stats.getN(), stats.getMin(), stats.getMax(), stats.getMean(), stats.getStandardDeviation(), stats.getSkewness(), stats.getKurtosis()));
				}
			}
		}
	}
}
