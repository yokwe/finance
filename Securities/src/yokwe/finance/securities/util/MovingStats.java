package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

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
	
	protected final int    size;
	protected final double data[];
	// pos point to next position
	protected int          pos;
	// count holds number of count of data
	protected boolean      firstTime;

	protected MovingStats(int dataSize) {
		size = dataSize;
		if (size <= 1) {
			logger.error("size = {}", size);
			throw new SecuritiesException("size <= 1");
		}
		data      = new double[size];
		pos       = 0;
		firstTime = true;
		Arrays.fill(data, 0.0);
	}
	
	@Override
	public void accept(double value) {
		if (firstTime) {
			for(int i = 0; i < size; i++) {
				data[pos++] = value;
			}
			pos       = 0;
			firstTime = false;
			return;
		}
		data[pos++] = value;
		if (pos == size) pos = 0;
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
			return MovingStats.getMean(data);
		}

		@Override
		public double getVariance() {
			final double mean = MovingStats.getMean(data);
			final int    size = data.length;
			double ret = 0;
			for(int i = 0; i < size; i++) {
				double t = mean - data[i];
				ret += (t * t);
			}
			return ret / size;
		}
	}
	
	public static final class Exponential extends MovingStats {
		private final double weight[];
		public final double weightRatio;
		
		public Exponential(int dataSize) {
			super(dataSize);
			double alfa = 2.0 / (dataSize + 1);
//			double alfa = 1.0 / dataSize;
			
			// From 0:high to size-1:low weight
			weight = new double[size];
			{
				double w  = alfa;
				double wr = 0;
				for(int i = 0; i < size; i++) {
					weight[size - i - 1] = w;
					wr += w;
					w *= (1 - alfa);
				}
				weightRatio = (1.0 / wr);
			}
		}
		
		private double[] getWeightedData() {
			double ret[] = new double[size];
			
			int index = 0;
			// [pos .. size)
			for(int dIndex = pos; dIndex < size; dIndex++) {
				ret[index] = data[dIndex] * weight[index];
				index++;
			}
			// [0 .. pos)
			for(int dIndex = 0; dIndex < pos; dIndex++) {
				ret[index] = data[dIndex] * weight[index];
				index++;
			}
			return ret;
		}
		
		public double getMean() {
			double ret = 0;
			int index = 0;
			for(int dIndex = pos; dIndex < size; dIndex++) {
				ret += data[dIndex] * weight[index++];
			}
			for(int dIndex = 0; dIndex < pos; dIndex++) {
				ret += data[dIndex] * weight[index++];
			}
			return ret;
		}

		@Override
		public double getVariance() {
			final double mean = MovingStats.getMean(data);
			
			double ret = 0;
			int index = 0;
			for(int dIndex = pos; dIndex < size; dIndex++) {
				double t = mean - data[dIndex];
				ret += (t * t) * weight[index++];
			}
			for(int dIndex = 0; dIndex < pos; dIndex++) {
				double t = mean - data[dIndex];
				ret += (t * t) * weight[index++];
			}
			return ret;
		}
	}
	
	public static final class EMA {
		protected static abstract class Base implements DoubleUnaryOperator {
			final protected MovingStats movingStats;
			public Base(int dataSize) {
				movingStats = new Exponential(dataSize);
			}
		}
		public static final class Mean extends Base {
			public Mean(int dataSize) {
				super(dataSize);
			}
			@Override
			public double applyAsDouble(double value) {
				movingStats.accept(value);
				return movingStats.getMean();
			}
		}
		public static final class StandardDeviation extends Base {
			public StandardDeviation(int dataSize) {
				super(dataSize);
			}
			@Override
			public double applyAsDouble(double value) {
				movingStats.accept(value);
				return movingStats.getStandardDeviation();
			}
		}
	}
	
	public static final class SMA {
		protected static abstract class Base implements DoubleUnaryOperator {
			final protected MovingStats movingStats;
			public Base(int dataSize) {
				movingStats = new Simple(dataSize);
			}
		}
		public static final class Mean extends Base {
			public Mean(int dataSize) {
				super(dataSize);
			}
			@Override
			public double applyAsDouble(double value) {
				movingStats.accept(value);
				return movingStats.getMean();
			}
		}
		public static final class StandardDeviation extends Base {
			public StandardDeviation(int dataSize) {
				super(dataSize);
			}
			@Override
			public double applyAsDouble(double value) {
				movingStats.accept(value);
				return movingStats.getStandardDeviation();
			}
		}
	}
	
	public static void main(String[] args) {
		{
			Exponential expo = new Exponential(10);
			for(int i = 0; i < expo.size; i++) {
				expo.accept(10 + i);
			}
			double wd[] = expo.getWeightedData();
			double wt = 0;
			double dt = 0;
			double wdt = 0;
			for(int i = 0; i < expo.size; i++) {
				wt += expo.weight[i];
				dt += expo.data[i];
				wdt += wd[i];
				logger.info("{}", String.format("%2d  %8.3f  %8.3f  %8.3f", i, expo.weight[i], expo.data[i], wd[i]));
			}
			logger.info("{}", String.format("%8.3f  %8.3f  %8.3f", wt, dt, wdt));
		}
		
		{
			Exponential expo = new Exponential(10);
			Simple      simp = new Simple(10);
			for(int i = 0; i < expo.size; i++) {
				expo.accept(100 + i);
				simp.accept(100 + i);
			}
			
			logger.info("{}", String.format("mean  s %8.3f  e %8.3f  w %8.3f", simp.getMean(), expo.getMean(), expo.getMean() * expo.weightRatio));
			logger.info("{}", String.format("var   s %8.3f  e %8.3f", simp.getVariance(),          expo.getVariance()));
			logger.info("{}", String.format("sd    s %8.3f  e %8.3f", simp.getStandardDeviation(), expo.getStandardDeviation()));
		}
		
	}
}
