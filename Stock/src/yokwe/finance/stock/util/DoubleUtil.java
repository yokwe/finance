package yokwe.finance.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;

public final class DoubleUtil {
	private static final Logger logger = LoggerFactory.getLogger(DoubleUtil.class);
	
	public static final double ALMOST_ZERO = 0.000001;
	public static boolean isAlmostZero(double value) {
		return -ALMOST_ZERO < value && value < ALMOST_ZERO;
	}
	public static boolean isAlmostEqual(double a, double b) {
		return isAlmostZero(a - b);
	}
	
	public static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    return bd.setScale(places, RoundingMode.HALF_UP).doubleValue();
	}
	
	public static double round(String value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    BigDecimal bd = new BigDecimal(value);
	    return bd.setScale(places, RoundingMode.HALF_UP).doubleValue();
	}
	
	public static double roundPrice(double value) {
		return round(String.format("%.4f", value), 2);
	}
	public static double roundQuantity(double value) {
		return round(String.format("%.7f", value), 5);
	}

	

	public static final double CONFIDENCE_95_PERCENT = 1.65;
	public static final double CONFIDENCE_99_PERCENT = 2.33;

	public enum Confidence {
		PERCENT_95(CONFIDENCE_95_PERCENT),
		PERCENT_99(CONFIDENCE_99_PERCENT),
		DEFAULT(CONFIDENCE_95_PERCENT);
		
		private double value;
		double getValue() {
			return value;
		}
		Confidence(double value) {
			this.value = value;
		}
	}
	
	public static final double DEFAULT_DECAY_FACTOR = 0.94;
	public static final double DEFAULT_ALPHA        = 1.0 - DEFAULT_DECAY_FACTOR;

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
			throw new UnexpectedException("a.length != b.length");
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
			throw new UnexpectedException("a.length != b.length");
		}
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] / b[i];
		}
		return ret;
	}
	
	// ema covariance
	public static double[] ema_covariance(double alpha, double data1[], double data2[]) {
		return Arrays.stream(multiply(data1, data2)).map(ema(alpha)).toArray();
	}
	// ema correlation
	public static double[] ema_correlation(double alpha, double data1[], double data2[]) {
		double sd1[] = Arrays.stream(data1).map(ema_sd(alpha)).toArray();
		double sd2[] = Arrays.stream(data2).map(ema_sd(alpha)).toArray();
		double cov[] = ema_covariance(alpha, data1, data2);
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
	
	// multiply
	private static final class multiply implements DoubleUnaryOperator {
		private final double factor;
		
		private multiply(double factor) {
			this.factor = factor;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			return value * factor;
		}
	};
	public static DoubleUnaryOperator multiply(double factor) {
		return new multiply(factor);
	}
	
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
			final double ret = (value / lastValue) - 1.0;
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
			final double ret = Math.log(value / lastValue);
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
	public static final class EMA_NR implements DoubleUnaryOperator, DoubleConsumer {
		private final int    size;
		private final double data[];
		// pos point to next position
		private int          pos;
		
		public  final double alpha;
		private final double weight[];
		public  final double weightRatio;

		public EMA_NR(int dataSize, double alpha) {
			// Sanity check
			if (dataSize <= 0) {
				logger.info("dataSize = {}", dataSize);
				throw new UnexpectedException("invalid dataSize");
			}
			if (alpha <= 0.0 || 1.0 <= alpha) {
				logger.info("alpha = {}", String.format("%.2f", alpha));
				throw new UnexpectedException("invalid alpha");
			}

			size = dataSize;
			if (size <= 1) {
				logger.error("size = {}", size);
				throw new UnexpectedException("size <= 1");
			}
			data      = new double[size];
			pos       = -1;
			Arrays.fill(data, 0.0);
			
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
//				logger.info("gws {}", String.format("%2d  %8.3f  %8.3f  %8.3f  %8.3f", dIndex, ret, data[dIndex], weight[index], data[dIndex] * weight[index]));
				index++;
			}
			// [0 .. pos)
			for(int dIndex = 0; dIndex < pos; dIndex++) {
				ret += data[dIndex] * weight[index];
//				logger.info("gws {}", String.format("%2d  %8.3f  %8.3f  %8.3f  %8.3f", dIndex, ret, data[dIndex], weight[index], data[dIndex] * weight[index]));
				index++;
			}
			return ret;
		}

		@Override
		public double applyAsDouble(double value) {
			// Ignore NaN value
			if (Double.isNaN(value)) return Double.NaN;
			
			accept(value);
			return getWeightedSum();
		}
		@Override
		public void accept(double value) {
			// Ignore NaN value
			if (Double.isNaN(value)) return;
			
			if (pos == -1) {
				for(int i = 0; i < size; i++) {
					data[i] = value;
				}
				pos = 0;
				return;
			}
			data[pos++] = value;
			if (pos == size) pos = 0;
		}
		public double getValue() {
			return getWeightedSum();
		}
	}
	public static EMA_NR ema_nr(double alpha) {
		return new EMA_NR(getDataSize99(alpha), alpha);
	}
	public static EMA_NR semaFromDecayFactor(double decayFactor) {
		return ema_nr(getAlphaFromDecayFactor(decayFactor));
	}
	public static EMA_NR ema_nr(int dataSize) {
		return new EMA_NR(dataSize, getAlpha(dataSize));
	}
	public static EMA_NR ema_nr(int dataSize, double alpha) {
		return new EMA_NR(dataSize, alpha);
	}

	
	// Recursive Exponential Moving Average
	public static final class EMA implements DoubleUnaryOperator, DoubleConsumer {
		private final double alpha;
		private double       avg;
		
		private EMA(int dataSize) {
			this(getAlpha(dataSize));
		}
		
		private EMA(double alpha) {
			// Sanity check
			if (alpha <= 0.0 || 1.0 <= alpha) {
				logger.info("alpha = {}", String.format("%.2f", alpha));
				throw new UnexpectedException("invalid alpha");
			}

			this.alpha = alpha;
			this.avg   = Double.NaN;
		}
		
		@Override
		public double applyAsDouble(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return Double.NaN;
			
			accept(value);
			return avg;
		}

		@Override
		public void accept(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return;
			
			if (Double.isNaN(avg)) {
				avg = value;
			}
			avg = avg + alpha * (value - avg);
		}
		
		public double getValue() {
			return avg;
		}
	};
	public static EMA ema(double alpha) {
		return new EMA(alpha);
	}
	public static EMA emaFromDecayFactor(double decayFactor) {
		return new EMA(getAlphaFromDecayFactor(decayFactor));
	}
	public static EMA ema(int dataSize) {
		return new EMA(getAlpha(dataSize));
	}
	
	// ema_var
	private static final class EMA_Variance implements DoubleUnaryOperator {
		private final DoubleUnaryOperator ema;
		private EMA_Variance(double alpha) {
			ema = ema(alpha);
		}
		@Override
		public double applyAsDouble(double value) {
			return ema.applyAsDouble(value * value);
		}
	}
	public static DoubleUnaryOperator ema_variance(double alpha) {
		return new EMA_Variance(alpha);
	}
	// ema_sd
	private static final class EMA_StandardDeviation implements DoubleUnaryOperator {
		private final DoubleUnaryOperator var;
		private EMA_StandardDeviation(double alpha) {
			var = new EMA_Variance(alpha);
		}
		@Override
		public double applyAsDouble(double value) {
			return Math.sqrt(var.applyAsDouble(value));
		}
	}
	public static DoubleUnaryOperator ema_sd(double alpha) {
		return new EMA_StandardDeviation(alpha);
	}
	
	// TODO we should define methods that return standard deviation of log return;
	//   also provide more than one version of HistoricalVolatility or ValueAtRisk

	// valueAtRisk
	private static class ValueAtRisk implements DoubleUnaryOperator {
		private final EMA ema;
		private final double factor;
		
		private ValueAtRisk(double alpha, Confidence confidence) {
			ema    = new EMA(alpha);
			factor = confidence.getValue();
		}
		// applyAsDouble will takes log return (not squared log return)
		@Override
		public double applyAsDouble(double value) {
			return Math.sqrt(ema.applyAsDouble(value * value)) * factor;
		}
	}
	public static DoubleUnaryOperator valueAtRisk(double alpha, Confidence confidence) {
		return new ValueAtRisk(alpha, confidence);
	}
	
	// Simple Moving Average
	public static final class SMA implements DoubleUnaryOperator, DoubleConsumer {
		private final int    size;
		private final double data[];
		private double       sum;
		private int          pos;
		private int          count;
		
		private SMA(int dataSize) {
			size  = dataSize;
			data  = new double[size];
			sum   = Double.NaN;
			pos   = 0;
			count = 0;
			
			Arrays.fill(data, 0.0);
		}
		
		@Override
		public double applyAsDouble(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return sum;
			
			accept(value);
			return sum / count;
		}

		@Override
		public void accept(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return;
			
			if (count < size) {
				if (count == 0) sum = 0.0;
				data[pos++] = value;
				if (pos == size) pos = 0;
				sum += value;
				count++;
			} else {
				sum += value - data[pos];
				data[pos++] = value;
				if (pos == size) pos = 0;
			}
		}
		
		public double getValue() {
			if (count == 0) return Double.NaN;
			return sum / count;
		}
	};
	public static SMA sma(int dataSize) {
		return new SMA(dataSize);
	}
	// sma_var
	private static final class SMA_Variance implements DoubleUnaryOperator {
		private final DoubleUnaryOperator sma;
		private SMA_Variance(int dataSize) {
			sma = sma(dataSize);
		}
		@Override
		public double applyAsDouble(double value) {
			return sma.applyAsDouble(value * value);
		}
	}
	public static DoubleUnaryOperator sma_variance(int dataSize) {
		return new SMA_Variance(dataSize);
	}
	// ema_sd
	private static final class SMA_StandardDeviation implements DoubleUnaryOperator {
		private final DoubleUnaryOperator var;
		private SMA_StandardDeviation(int dataSize) {
			var = new SMA_Variance(dataSize);
		}
		@Override
		public double applyAsDouble(double value) {
			return Math.sqrt(var.applyAsDouble(value));
		}
	}
	public static DoubleUnaryOperator sma_sd(int dataSize) {
		return new SMA_StandardDeviation(dataSize);
	}
	

	// simpleStats
	public enum StatsType {
		MIN, MAX, SUM, MEAN, VAR, SD, KURT, SKEW,
	}
	public static final class SimpleStats implements DoubleUnaryOperator, DoubleConsumer {
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
			return getValue();
		}
		@Override
		public void accept(double value) {
			stats.accept(value);
		}
		public double getValue() {
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
				throw new UnexpectedException("Unknown type");
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
	
	// TODO Write the code that calculate mean using reduce
	// TODO Write the code that calculate standard deviation using reduce 
	
	//
	// test code
	//
	private static void testTable41() {
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
			logger.info("Table 4.1 {}", String.format("%8.3f  %8.3f  %8.3f", data[i + 1], rr[i] * 100, lr[i] * 100));
		}
	}
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
		{
//			EMA_NR ema = ema_nr(b.length, alpha);
			EMA_NR ema = ema_nr(alpha);
			Arrays.stream(b).forEach(ema);
			// To calculate standard deviation apply sqrt
			logger.info("Table 5.2    sema   {}", String.format("%8.3f  %8.3f", Math.sqrt(ema.getValue()), ema.getValue()));
		}
		// TODO There is significant difference between EMA_NR and EMA.
		{
			EMA ema = ema(alpha);
			Arrays.stream(b).forEach(ema);
			// To calculate standard deviation apply sqrt
			logger.info("Table 5.2    ema    {}", String.format("%8.3f  %8.3f", Math.sqrt(ema.getValue()), ema.getValue()));
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
		double var_s[] = Arrays.stream(multiply(data, data)).map(ema_nr(alpha)).toArray();
		
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

		final double alpha = getAlphaFromDecayFactor(DEFAULT_DECAY_FACTOR);
		double rva[] = Arrays.stream(data_a).map(ema_variance(alpha)).toArray();
		double rvb[] = Arrays.stream(data_b).map(ema_variance(alpha)).toArray();
		double cov[] = ema_covariance(alpha, data_a, data_b);
		double cor[] = ema_correlation(alpha, data_a, data_b);
		
		logger.info("");
		for(int i = 0; i < data_a.length; i++) {
			logger.info("Table 5.5 {}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", data_a[i], data_b[i], rva[i], rvb[i], cov[i], cor[i]));
		}
	}
	public static void main(String args[]) {
		testTable41();
		testTable52();
		testTable53();
		testTable55();
	}
}
