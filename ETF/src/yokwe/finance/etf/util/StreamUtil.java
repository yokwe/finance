package yokwe.finance.etf.util;

import java.util.ArrayList;
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

// https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance

public class StreamUtil {
	static final Logger logger = LoggerFactory.getLogger(StreamUtil.class);
	
	private static class StatsAccumlator {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		int    n   = 0;
		double mean  = 0.0;
		double M2  = 0.0;
		double M3  = 0.0;
		double M4  = 0.0;
		
		void apply(double x) {
			final int n1 = n;
			n = n + 1;
			final double delta = x - mean;
			final double delta_n = delta / n;
			final double delta_n2 = delta_n * delta_n;
			final double term1 = delta * delta_n * n1;
			mean = mean + delta_n;
			M4 = M4 + term1 * delta_n2 * (n * n - 3 * n + 3) + 6 * delta_n2 * M2 - 4 * delta_n * M3;
			M3 = M3 + term1 * delta_n * (n - 2) - 3 * delta_n * M2;
			M2 = M2 + term1;
		}
		
		public String toString() {
			return String.format("[%d  %.3f]", n, mean);
		}
	}
	
	public static class Stats {
		public final double min;
		public final double max;
		public final int    count;
		public final double mean;
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
			this.mean         = a.mean;
			this.sdPopulation = Math.sqrt(a.M2 / (a.n + 1 - 1));
			this.sdSample     = Math.sqrt(a.M2 / (a.n + 1 - 2));
			
			final double skew = a.M3 / Math.pow(a.M2, 1.5);
			this.skewnessPopulation = Math.sqrt(a.n) * skew;
			this.skewnessSample = (a.n * Math.sqrt(a.n - 1) / (a.n - 2)) * skew;
			
			// TODO still kurtosis has wrong value
			final double kurtosis = (a.n * a.M4) / (a.M2 * a.M2);
			this.kurtosisPopulation = kurtosis - 3;
			this.kurtosisSample     = kurtosis * ((a.n + 1.0) * (a.n - 1.0)) / ((a.n - 2.0) * (a.n - 3.0)) - 3;
		}
		public String toString() {
			return String.format("[%d  %.3f  %.3f  %.3f  %.3f  %.3f  %.3f  %.3f]", count, min, mean, max, sdPopulation, sdSample, skewnessPopulation, kurtosisPopulation);
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
		    double mean = 12.40454545454550;
		    double var  = 10.00235930735930;
		    double skew =  1.437423729196190;
		    double kurt =  2.377191264804700;

			double[] values = {
					12.5, 12, 11.8, 14.2, 14.9, 14.5, 21, 8.2,
					10.3, 11.3, 14.1, 9.9, 12.2, 12, 12.1, 11,
					19.8, 11, 10, 8.8, 9, 12.3 };
			List<Double> valueList = new ArrayList<>();
			for(double value: values) valueList.add(value);
			Stats stats = valueList.stream().collect(toStats);
			
			logger.info("Data {}", valueList);

			logger.info("---- mean");
			logger.info("Expect = {}", String.format("%10.7f", mean));
			logger.info("Actual = {}", String.format("%10.7f", stats.mean));
			
			logger.info("---- standard deviation");
			logger.info("Expect = {}", String.format("%10.7f", Math.sqrt(var)));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.sdSample));
			logger.info("Actual = {}  population", String.format("%10.7f", stats.sdPopulation));

			logger.info("---- skewness");
			logger.info("Expect = {}", String.format("%10.7f", skew));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.skewnessSample));
			logger.info("Actual = {}  population", String.format("%10.7f", stats.skewnessPopulation));

			logger.info("---- kurtosis");
			logger.info("Expect = {}", String.format("%10.7f", kurt));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.kurtosisSample));
			logger.info("Actual = {}  population", String.format("%10.7f", stats.kurtosisPopulation));
		}
		

		logger.info("STOP");
	}
}
