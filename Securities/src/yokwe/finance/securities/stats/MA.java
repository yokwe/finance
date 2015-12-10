package yokwe.finance.securities.stats;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public abstract class MA implements DoubleUnaryOperator, DoubleConsumer {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MA.class);

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
	
	public static final class Simple extends MA {
		private final int    size;
		private final double data[];
		private double       sum;
		private int          pos;
		private int          count;

		public Simple(int dataSize) {
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

	public static final class Exp extends MA {
		private final int    size;
		private final double data[];
		// pos point to next position
		private int          pos;
		private int          count;

		public  final double alpha;
		private final double weight[];
		
		public Exp(double alpha) {
			this(getDataSize99(alpha), alpha);
		}

		public Exp(int dataSize, double alpha) {
			// Sanity check
			if (dataSize <= 0) {
				logger.info("dataSize = {}", dataSize);
				throw new SecuritiesException("invalid dataSize");
			}
			if (alpha <= 0.0 || 1.0 <= alpha) {
				logger.info("alpha = {}", String.format("%.2f", alpha));
				throw new SecuritiesException("invalid alpha");
			}

			size = dataSize;
			if (size <= 1) {
				logger.error("size = {}", size);
				throw new SecuritiesException("size <= 1");
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
					weight[size - i - 1] = w;
					w *= (1 - alpha);
				}
			}
		}
		
		private double getWeightedSum() {
			double ret = 0.0;
			
			int index = 0;
			// [pos .. size)
			for(int dIndex = pos; dIndex < size; dIndex++) {
				if (index == count) break;
				ret += data[dIndex] * weight[index];
//				logger.info("gws {}", String.format("%2d  %8.3f  %8.3f  %8.3f  %8.3f", dIndex, ret, data[dIndex], weight[index], data[dIndex] * weight[index]));
				index++;
			}
			// [0 .. pos)
			for(int dIndex = 0; dIndex < pos; dIndex++) {
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
			
			if (count < size) {
				data[pos++] = value;
				if (pos == size) pos = 0;
				count++;
			} else {
				data[pos++] = value;
				if (pos == size) pos = 0;
			}
		}
		
		@Override
		public double getValue() {
			if (pos == -1) return Double.NaN;
			return getWeightedSum();
		}
	}
	
	public static final class RExp extends MA {
		private final double alpha;
		private double       avg;
		
		public RExp(double alpha) {
			// Sanity check
			if (alpha <= 0.0 || 1.0 <= alpha) {
				logger.info("alpha = {}", String.format("%.2f", alpha));
				throw new SecuritiesException("invalid alpha");
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
}
