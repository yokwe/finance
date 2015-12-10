package yokwe.finance.securities.stats;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

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
	public static void square(double a[]) {
		for(int i = 0; i < a.length; i++) {
			a[i] *= a[i];
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
		final MA.SMA sma = MA.sma(dataSize);
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
		double ret[] = var_sma(lr, dataSize);
		sqrt(ret);
		return ret;
	}
	
	public static double[] ema(double data[], double alpha) {
		MA.EMA ema = MA.ema(alpha);
		double ret[] = new double[data.length];
		for(int i = 0; i < data.length; i++) {
			ret[i] = ema.applyAsDouble(data[i]);
		}
		return ret;
	}

	public static double[] var_ema(double lr[], double alpha) {
		return ema(multiply(lr, lr), alpha);
	}
	
	public static double[] sd_ema(double lr[], double alpha) {
		double ret[] = var_ema(lr, alpha);
		sqrt(ret);
		return ret;
	}

	public static double[] cov_ema(double data1[], double data2[], double alpha) {
		return ema(multiply(data1, data2), alpha);
	}
	
	public static double[] cor_ema(double data1[], double data2[], double alpha) {
		double sd1[] = sd_ema(data1, alpha);
		double sd2[] = sd_ema(data2, alpha);
		double cov[] = cov_ema(data1, data2, alpha);
		return divide(cov, multiply(sd1, sd2));
	}

	
	private static void testTable53() {
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

		double alpha = MA.getAlphaFromDecayFactor(MA.DEFAULT_DECAY_FACTOR);
		double var_r[] = var_ema(data, alpha);
		
		logger.info("");
		for(int i = 0; i < data.length; i++) {
			logger.info("Table 5.3 {}", String.format("%8.3f  %8.3f", data[i], var_r[i]));
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

		double alpha = MA.getAlphaFromDecayFactor(MA.DEFAULT_DECAY_FACTOR);
		double rva[] = var_ema(data_a, alpha);
		double rvb[] = var_ema(data_b, alpha);
		double cov[] = cov_ema(data_a, data_b, alpha);
		double cor[] = cor_ema(data_a, data_b, alpha);
		
		logger.info("");
		for(int i = 0; i < data_a.length; i++) {
			logger.info("Table 5.5 {}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", data_a[i], data_b[i], rva[i], rvb[i], cov[i], cor[i]));
		}
	}

	public static void main(String[] args) {
		testTable53();
		testTable55();
	}
}
