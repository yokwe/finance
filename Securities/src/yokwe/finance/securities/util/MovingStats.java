package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public abstract class MovingStats implements DoubleConsumer {
	private static final Logger logger = LoggerFactory.getLogger(MovingStats.class);
	
	public static double getAlpha(int dataSize) {
		// From alpha = 2 / (N + 1)
		return 2.0 / (dataSize + 1.0);
	}
	public static int getDataSize(double alpha) {
		// From alpha = 2 / (N + 1)
		return (int)Math.round((2.0 / alpha) - 1.0);
	}
	// From k = log(0.01) / log (1 - alpha)
	public static int getDataSize99(double alpha) {
		return (int)Math.round(Math.log(0.01) / Math.log(1 - alpha));
	}


	protected static double getMean(double data[]) {
		final int size = data.length;
		double ret = 0;
		for(int i = 0; i < size; i++) {
			ret += data[i];
		}
		return ret / size;
	}
	
	public abstract double getMean();
	public abstract double getVariance();
	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	protected static abstract class DataArray extends MovingStats {
		protected final int    size;
		protected final double data[];
		// pos point to next position
		protected int          pos;
		// count holds number of count of data
		protected boolean      firstTime;

		protected DataArray(int dataSize) {
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
			// Ignore NaN value
			if (Double.isNaN(value)) return;
			
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
	}
	
	public static final class Simple extends DataArray {
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
	
	public static final class Exponential extends DataArray {		
		public  final double alpha;
		private final double weight[];
		public  final double weightRatio;
		
		public Exponential(int dataSize, double alpha) {
			super(dataSize);
			logger.info("{}", String.format("Exponential %3d  %8.3f", dataSize, alpha));
			
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
		
		public Exponential(int dataSize) {
			this(dataSize, getAlpha(dataSize));
		}
		public Exponential(double alpha) {
			this(getDataSize99(alpha), alpha);
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
	
	public static final class RecursiveExponential extends MovingStats {
		public final double alpha;
		private      double mean;
		private      double var;
		
		RecursiveExponential(int dataSize) {
			this(getAlpha(dataSize));
		}
		
		RecursiveExponential(double alpha) {
			this.alpha = alpha;
			this.mean  = Double.NaN;
			this.var   = Double.NaN;
			logger.info("{}", String.format("RecursiveExponential %8.3f", alpha));
		}
		
		@Override
		public void accept(double value) {
			if (Double.isNaN(value)) return;
			
			if (Double.isNaN(mean)) {
				mean = value;
				var  = 0;
			}
			
			mean = mean + alpha * (value           - mean);
			var  = var  + alpha * ((value * value) - var);
		}
		@Override
		public double getMean() {
			return mean;
		}
		@Override
		public double getVariance() {
			return var;
		}
	}
	
	
	protected static abstract class Base implements DoubleUnaryOperator {
		final protected MovingStats movingStats;
		public Base(MovingStats movingStats) {
			this.movingStats = movingStats;
		}
	}
	protected static class BaseMean extends Base {
		protected BaseMean(MovingStats movingStats) {
			super(movingStats);
		}
		@Override
		public double applyAsDouble(double value) {
			movingStats.accept(value);
			return movingStats.getMean();
		}
	}
	protected static class BaseSD extends Base {
		protected BaseSD(MovingStats movingStats) {
			super(movingStats);
		}
		@Override
		public double applyAsDouble(double value) {
			movingStats.accept(value);
			return movingStats.getStandardDeviation();
		}
	}
	protected static class BaseVAR extends Base {
		protected BaseVAR(MovingStats movingStats) {
			super(movingStats);
		}
		@Override
		public double applyAsDouble(double value) {
			movingStats.accept(value);
			return movingStats.getVariance();
		}
	}

	public static final class S {
		public static DoubleUnaryOperator mean(int dataSize) {
			return new BaseMean(new Simple(dataSize));
		}
		public static DoubleUnaryOperator sd(int dataSize) {
			return new BaseSD(new Simple(dataSize));
		}
		public static DoubleUnaryOperator var(int dataSize) {
			return new BaseVAR(new Simple(dataSize));
		}
	}
	public static final class E {
		public static DoubleUnaryOperator mean(int dataSize) {
			return new BaseMean(new Exponential(dataSize));
		}
		public static DoubleUnaryOperator mean(double alpha) {
			return new BaseMean(new Exponential(alpha));
		}
		public static DoubleUnaryOperator sd(int dataSize) {
			return new BaseSD(new Exponential(dataSize));
		}
		public static DoubleUnaryOperator sd(double alpha) {
			return new BaseSD(new Exponential(alpha));
		}
		public static DoubleUnaryOperator var(int dataSize) {
			return new BaseVAR(new Exponential(dataSize));
		}
		public static DoubleUnaryOperator var(double alpha) {
			return new BaseVAR(new Exponential(alpha));
		}
	}
	public static final class RE {
		public static DoubleUnaryOperator mean(int dataSize) {
			return new BaseMean(new RecursiveExponential(dataSize));
		}
		public static DoubleUnaryOperator mean(double alpha) {
			return new BaseMean(new RecursiveExponential(alpha));
		}
		public static DoubleUnaryOperator sd(int dataSize) {
			return new BaseSD(new RecursiveExponential(dataSize));
		}
		public static DoubleUnaryOperator sd(double alpha) {
			return new BaseSD(new RecursiveExponential(alpha));
		}
		public static DoubleUnaryOperator var(int dataSize) {
			return new BaseVAR(new RecursiveExponential(dataSize));
		}
		public static DoubleUnaryOperator var(double alpha) {
			return new BaseVAR(new RecursiveExponential(alpha));
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
		
		{
			Simple      simpl = new Simple(33);
			Exponential expoA = new Exponential(33);
			Exponential expoB = new Exponential(0.059);
			Exponential expoC = new Exponential(Exponential.getDataSize99(0.059), 0.059);
			RecursiveExponential rexpo = new RecursiveExponential(33);
			
			for(int i = 0; i < 1000; i++) {
				simpl.accept(100 + i);
				expoA.accept(100 + i);
				expoB.accept(100 + i);
				expoC.accept(100 + i);
				rexpo.accept(100 + i);
			}
			
			logger.info("");
			logger.info("{}", String.format("size  S %8d  A %8d  B %8d  C %8d",             simpl.size,                   expoA.size,                   expoB.size,                   expoC.size));
			logger.info("{}", String.format("alpha S           A %8.3f  B %8.3f  C %8.3f",  expoA.alpha,                  expoB.alpha,                  expoC.alpha));
			logger.info("{}", String.format("wf    S           A %8.3f  B %8.3f  C %8.3f",  expoA.weightRatio,            expoB.weightRatio,            expoC.weightRatio));
			logger.info("{}", String.format("mean  S %8.3f  A %8.3f  B %8.3f  C %8.3f  R %8.3f",     simpl.getMean(),              expoA.getMean(),              expoB.getMean(),              expoC.getMean(),              rexpo.getMean()));
			logger.info("{}", String.format("var   S %8.3f  A %8.3f  B %8.3f  C %8.3f  R %8.3f",     simpl.getVariance(),          expoA.getVariance(),          expoB.getVariance(),          expoC.getVariance(),          rexpo.getVariance()));
			logger.info("{}", String.format("sd    S %8.3f  A %8.3f  B %8.3f  C %8.3f  R %8.3f",     simpl.getStandardDeviation(), expoA.getStandardDeviation(), expoB.getStandardDeviation(), expoC.getStandardDeviation(), rexpo.getStandardDeviation()));
		}
		
	}
}
