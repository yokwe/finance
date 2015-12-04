package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public abstract class MovingStats implements DoubleConsumer {
	private static final Logger logger = LoggerFactory.getLogger(MovingStats.class);

	private static double getMean(double data[]) {
		final int size = data.length;
		double ret = 0;
		for(int i = 0; i < size; i++) {
			ret += data[i];
		}
		return ret / size;
	}
	private static double getVariance(double data[]) {
		final int size = data.length;
		final double mean = getMean(data);
		double ret = 0;
		for(int i = 0; i < size; i++) {
			double t = mean - data[i];
			ret += t * t;
		}
		return ret / size;
	}
	
	protected final int    size;
	protected final double data[];
	// pos point to next position
	protected int          pos;
	// count holds number of count of data
	protected int          count;

	protected MovingStats(int dataSize) {
		size = dataSize;
		if (size <= 1) {
			logger.error("size = {}", size);
			throw new SecuritiesException("size <= 1");
		}
		data  = new double[size];
		pos   = 0;
		count = 0;
		Arrays.fill(data, 0.0);
	}
	
	@Override
	public void accept(double value) {
		data[pos++] = value;
		if (pos == size) pos = 0;
		if (count < size) count++;
	}
	
	public abstract double getMean();
	public abstract double getVariance();
	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public static final class Simple extends MovingStats {
		public Simple(int dataSize) {
			super(dataSize);
		}
		
		@Override
		public double getMean() {
			return getMean(Arrays.copyOf(data, count));
		}

		@Override
		public double getVariance() {
			return getVariance(Arrays.copyOf(data, count));
		}
	}
	
	public static final class Exponential extends MovingStats {
		final double weight[];
		
		public Exponential(int dataSize, double decayFactor) {
			super(dataSize);
			weight = new double[size];
			pos  = 0;
			{
				double t = 1.0;
				for(int i = 0; i < size; i++) {
					weight[size - i - 1] = (1 - decayFactor) * t;
					t *= decayFactor;
				}
			}
		}
		
		private double[] getWeightedData() {
			double ret[] = new double[count];
			
			int wIndex = size;
			int rIndex = 0;
			
			// pos - 1 => 0
			for(int dIndex = pos - 1; 0 <= dIndex; dIndex--) {
				if (rIndex == count) break; // No more data to process
				ret[rIndex++] = data[dIndex] * weight[wIndex--];
			}
			// size - 1 => pos
			for(int dIndex = size - 1; pos <= dIndex; dIndex--) {
				if (rIndex == count) break; // No more data to process
				ret[rIndex++] = data[dIndex] * weight[wIndex--];
			}
			return ret;
		}
		
		public double getMean() {
			return getMean(getWeightedData());
		}

		@Override
		public double getVariance() {
			return getVariance(getWeightedData());
		}
	}
	
	public static void main(String[] args) {
		Exponential expo = new Exponential(20, 0.94);
		for(int i = 0; i < 20; i++) {
			logger.info("{}", String.format("%2d  %8.3f", i, expo.weight[i]));
		}
	}
}
