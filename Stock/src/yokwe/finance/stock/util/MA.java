package yokwe.finance.stock.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;

public abstract class MA implements DoubleUnaryOperator, DoubleConsumer {
	private static final Logger logger = LoggerFactory.getLogger(MA.class);
	
	public static final double DEFAULT_DECAY_FACTOR = 0.94;
	public static final double DEFAULT_ALPHA        = getAlphaFromDecayFactor(DEFAULT_DECAY_FACTOR);

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

	public abstract double getValue();
	
	@Override
	public double applyAsDouble(double value) {
		// Ignore NaN value
		if (Double.isNaN(value)) return Double.NaN;
		
		accept(value);
		return getValue();
	}
	
	public static final class SMA extends MA {
		public  final int    size;
		private final double data[];
		private double       sum;
		private int          pos;
		private int          count;

		public SMA(int dataSize) {
			size  = dataSize;
			data  = new double[size];
			sum   = Double.NaN;
			pos   = 0;
			count = 0;
			
			Arrays.fill(data, 0.0);
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
		
		@Override
		public double getValue() {
			if (count == 0) return Double.NaN;
			return sum / count;
		}
	}
	public static SMA sma(int dataSize) {
		return new SMA(dataSize);
	}
	public static SMA sma(int dataSize, double[] values) {
		SMA sma = new SMA(dataSize);
		Arrays.stream(values).forEach(sma);
		return sma;
	}

	public static final class EMA_NR extends MA {
		public  final int    size;
		private final double data[];
		// pos point to next position
		private int          pos;
		private int          count;

		public  final double alpha;
		private final double weight[];
		
		public EMA_NR(double alpha) {
			this(getDataSize99(alpha), alpha);
		}

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
			data  = new double[size];
			pos   = 0;
			count = 0;
			Arrays.fill(data, 0.0);
			
			this.alpha = alpha;
			// From 0:high to size-1:low weight
			weight = new double[size];
			{
				double w  = alpha;
				for(int i = 0; i < size; i++) {
					weight[i] = w;
					w *= (1 - alpha);
				}
//				for(int i = 0; i < size; i++) {
//					logger.info("{}", String.format("weight %3d  %6.4f", i, weight[i]));
//				}
			}
		}
		
		private double getWeightedSum() {
			double ret = 0.0;
			
			int index = 0;
			// pos - 1 -> 0
			for(int dIndex = pos - 1; 0 <= dIndex; dIndex--) {
				if (index == count) break;
				ret += data[dIndex] * weight[index];
//				logger.info("gws {}", String.format("%2d  %8.3f  %8.3f  %8.3f  %8.3f", dIndex, ret, data[dIndex], weight[index], data[dIndex] * weight[index]));
				index++;
			}
			// pos - 1 -> pos
			for(int dIndex = size - 1; pos <= dIndex; dIndex--) {
				if (index == count) break;
				ret += data[dIndex] * weight[index];
//				logger.info("gws {}", String.format("%2d  %8.3f  %8.3f  %8.3f  %8.3f", dIndex, ret, data[dIndex], weight[index], data[dIndex] * weight[index]));
				index++;
			}
			return ret;
		}

		@Override
		public void accept(double value) {
			// Ignore NaN value
			if (Double.isNaN(value)) return;
			
			if (count == 0) {
				for(int i = 0; i < size; i++) {
					data[i] = value;
				}
				count = size;
				pos = 0;
				return;
			}
			data[pos++] = value;
			if (pos == size) pos = 0;
		}
		
		@Override
		public double getValue() {
			if (pos == -1) return Double.NaN;
			return getWeightedSum();
		}
	}
	public static EMA_NR ema_nr(double alpha) {
		return new EMA_NR(alpha);
	}
	public static EMA_NR ema_nr(int dataSize, double alpha) {
		return new EMA_NR(dataSize, alpha);
	}

	
	public static final class EMA extends MA {
		public  final double alpha;
		private       double avg;
		
		public EMA(double alpha) {
			// Sanity check
			if (alpha <= 0.0 || 1.0 <= alpha) {
				logger.info("alpha = {}", String.format("%.2f", alpha));
				throw new UnexpectedException("invalid alpha");
			}

			this.alpha = alpha;
			this.avg   = Double.NaN;
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
		
		@Override
		public double getValue() {
			return avg;
		}
	}
	public static EMA ema(double alpha) {
		return new EMA(alpha);
	}
	public static EMA ema() {
		return new EMA(DEFAULT_ALPHA);
	}


	private static void testSimple() {
		double data[] = {1.0, 2.0, 3.0, 4.0, 5.0};
		{
			logger.info("");
			MA ma = sma(2);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("SMA 2 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
		{
			logger.info("");
			MA ma = sma(5);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("SMA 5 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
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

		double var_r[] = Arrays.stream(DoubleArray.multiply(data, data)).map(ema(alpha)).toArray();
		double var_s[] = Arrays.stream(DoubleArray.multiply(data, data)).map(ema_nr(alpha)).toArray();
		
		logger.info("");
		for(int i = 0; i < var_s.length; i++) {
			logger.info("Table 5.3 {}", String.format("%8.3f  %8.3f  %8.3f", data[i], var_r[i], var_s[i]));
		}
	}
	public static void main(String[] args) {
		testSimple();
		testTable53();
	}
}
