package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class DoubleUtil {
	private static final Logger logger = LoggerFactory.getLogger(DoubleUtil.class);

	//
	// Utility Methods for array of double
	//
	public static double mean(double data[]) {
		final int size = data.length;
		double ret = 0;
		for(int i = 0; i < size; i++) {
			ret += data[i];
		}
		return ret / size;
	}
	public static double[] multiply(double a[], double b[]) {
		if (a.length != b.length) {
			logger.error("a.length = {}  b.length = {}", a.length, b.length);
			throw new SecuritiesException("a.length != b.length");
		}
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] * b[i];
		}
		return ret;
	}
	public static double[] divide(double a[], double b[]) {
		if (a.length != b.length) {
			logger.error("a.length = {}  b.length = {}", a.length, b.length);
			throw new SecuritiesException("a.length != b.length");
		}
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] / b[i];
		}
		return ret;
	}
	
	// EWMA variance, covariance and correlation
	public static DoubleStream getCovarianceDoubleStream(double alpha, double data1[], double data2[]) {
		return Arrays.stream(multiply(data1, data2)).map(ema(alpha));
	}
	public static double[] getCovariance(double alpha, double data1[], double data2[]) {
		return getCovarianceDoubleStream(alpha, data1, data2).toArray();
	}
	public static double[] getVariance(double alpha, double data[]) {
		return getCovariance(alpha, data, data);
	}
	public static double[] getStandardDeviation(double alpha, double data[]) {
		return getCovarianceDoubleStream(alpha, data, data).map(sqrt()).toArray();
	}
	public static double[] getCorrelation(double alpha, double data1[], double data2[]) {
		double sd1[] = getStandardDeviation(alpha, data1);
		double sd2[] = getStandardDeviation(alpha, data2);
		double cov[] = getCovariance(alpha, data1, data2);
		
		return divide(cov, multiply(sd1, sd2));
	}



	//
	// DoublePredictor
	//
	private static final DoublePredicate skipNaN = new DoublePredicate() {
		public boolean test(double value) {
			return !Double.isNaN(value);
		}
	};
	public static DoublePredicate skipNan() {
		return skipNaN;
	}
	
	
	//
	// DoubleUnaryOperator
	//
	
	// square
	private static final DoubleUnaryOperator squareInstance = new DoubleUnaryOperator() {
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			return value * value;
		}
	};
	public static DoubleUnaryOperator square() {
		return squareInstance;
	}

	// sqrt
	private static final DoubleUnaryOperator sqrtInstance = new DoubleUnaryOperator() {
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			return Math.sqrt(value);
		}
	};
	public static DoubleUnaryOperator sqrt() {
		return sqrtInstance;
	}
	

	// relativeReturn
	private static final class RelativeReturn implements DoubleUnaryOperator {
		private double lastValue;
		public RelativeReturn() {
			lastValue = Double.NaN;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return Double.NaN;
			}
			final double ret = ((value / lastValue) - 1.0) * 100.0;
			lastValue = value;
			return ret;
		}
	}
	public static DoubleUnaryOperator relativeReturn() {
		return new RelativeReturn();
	}
	public static DoubleStream relativeReturn(double[] data) {
		return Arrays.stream(data).map(relativeReturn()).filter(skipNan());
	}
	
	// logReturn
	private static final class LogReturn implements DoubleUnaryOperator {
		private double lastValue;
		public LogReturn() {
			lastValue = Double.NaN;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return Double.NaN;
			}
			final double ret = Math.log(value / lastValue) * 100.0;
			lastValue = value;
			return ret;
		}
	}
	public static DoubleUnaryOperator logReturn() {
		return new LogReturn();
	}
	public static DoubleStream logReturn(double[] data) {
		return Arrays.stream(data).map(logReturn()).filter(skipNan());
	}

	// EMA - exponential moving average
	// dataSize 32 => decayFactor 93.94
	public static double getAlpha(int dataSize) {
		// From alpha = 2 / (N + 1)
		return 2.0 / (dataSize + 1.0);
	}
	public static double getAlphaFromDecayFactor(double decayFactor) {
		return 1.0 - decayFactor;
	}
	public static int getDataSize(double alpha) {
		// From alpha = 2 / (N + 1)
		return (int)Math.round((2.0 / alpha) - 1.0);
	}
	// From k = log(0.01) / log (1 - alpha)
	public static int getDataSize99(double alpha) {
		return (int)Math.round(Math.log(0.01) / Math.log(1 - alpha));
	}
	private static final class EMA implements DoubleUnaryOperator {
		private final double alpha;
		private double       var;
		
		// dataSize 32 => decayFactor 93.94
		private EMA(int dataSize) {
			this(getAlpha(dataSize));
		}
		
		private EMA(double alpha) {
			this.alpha = alpha;
			this.var   = Double.NaN;
		}
		
		@Override
		public double applyAsDouble(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return Double.NaN;
			
			if (Double.isNaN(var)) {
				var = value;
			}
			var = var + alpha * (value - var);
			return var;
		}
	};
	public static DoubleUnaryOperator ema(double alpha) {
		return new EMA(alpha);
	}
	public static DoubleUnaryOperator emaFromDecayFactor(double decayFactor) {
		return new EMA(getAlphaFromDecayFactor(decayFactor));
	}
	public static DoubleUnaryOperator ema(int dataSize) {
		return new EMA(getAlpha(dataSize));
	}
	
	// simpleStats
	public enum StatsType {
		MIN, MAX, SUM, MEAN, VAR, SD, KURT, SKEW,
	}
	private static final class SimpleStats implements DoubleUnaryOperator {
		private final StatsType type;
		private final Stats     stats;
		
		private SimpleStats(StatsType type) {
			this.type = type;
			this.stats = new Stats();
		}
		private SimpleStats(StatsType type, int dataSize) {
			this.type = type;
			this.stats = new Stats(dataSize);
		}
		
		@Override
		public double applyAsDouble(double value) {
			stats.accept(value);
			
			switch(type) {
			case MIN:
				return stats.getMin();
			case MAX:
				return stats.getMax();
			case SUM:
				return stats.getSum();
			case MEAN:
				return stats.getMean();
			case VAR:
				return stats.getVariance();
			case SD:
				return stats.getStandardDeviation();
			case KURT:
				return stats.getKurtosis();
			case SKEW:
				return stats.getSkewness();
			default:
				logger.error("Unknown type = {}", type);
				throw new SecuritiesException("Unknown type");
			}
		}
	}
	public static DoubleUnaryOperator simpleStats(StatsType type) {
		return new SimpleStats(type);
	}
	public static DoubleUnaryOperator simpleStats(StatsType type, int dataSize) {
		return new SimpleStats(type, dataSize);
	}

	
	
	//
	// DoubleFunction
	//
	
	// sample
	private static final class Sample implements DoubleFunction<DoubleStream> {
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
	}
	public static DoubleFunction<DoubleStream> sample(int interval) {
		return new Sample(interval);
	}
	
	
	//
	// DoubleConsumer
	//
	
	// stats
	public static final class Stats implements DoubleConsumer {
		final DescriptiveStatistics stats;

		public Stats() {
			stats = new DescriptiveStatistics();
		}
		public Stats(int dataSize) {
			stats = new DescriptiveStatistics(dataSize);
		}
		
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
		public double getSum() {
			return stats.getSum();
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
		public double getKurtosis() {
			return stats.getKurtosis();
		}
		public double getSkewness() {
			return stats.getSkewness();
		}
	}
	public DoubleConsumer stats() {
		return new Stats();
	}
	public DoubleConsumer stats(int dataSize) {
		return new Stats(dataSize);
	}
}
