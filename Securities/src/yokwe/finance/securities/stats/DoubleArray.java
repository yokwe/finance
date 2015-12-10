package yokwe.finance.securities.stats;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

public final class DoubleArray {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DoubleArray.class);

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
	
	public static void multiply(double a[], double b) {
		for(int i = 0; i < a.length; i++) {
			a[i] *= b;
		}
	}
	public static void sqrt(double a[]) {
		for(int i = 0; i < a.length; i++) {
			a[i] = Math.sqrt(a[i]);
		}
	}
	
	public static double mean(double[] data) {
		if (data.length == 0) return Double.NaN;
		double ret = 0;
		for(double value: data) ret += value;
		return ret / data.length;
	}
	public static double variance(double[] data, double mean) {
		if (data.length == 0) return Double.NaN;
		double ret = 0;
		for(double value: data) {
			double t = mean - value;
			ret += t * t;
		}
		return ret / data.length;
	}
	public static double variance(double[] data) {
		return variance(data, mean(data));
	}
	public static double standardDeviation(double[] data) {
		return standardDeviation(data);
	}
	public static double standardDeviation(double[] data, double mean) {
		if (data.length == 0) return Double.NaN;
		return Math.sqrt(variance(data, mean));
	}
	
	public static double[] var_sma(double lr[], final int dataSize) {
		return var_sma(lr, MA.sma(dataSize));
	}
	public static double[] var_sma(double lr[], MA.SMA sma) {
		final int dataSize = sma.size;
		int pos = 0;
		double save[] = new double[dataSize];
		double sum = lr[0] * dataSize;
		Arrays.fill(save, lr[0]);
		
		double ret[] = new double[lr.length];
		for(int i = 0; i < lr.length; i++) {
			double data = lr[i];
			
			sum += data - save[pos];
			save[pos++] = data;
			if (pos == dataSize) pos = 0;
			
			double sd = variance(save, sum / dataSize);

			ret[i] = sma.applyAsDouble(sd);
		}
		return ret;
	}
	public static double[] sd_sma(double lr[], final int dataSize) {
		return sd_sma(lr, MA.sma(dataSize));
	}
	public static double[] sd_sma(double lr[], MA.SMA sma) {
		double ret[] = var_sma(lr, sma);
		sqrt(ret);
		return ret;
	}
	
	public static double[] var_ema(double lr[], double alpha) {
		return var_ema(lr, MA.ema(alpha));
	}
	public static double[] var_ema(double lr[], MA.EMA ema) {
		double ret[] = new double[lr.length];
		for(int i = 0; i < lr.length; i++) {
			double data = lr[i];
			ret[i] = ema.applyAsDouble(data * data);
		}
		return ret;
	}
	
	public static double[] sd_ema(double lr[], double alpha) {
		return sd_ema(lr, MA.ema(alpha));
	}
	public static double[] sd_ema(double lr[], MA.EMA ema) {
		double ret[] = var_ema(lr, ema);
		sqrt(ret);
		return ret;
	}

	
	private static void testTable53() {
		double alpha = MA.getAlphaFromDecayFactor(DoubleUtil.DEFAULT_DECAY_FACTOR);
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

		double var_r[] = var_ema(data, alpha);
		
		logger.info("");
		for(int i = 0; i < data.length; i++) {
			logger.info("Table 5.3 {}", String.format("%8.3f  %8.3f", data[i], var_r[i]));
		}
	}

	public static void main(String[] args) {
		testTable53();
	}
}
