package yokwe.finance.etf.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;

public class StreamUtil {
	static final Logger logger = LoggerFactory.getLogger(StreamUtil.class);
	
	public static class Stats {
		private static class Accumlator {
			SummaryStatistics summary = new SummaryStatistics();
			Skewness          skew    = new Skewness();
			Kurtosis          kurt    = new Kurtosis();
			
			void apply(double x) {
				summary.addValue(x);
				skew.increment(x);
				kurt.increment(x);
			}
		}
		
		public static Collector<Double, Accumlator, Stats> getInstance() {
			Supplier<Accumlator> supplier = () -> new Accumlator();
			BiConsumer<Accumlator, Double> accumulator = (a, e) -> a.apply(e);
			BinaryOperator<Accumlator> combiner = (a1, a2) -> {
				logger.error("combiner  {}  {}", a1.toString(), a2.toString());
				throw new ETFException("Not expected");
			};
			Function<Accumlator, Stats> finisher = (a) -> new Stats(a);
			return Collector.of(supplier, accumulator, combiner, finisher);
		}
		
		public final int    n;
		public final double min;
		public final double max;
		public final double mean;
		
		public final double stdv;                // sample
		public final double skewness;            // sample
		public final double kurtosis;            // sample
		
		public final double population_stdv;     // population
		public final double population_skewness; // population
		public final double population_kurtosis; // population  *NOT VERIFIED*
		
		private Stats(Accumlator a) {
			this.n        = (int)a.summary.getN();
			this.min      = a.summary.getMin();
			this.max      = a.summary.getMax();
			this.mean     = a.summary.getMean();
			
			this.stdv     = a.summary.getStandardDeviation();
			this.skewness = a.skew.getResult();
			this.kurtosis = a.kurt.getResult();
			
			// convert sample to population
			this.population_stdv     = stdv     * Math.sqrt((double)(n - 1) / (double)(n));
			this.population_skewness = skewness * ((double)n - 2) / Math.sqrt(n * (n - 1));
			this.population_kurtosis = kurtosis * (double)((n - 2) * (n - 3)) / (double)((n + 1) * (n - 1));
		}
		public String toString() {
			return String.format("[%3d  %.3f  %.3f  %.3f  %.3f  %.3f  %.3f]", n, min, mean, max, stdv, skewness, kurtosis);
		}
	}
	
	public static class Sampling {
		private static final class Accumlator {
			final int    interval;
			Mean         mean   = new Mean();
			List<Double> result = new ArrayList<>();
			
			Accumlator(int interval) {
				this.interval = interval;
			}
			
			void apply(double x) {
				mean.increment(x);
				if (mean.getN() == interval) {
					result.add(mean.getResult());
					mean.clear();
				}
			}
			List<Double> finish() {
				if (mean.getN() != 0) {
					result.add(mean.getResult());
					mean.clear();
				}
				return result;
			}
		}
		
		public static Collector<Double, Accumlator, List<Double>> getInstance(int interval) {
			Supplier<Accumlator>               supplier    = () -> new Accumlator(interval);
			BiConsumer<Accumlator, Double>     accumulator = (a, e) -> a.apply(e);
			BinaryOperator<Accumlator> combiner = (a1, a2) -> {
				logger.error("combiner  {}  {}", a1.toString(), a2.toString());
				throw new ETFException("Not expected");
			};
			Function<Accumlator, List<Double>> finisher    = (a) -> a.finish();
			return Collector.of(supplier, accumulator, combiner, finisher);
		}
	}

	public static class MovingAverage {
		private static final class Accumlator {
			final int                   interval;
			final DescriptiveStatistics stats;
			List<Double> result = new ArrayList<>();
			
			Accumlator(int interval) {
				this.interval = interval;
				this.stats    = new DescriptiveStatistics(interval);
			}
			
			void apply(double x) {
				stats.addValue(x);
				if (interval <= stats.getN()) {
					result.add(stats.getMean());
				}
			}
			List<Double> finish() {
				return result;
			}
		}
		
		public static Collector<Double, Accumlator, List<Double>> getInstance(int interval) {
			Supplier<Accumlator>               supplier    = () -> new Accumlator(interval);
			BiConsumer<Accumlator, Double>     accumulator = (a, e) -> a.apply(e);
			BinaryOperator<Accumlator> combiner = (a1, a2) -> {
				logger.error("combiner  {}  {}", a1.toString(), a2.toString());
				throw new ETFException("Not expected");
			};
			Function<Accumlator, List<Double>> finisher    = (a) -> a.finish();
			return Collector.of(supplier, accumulator, combiner, finisher);
		}
	}

	private static void testStats() {
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
			Stats stats = valueList.stream().collect(Stats.getInstance());
			
			logger.info("---- mean");
			logger.info("Data {}", valueList);
			logger.info("Expect = {}", String.format("%10.7f", mean));
			logger.info("Actual = {}", String.format("%10.7f", stats.mean));
			
			logger.info("---- standard deviation sample");
			logger.info("Data {}", valueList);
			logger.info("Expect = {}", String.format("%10.7f", Math.sqrt(var)));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.stdv));

			logger.info("---- skewness sample");
			logger.info("Data {}", valueList);
			logger.info("Expect = {}", String.format("%10.7f", skew));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.skewness));

			logger.info("---- kurtosis sample");
			logger.info("Data {}", valueList);
			logger.info("Expect = {}", String.format("%10.7f", kurt));
			logger.info("Actual = {}  sample", String.format("%10.7f", stats.kurtosis));
		}
		
		{
		    double skew =  0.07925;

			double[] values = {1, 2, 3, 4, 5, 6, 8, 8};
			List<Double> valueList = new ArrayList<>();
			for(double value: values) valueList.add(value);
			Stats stats = valueList.stream().collect(Stats.getInstance());
			
			logger.info("---- skewness population");
			logger.info("Data {}", valueList);

			logger.info("Expect = {}", String.format("%10.7f", skew));
			logger.info("Actual = {}  populatin", String.format("%10.7f", stats.population_skewness));
		}
	}
	private static void testSampling() {
		double[] values = {
				1, 1, 1,   2, 2, 2,
				3, 3, 3,   4, 4, 4,
				5, 5, 5,   6, 6, 6,
		};
		List<Double> valueList = new ArrayList<>();
		for(double value: values) valueList.add(value);
		
		logger.info("valueList = {}", valueList);
		logger.info("sampling  3 = {}", valueList.stream().collect(Sampling.getInstance(3)));
		logger.info("sampling  6 = {}", valueList.stream().collect(Sampling.getInstance(6)));
		logger.info("sampling 12 = {}", valueList.stream().collect(Sampling.getInstance(12)));

	}
	private static void testMovingAverage() {
		double[] values = {
				1, 1, 1,   2, 2, 2,
				3, 3, 3,   4, 4, 4,
				5, 5, 5,   6, 6, 6,
		};
		List<Double> valueList = new ArrayList<>();
		for(double value: values) valueList.add(value);
		logger.info("valueList = {}", valueList);
		logger.info("sampling  3 = {}", valueList.stream().collect(MovingAverage.getInstance(3)));
	}
	public static void main(String[] args) {
		logger.info("START");
		
		testStats();
		testSampling();
		testMovingAverage();
	
		logger.info("STOP");
	}
}
