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
	
	public static final double DEFAULT_DECAY_FACTOR = 0.94;

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
	public static double[] multiply(double a[], double b) {
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] * b;
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
	// Exponential Moving Average
	public static final class SEMA implements DoubleUnaryOperator {
		private final int    size;
		private final double data[];
		// pos point to next position
		private int          pos;
		
		public  final double alpha;
		private final double weight[];
		public  final double weightRatio;

		public SEMA(int dataSize) {
			this(dataSize, getAlpha(dataSize));
		}
		public SEMA(double alpha) {
			this(getDataSize99(alpha), alpha);
		}
		public SEMA(int dataSize, double alpha) {
			size = dataSize;
			if (size <= 1) {
				logger.error("size = {}", size);
				throw new SecuritiesException("size <= 1");
			}
			data      = new double[size];
			Arrays.fill(data, 0.0);
			pos       = -1;
			
			this.alpha = alpha;
			// From 0:high to size-1:low weight
			weight = new double[size];
			{
				double w  = alpha;
				double wr = 0;
				for(int i = 0; i < size; i++) {
					weight[size - i - 1] = w;
					wr += w;
					w *= (1 - alpha);
				}
				weightRatio = (1.0 / wr);
			}

		}
		
		private double getWeightedSum() {
			double ret = 0.0;
			
			int index = 0;
			// [pos .. size)
			for(int dIndex = pos; dIndex < size; dIndex++) {
				ret += data[dIndex] * weight[index];
				index++;
			}
			// [0 .. pos)
			for(int dIndex = 0; dIndex < pos; dIndex++) {
				ret += data[dIndex] * weight[index];
				index++;
			}
			return ret;
		}

		@Override
		public double applyAsDouble(double value) {
			// Ignore NaN value
			if (Double.isNaN(value)) return Double.NaN;
			
			if (pos == -1) {
				for(int i = 0; i < size; i++) {
					data[i] = value;
				}
				pos = 0;
				return getWeightedSum();
			}
			data[pos++] = value;
			if (pos == size) pos = 0;
			return getWeightedSum();
		}
	}
	public static DoubleUnaryOperator sema(double alpha) {
		return new SEMA(alpha);
	}
	public static DoubleUnaryOperator semaFromDecayFactor(double decayFactor) {
		return new SEMA(getAlphaFromDecayFactor(decayFactor));
	}
	public static DoubleUnaryOperator sema(int dataSize) {
		return new SEMA(getAlpha(dataSize));
	}

	
	// Recursive Exponential Moving Average
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
	
	
	//
	// test code
	//
	private static void testTable52() {
		double[] a = {
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

		logger.info("");
		double alpha = getAlphaFromDecayFactor(DEFAULT_DECAY_FACTOR);
		double c[] = new double[a.length];
		Arrays.fill(c, 1.0 / a.length);
		double d[] = new double[a.length];
		{
			double decayFactor = DEFAULT_DECAY_FACTOR;
			double t = 1.0 - decayFactor;
			for(int i = 0; i < a.length; i++) {
				d[d.length - i - 1] = t;
				t *= decayFactor;
			}
		}
		double b[] = multiply(a, a);
		double e[] = multiply(b, c);
		double f[] = multiply(b, d);
		for(int i = 0; i < a.length; i++) {
			logger.info("Table 5.2 {}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", a[i], b[i], c[i], d[i], e[i], f[i]));
		}
		logger.info("Table 5.2");
		{
			Stats stats = new Stats();
			Arrays.stream(e).forEach(stats);
			// To calculate standard deviation apply sqrt
			logger.info("Table 5.2    equal  {}", String.format("%8.3f  %8.3f", Math.sqrt(stats.getSum()), stats.getSum()));
		}
		{
			Stats stats = new Stats();
			Arrays.stream(f).forEach(stats);
			// To calculate standard deviation apply sqrt
			logger.info("Table 5.2    sema   {}", String.format("%8.3f  %8.3f", Math.sqrt(stats.getSum()), stats.getSum()));
		}
		// TODO not working as expected - value should be near 0.333
		{
			double result[] = Arrays.stream(b).map(ema(alpha)).toArray();
			// To calculate standard deviation apply sqrt
			logger.info("Table 5.2    sema   {}", String.format("%8.3f", result[result.length - 1]));
		}
		
	}
	private static void testTable53() {
		double alpha = getAlphaFromDecayFactor(DEFAULT_DECAY_FACTOR);
		double[] data = {
			 0.633,
			 0.115,
			-0.459,
			 0.093,
			 0.176,
			-0.087,
			-0.142,
			 0.324,
			-0.943,
			-0.528,
			-0.107,
			-0.159,
			-0.445,
		 	 0.053,
			 0.152,
			-0.318,
			 0.424,
			-0.708,
			-0.105,
			-0.257,
			};

		double var_r[] = Arrays.stream(multiply(data, data)).map(ema(alpha)).toArray();
		double var_s[] = Arrays.stream(multiply(data, data)).map(sema(alpha)).toArray();
		
		logger.info("");
		for(int i = 0; i < var_s.length; i++) {
			logger.info("Table 5.3 {}", String.format("%8.3f  %8.3f  %8.3f", data[i], var_r[i], var_s[i]));
		}
	}
	private static void testTable55() {
		// Calculation of variance, covariance and correlation
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

		final double alpha = DoubleUtil.getAlphaFromDecayFactor(DEFAULT_DECAY_FACTOR);
		double rva[] = DoubleUtil.getVariance(alpha, data_a);
		double rvb[] = DoubleUtil.getVariance(alpha, data_b);
		double cov[] = DoubleUtil.getCovariance(alpha, data_a, data_b);
		double cor[] = DoubleUtil.getCorrelation(alpha, data_a, data_b);
		
		logger.info("");
		for(int i = 0; i < data_a.length; i++) {
			logger.info("Table 5.5 {}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", data_a[i], data_b[i], rva[i], rvb[i], cov[i], cor[i]));
		}
	}
	public static void main(String args[]) {
		testTable52();
		testTable53();
		testTable55();
	}
}
