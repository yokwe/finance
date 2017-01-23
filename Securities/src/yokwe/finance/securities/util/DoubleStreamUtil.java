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

import yokwe.finance.securities.SecuritiesException;

public class DoubleStreamUtil {
	static final Logger logger = LoggerFactory.getLogger(DoubleStreamUtil.class);
	
	public static final class Stats implements DoubleConsumer {
		final DescriptiveStatistics stats = new DescriptiveStatistics();

		@Override
		public void accept(double value) {
			stats.addValue(value);
		}
		
		public Stats() {}
		
		public Stats(double[] values) {
			this();
			Arrays.stream(values).forEach(this);
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
		public double getVariance() {
			return stats.getVariance();
		}
		public double getPopulatedVariance() {
			return stats.getPopulationVariance();
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

	public static final class SimpleMovingStats implements DoubleUnaryOperator {
		public enum Type {
			MIN, MAX, SUM, VARIANCE, MEAN, STANDARD_DEVIATION, KURTOSIS, SKEWNESS,
		}
		final Type                  type;
		final DescriptiveStatistics stats;
		
		private SimpleMovingStats(Type type, int interval) {
			this.type  = type;
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
			switch(type) {
			case MIN:
				return stats.getMin();
			case MAX:
				return stats.getMax();
			case SUM:
				return stats.getSum();
			case VARIANCE:
				return stats.getVariance();
			case MEAN:
				return stats.getMean();
			case STANDARD_DEVIATION:
				return stats.getStandardDeviation();
			case KURTOSIS:
				return stats.getKurtosis();
			case SKEWNESS:
				return stats.getSkewness();
			default:
				logger.error("Unknown type = {}", type);
				throw new SecuritiesException("Unknown type");
			}
		}

		public static SimpleMovingStats getInstance(Type type, int interval) {
			return new SimpleMovingStats(type, interval);
		}
	}

	public static final class MovingSD {
		public static SimpleMovingStats getInstance(int interval) {
			return new SimpleMovingStats(SimpleMovingStats.Type.STANDARD_DEVIATION, interval);
		}
	}

	public static final class MovingAverage {
		public static SimpleMovingStats getInstance(int interval) {
			return new SimpleMovingStats(SimpleMovingStats.Type.MEAN, interval);
		}
	}

	public static final class RelativeRatio implements DoubleUnaryOperator {
		private double lastValue = Double.NaN;
		
		private RelativeRatio() {
		}

		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return 1;
			} else {
				double ret = value / lastValue;
				lastValue = value;
				return ret;
			}
		}

		public static RelativeRatio getInstance() {
			return new RelativeRatio();
		}
	}
	
	// One day Historical Volatility
	public static class HistoricalVolatility implements DoubleUnaryOperator {
		private final DoubleUnaryOperator rr;
		private final DoubleUnaryOperator sd;
		private double lastValue = Double.NaN;
		
		private HistoricalVolatility(int interval) {
			rr = RelativeRatio.getInstance();
			sd = SimpleMovingStats.getInstance(SimpleMovingStats.Type.STANDARD_DEVIATION, interval);
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return 0;
			}
			double ret = rr.applyAsDouble(value);
			ret = Math.log(ret);
			ret = sd.applyAsDouble(ret);
			return ret;
		}
		
		// Usually interval is 21 (monthly)
		public static DoubleUnaryOperator getInstance(int interval) {
			return new HistoricalVolatility(interval);
		}
	}

	public static class ValueAtRisk implements DoubleUnaryOperator {
		private final double C_RELIABILITY = 2.33;              // For 99% reliability
//		private final double C_RELIABILITY = 2.00;              // For 95% reliability
		
		private final DoubleUnaryOperator hv;
		
		private ValueAtRisk(int interval) {
			hv = HistoricalVolatility.getInstance(interval);
		}
		@Override
		public double applyAsDouble(double value) {
			value = hv.applyAsDouble(value);
			return value * C_RELIABILITY;
		}
		
		public static DoubleUnaryOperator getInstance(int interval) {
			return new ValueAtRisk(interval);
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
	
	private static void testRelativeRatio() {
		double[] values = {
				1, 1, 1,   2, 2, 2,
				3, 3, 3,   4, 4, 4,
				5, 5, 5,   6, 6, 6,
		};
		
		double[] rr = Arrays.stream(values).map(RelativeRatio.getInstance()).toArray();
		
		logger.info("values {}", Arrays.stream(values).boxed().collect(Collectors.toList()));
		logger.info("rr     {}", Arrays.stream(rr).boxed().collect(Collectors.toList()));
	}
	
	private static void testSD() {
		// Standard Deviation of below is  2.9277
		double[] values = {11, 13, 12, 16, 18, 19, 14, 17,};
		
		double[] sd = Arrays.stream(values).map(SimpleMovingStats.getInstance(SimpleMovingStats.Type.STANDARD_DEVIATION, values.length)).toArray();
		
		logger.info("values {}", Arrays.stream(values).boxed().collect(Collectors.toList()));
		logger.info("sd     {}", sd[sd.length - 1]);
	}
	
	// MovingHistoricalVoratility
	//   double[] hb = Arrays.stream(values).map(RelativeRatio.getInstance()).map(o -> Math.log(o)).map(MovingSD.getInstance(20)).toArray();
	
	public static void main(String[] args) {
		testMovingStats();
		testStats();
		testSampling();
		testMovingAverage();
		testRelativeRatio();
		testSD();
	}
}
