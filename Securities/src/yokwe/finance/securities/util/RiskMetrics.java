package yokwe.finance.securities.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
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
	
	private static final DoubleUnaryOperator APPLY_SQRT = new DoubleUnaryOperator() {
		@Override
		public double applyAsDouble(double value) {
			return Math.sqrt(value);
		}
	};
	private static DoubleUnaryOperator applySqrt() {
		return APPLY_SQRT;
	}
	
	private static final class RecursiveVariance implements DoubleUnaryOperator {
		private final double decayFactor;
		private double var = Double.NaN;
		
		RecursiveVariance(double decayFactor) {
			this.decayFactor = decayFactor;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(var)) {
				var = value;
			}
			var = value + decayFactor * (var - value);
			return var;
		}
	};
	private DoubleUnaryOperator recursiveVariance() {
		return new RecursiveVariance(decayFactor);
	}
	
	private static final class ExponentialMovingAverage implements DoubleUnaryOperator {
		private final double decayFactor;
		private double ema = Double.NaN;
		
		ExponentialMovingAverage(double decayFactor) {
			this.decayFactor = decayFactor;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(ema)) {
				ema = value;
			}
			
			ema = ema + decayFactor * (value - ema);
			return ema;
		}
	};
	private DoubleUnaryOperator expornentialMovingAverage() {
		return new ExponentialMovingAverage(decayFactor);
	}
	
	public static double[] multiply(double a[], double b[]) {
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] * b[i];
		}
		return ret;
	}
	public static double[] divide(double a[], double b[]) {
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] / b[i];
		}
		return ret;
	}
	
	
	final double              decayFactor;
	final DoubleUnaryOperator recursiveVariance;
	
	public RiskMetrics(double decayFactor) {
		this.decayFactor       = decayFactor;
		this.recursiveVariance = new RecursiveVariance(decayFactor);
	}
	public RiskMetrics() {
		this(DEFAULT_DECAY_FACTOR);
	}
	
	
	public static double[] getRelativeReturn(double data[]) {
		double ret[] = new double[data.length - 1];
		for(int i = 1; i < data.length; i++) {
			final double d0 = data[i - 1];
			final double d1 = data[i];
			ret[i - 1] = ((d1 / d0) - 1) * 100;
		}
		return ret;
	}
		
	public static double[] getLogReturn(double data[]) {
		double ret[] = new double[data.length - 1];
		for(int i = 1; i < data.length; i++) {
			final double d0 = data[i - 1];
			final double d1 = data[i];
			ret[i - 1] = Math.log(d1 / d0) * 100;
		}
		return ret;
	}
	
	
	// EWMA variance, covariance and correlation
	private DoubleStream getCovarianceDoubleStream(double data1[], double data2[]) {
		return Arrays.stream(multiply(data1, data2)).map(recursiveVariance());
	}
	
	public double[] getCovariance(double data1[], double data2[]) {
		return getCovarianceDoubleStream(data1, data2).toArray();
	}
	public double[] getVariance(double data[]) {
		return getCovariance(data, data);
	}
	public double[] getStandardDeviation(double data[]) {
		return getCovarianceDoubleStream(data, data).map(applySqrt()).toArray();
	}
	public double[] getCorrelation(double data1[], double data2[]) {
		double sd1[] = getStandardDeviation(data1);
		double sd2[] = getStandardDeviation(data2);
		double cov[] = getCovariance(data1, data2);
		
		return divide(cov, multiply(sd1, sd2));
	}

	
	// EMA -- Exponential Moving Average
	public double[] getEMA(double data[]) {
		return Arrays.stream(data).map(expornentialMovingAverage()).toArray();
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
			
			double rr[] = getRelativeReturn(data);
			double lr[] = getLogReturn(data);
			
			logger.info("");
			for(int i = 0; i < rr.length; i++) {
				logger.info("{}", String.format("%8.3f  %8.3f  %8.3f", data[i + 1], rr[i], lr[i]));
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
				
				{
					RiskMetrics rm = new RiskMetrics();
					double lr[] = RiskMetrics.getLogReturn(data);
					double sd[] = rm.getStandardDeviation(lr);
					
					double lastData = data[data.length - 1];
					double lastSD   = sd[sd.length - 1];
					
					logger.info("");
					logger.info("{}", String.format("last  data %8.3f  sd %8.3f  %8.3f", lastData, lastSD, lastData * lastSD));
					
//					for(int i = 0; i < sd.length; i++) {
//						logger.info("{}", String.format("%8.3f  %8.3f", data[i], sd[i]));
//					}
				}
				
				{
					DescriptiveStatistics stats = new DescriptiveStatistics();
					Arrays.stream(data).forEach(o -> stats.addValue(o));
					
					double mean = stats.getMean();
					double sd   = stats.getStandardDeviation();
					
					logger.info("{}", String.format("n %4d  mean %8.3f  sd %8.3f", stats.getN(), mean, sd));
					logger.info("{}", String.format("%8.3f", mean - (CONFIDENCE_95_PERCENT * sd)));
				}

			} catch (SQLException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException();
			}
		}
	}
}
