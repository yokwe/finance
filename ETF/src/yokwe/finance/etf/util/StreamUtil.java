package yokwe.finance.etf.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;


// Formulas for Robust, One-Pass Parallel
// Computation of Covariances and
// Arbitrary-Order Statistical Moments
//   http://prod.sandia.gov/techlib/access-control.cgi/2008/086212.pdf

public class StreamUtil {
	static final Logger logger = LoggerFactory.getLogger(StreamUtil.class);
	
	private static class StatsAccumlator {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		int    n   = 0;
		double M1  = 0.0;
		double M2  = 0.0;
		double M3  = 0.0;
		double M4  = 0.0;
		
		void apply(double y) {
			n++;
			
			if (y < min) min = y;
			if (max < y) max = y;
			
			double delta = y - M1;
			double delta_n = delta / n;
			double delta_n2 = delta_n * delta_n;
			double term1 = delta * delta_n * (n - 1);
			
			M1 += delta_n;
			M4 += term1 * delta_n2 * (n * n - 3 * n + 3) + (6 * delta_n2 * M2) - (4 * delta_n * M3);
			M3 += term1 * delta_n * (n - 2) - 3 * delta_n * M2;
			M2 += term1;
		}
		
		public String toString() {
			return String.format("[%d  %.3f]", n, M1);
		}
	}
	
	public static class Stats {
		public final double min;
		public final double max;
		public final int    count;
		public final double avg;
		public final double sdPopulation;
		public final double sdSample;
		public final double skewnessPopulation;
		public final double skewnessSample;
		public final double kurtosisPopulation;
		public final double kurtosisSample;
		
		// http://www.real-statistics.com/descriptive-statistics/symmetry-skewness-kurtosis/
		// http://www.johndcook.com/blog/skewness_kurtosis/
		// http://www.johndcook.com/blog/running_regression/
		private Stats(StatsAccumlator a) {
			this.min          = a.min;
			this.max          = a.max;
			this.count        = a.n;
			this.avg          = a.M1;
			this.sdPopulation = Math.sqrt(a.M2 / (a.n + 1 - 1));
			this.sdSample     = Math.sqrt(a.M2 / (a.n + 1 - 2));
			
			final double skew = a.M3 / Math.pow(a.M2, 1.5);
			this.skewnessPopulation = Math.sqrt(a.n) * skew;
			this.skewnessSample = (a.n * Math.sqrt(a.n - 1) / (a.n - 2)) * skew;
			
			// TODO still kurtosis has wrong value
			final double kurtosis = a.M4 / (a.M2 * a.M2);
			this.kurtosisPopulation = (a.n * kurtosis) - 3;
			this.kurtosisSample     = ((a.n * (a.n + 1) * (a.n - 1)) / ((a.n - 2) * (a.n - 3)) * kurtosis) - 3;
		}
		public String toString() {
			return String.format("[%d  %.3f  %.3f  %.3f  %.3f  %.3f  %.3f  %.3f]", count, min, avg, max, sdPopulation, sdSample, skewnessPopulation, kurtosisPopulation);
		}
	}
	
	public static final Collector<Double, StatsAccumlator, Stats> toStats;
	static {
		Supplier<StatsAccumlator> supplier = () -> new StatsAccumlator();
		BiConsumer<StatsAccumlator, Double> accumulator = (a, e) -> a.apply(e);
		BinaryOperator<StatsAccumlator> combiner = (a1, a2) -> {
			logger.error("combiner  {}  {}", a1.toString(), a2.toString());
			throw new ETFException("Not expected");
		};
		Function<StatsAccumlator, Stats> finisher = (a) -> new Stats(a);
		toStats = Collector.of(supplier, accumulator, combiner, finisher, Characteristics.CONCURRENT);
	}
	
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			Double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
			List<Double> valueList = Arrays.asList(values);
			Stats stats = valueList.stream().collect(toStats);
			logger.info("Expect stats = [5  1.000  3.000  5.000  1.414  1.581]");
			logger.info("Actual stats = {}", stats);
		}
		
		{
			Double[] values = {70.0, 70.0, 70.0, 70.0, 85.0};
			List<Double> valueList = Arrays.asList(values);
			Stats stats = valueList.stream().collect(toStats);
			logger.info("Actual avg = {}  skewness = {}  kurtosis = {}",
					stats.avg, stats.sdPopulation, stats.skewnessPopulation, stats.kurtosisPopulation);
		}
		
		{
			Double[] values = {9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 20.0};
			List<Double> valueList = Arrays.asList(values);
			Stats stats = valueList.stream().collect(toStats);
			logger.info("--  9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 9.0, 20.0");
			logger.info("Expect count = 11  avg = 10.00  sd =   3.32  skewness =  3.32  kurtosis = 11.10");
			logger.info("{}", String.format("Actual count = %2d  avg = %5.2f  sd = %6.2f  skewness = %5.2f  kurtosis = %5.2f",
					stats.count, stats.avg, stats.sdSample, stats.skewnessSample, stats.kurtosisSample));
		}
		{
			Double[] values = {5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0};
			List<Double> valueList = Arrays.asList(values);
			Stats stats = valueList.stream().collect(toStats);
			logger.info("-- 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0");
			logger.info("Expect count = 11  avg = 10.00  sd =   3.32  skewness =  0.00  kurtosis = -1.20");
			logger.info("{}", String.format("Actual count = %2d  avg = %5.2f  sd = %6.2f  skewness = %5.2f  kurtosis = %5.2f",
					stats.count, stats.avg, stats.sdSample, stats.skewnessSample, stats.kurtosisSample));
		}
		
		
		logger.info("STOP");
	}

}
