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
	
	public static final class SMA extends MA {
		private final int    size;
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

	public static final class EMA extends MA {
		private final int    size;
		private final double data[];
		// pos point to next position
		private int          pos;
		private int          count;

		public  final double alpha;
		private final double weight[];
		
		public EMA(double alpha) {
			this(getDataSize(alpha), alpha);
		}

		public EMA(int dataSize, double alpha) {
			logger.info("{}", String.format("Exp %3d  %4.2f", dataSize, alpha));
			
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
			logger.info("{}", String.format("RExp %4.2f", alpha));
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


	private static void testSimple() {
//		double data[] = {1.0, 2.0, 3.0, 4.0, 5.0};
		double data[] = {1.0, 1.0, 1.0, 1.0, 1.0};
		{
			MA ma = new SMA(2);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("simple 2 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
		{
			MA ma = new SMA(5);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("simple 5 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
	}
	private static void testExp() {
		double data[] = {1.0, 2.0, 3.0, 4.0, 5.0};
		{
			MA ma = new EMA(0.1);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("Exp   2 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
		{
			MA ma = new EMA(0.8);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("Exp   5 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
	}
	private static void testRExp() {
//		double data[] = {1.0, 2.0, 3.0, 4.0, 5.0};
		double data[] = {1.0, 1.0, 1.0, 1.0, 1.0};
		{
			MA ma = new RExp(0.1);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("RExp  2 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
		{
			MA ma = new RExp(0.8);
			for(int i = 0; i < data.length; i++) {
				double result = ma.applyAsDouble(data[i]);
				logger.info("RExp  5 {}", String.format("%4.2f  %4.2f", data[i], result));
			}
		}
	}
	public static void main(String[] args) {
		testSimple();
		testExp();
		testRExp();
	}
}
