package yokwe.finance.securities.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;

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
			boolean firstTime = true;
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
	}
}
