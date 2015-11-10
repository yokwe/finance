package yokwe.finance.securities.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleStreamUtil {
	static final Logger logger = LoggerFactory.getLogger(DoubleStreamUtil.class);
	
	public static final class Stats implements DoubleConsumer {
		final DescriptiveStatistics stats = new DescriptiveStatistics();

		@Override
		public void accept(double value) {
			stats.addValue(value);
		}
		
		public double getMin() {
			return stats.getMin();
		}
		public double getMax() {
			return stats.getMax();
		}
		public double getMean() {
			return stats.getMean();
		}
		public double getStandardDeviation() {
			return stats.getStandardDeviation();
		}
		public double getKurtosis() {
			return stats.getKurtosis();
		}
		public double getSkewnewss() {
			return stats.getSkewness();
		}
	}
	
	public static final class MovingStats {
		public static class MapToObj implements DoubleFunction<MovingStats> {
			final DescriptiveStatistics stats;
			public MapToObj(int interval) {
				stats = new DescriptiveStatistics(interval);
			}
			
			public void clear() {
				stats.clear();
			}

			@Override
			public MovingStats apply(double value) {
				if (stats.getN() == 0) {
					for(int i = 1; i < stats.getWindowSize(); i++) {
						stats.addValue(value);
					}
				}
				stats.addValue(value);
				return new MovingStats(stats.getMean(), stats.getStandardDeviation(), stats.getKurtosis(), stats.getSkewness());
			}
		}
		public static MapToObj mapToObj(int interval) {
			return new MapToObj(interval);
		}

		public final double mean;
		public final double standardDeviation;
		public final double kurtosis;
		public final double skewness;
		
		private MovingStats(double mean, double standardDeviation, double kurtosis, double skewness) {
			this.mean = mean;
			this.standardDeviation = standardDeviation;
			this.kurtosis = kurtosis;
			this.skewness = skewness;
		}
	}

	public static final class Sample implements DoubleFunction<DoubleStream> {
		private int interval;
		private int count;
		private Sample(int interval) {
			this.interval = interval;
			this.count    = 0;
		}

		@Override
		public DoubleStream apply(double value) {
			count++;
			if (count == interval) {
				count = 0;
				return Arrays.stream(new double[]{value});
			} else {
				return null;
			}
		}
		public static Sample getInstance(int interval) {
			return new Sample(interval);
		}
	}
	
	private static void testSampling() {
		double[] values = {
				1, 2, 3,  4,  5,  6,
				7, 8, 9, 10, 11, 12,
		};
		
		double[] smaple3 = Arrays.stream(values).flatMap(Sample.getInstance(3)).toArray();
		double[] smaple6 = Arrays.stream(values).flatMap(Sample.getInstance(6)).toArray();
		
		logger.info("values   {}", Arrays.stream(values).boxed().collect(Collectors.toList()));
		logger.info("sample 3 {}", Arrays.stream(smaple3).boxed().collect(Collectors.toList()));
		logger.info("sample 6 {}", Arrays.stream(smaple6).boxed().collect(Collectors.toList()));
	}

	public static final class MovingAverage implements DoubleUnaryOperator {
		final DescriptiveStatistics stats;
		
		private MovingAverage(int interval) {
			this.stats = new DescriptiveStatistics(interval);
		}

		@Override
		public double applyAsDouble(double value) {
			if (stats.getN() == 0) {
				for(int i = 1; i < stats.getWindowSize(); i++) {
					stats.addValue(value);
				}
			}
			stats.addValue(value);
			return stats.getMean();
		}

		public static MovingAverage getInstance(int interval) {
			return new MovingAverage(interval);
		}
	}

	
	private static void testMovingStats() {
		double[] values = {
				1, 1, 1,   2, 2, 2,
				3, 3, 3,   4, 4, 4,
				5, 5, 5,   6, 6, 6,
		};
		
		logger.info("values {}", Arrays.stream(values).boxed().collect(Collectors.toList()));
		{
			double[] result = Arrays.stream(values).mapToObj(MovingStats.mapToObj(3)).mapToDouble(o -> o.mean).toArray();
			List<Double> listList = new ArrayList<>();
			for(int i = 0; i < values.length; i++) {
				if (((i + 1) % 3) == 0) listList.add(result[i]);
			}
			logger.info("stats 3 {}", listList);
		}
		{
			double[] result = Arrays.stream(values).mapToObj(MovingStats.mapToObj(6)).mapToDouble(o -> o.mean).toArray();
			List<Double> listList = new ArrayList<>();
			for(int i = 0; i < values.length; i++) {
				if (((i + 1) % 6) == 0) listList.add(result[i]);
			}
			logger.info("stats 6 {}", listList);
		}
	}
	
	private static void testMovingAverage() {
		double[] values = {
				1, 1, 1,   2, 2, 2,
				3, 3, 3,   4, 4, 4,
				5, 5, 5,   6, 6, 6,
		};
		
		double[] ma3 = Arrays.stream(values).map(MovingAverage.getInstance(3)).toArray();
		double[] ma6 = Arrays.stream(values).map(MovingAverage.getInstance(6)).toArray();
		
		logger.info("values {}", Arrays.stream(values).boxed().collect(Collectors.toList()));
		logger.info("ma 3   {}", Arrays.stream(ma3).boxed().collect(Collectors.toList()));
		logger.info("ma 6   {}", Arrays.stream(ma6).boxed().collect(Collectors.toList()));
	}
	
	private static void testStats() {
		{
			double[] values = {
					1, 1, 1,   2, 2, 2,
			};
			Stats stats = new Stats();
			Arrays.stream(values).forEach(stats);
			logger.info("stats mean {}", stats.getMean());
		}
		{
			double[] values = {
					1, 1, 1,   2, 2, 2,
					3, 3, 3,   4, 4, 4,
					5, 5, 5,   6, 6, 6,
			};
			Stats stats = new Stats();
			Arrays.stream(values).forEach(stats);
			logger.info("stats mean {}", stats.getMean());
		}

	}
	
	public static void main(String[] args) {
		testMovingStats();
		testStats();
		testSampling();
		testMovingAverage();
	}
}
