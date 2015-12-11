package yokwe.finance.securities.stats;

import java.util.Arrays;

import org.apache.commons.math3.stat.correlation.Covariance;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
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
	
	public static double cov(double[] data1, double data2[]) {
		if (data1.length != data2.length) {
			logger.error("data1.length = {}  data2.length = {}", data1.length, data2.length);
			throw new SecuritiesException("data1.length != data2.length");
		}
		final int size = data1.length;
		
		double mean1 = mean(data1);
		double mean2 = mean(data2);
		double ret = 0;
		for(int i = 0; i < size; i++) {
			double diff1 = data1[i] - mean1;
			double diff2 = data2[i] - mean2;
			ret += diff1 * diff2;
		}
		return ret / size;
	}
	
	public static class UniStats {
		final int    size;
		final double mean;
		final double variance;
		final double sd;
		final double diff[];
		
		public UniStats(int size, double mean, double variance, double diff[]) {
			this.size     = size;
			this.mean     = mean;
			this.variance = variance;
			this.sd       = Math.sqrt(variance);
			this.diff     = diff;
		}
		public UniStats(double data[]) {
			if (data.length == 0) {
				logger.error("data.length == 0");
				throw new SecuritiesException("data.length == 0");
			}
			size = data.length;
			diff = new double[size];
			mean = mean(data);
			double var = 0;
			for(int i = 0; i < size; i++) {
				double diff = mean - data[i];
				var += diff * diff;
				this.diff[i] = diff;
			}
			variance = var / size;
			sd       = Math.sqrt(variance);
		}
		@Override
		public String toString() {
			return String.format("{%d %8.4f %8.4f %8.4f}", size, mean, variance, sd);
		}
	}
	public static class BiStats {
		public final UniStats stats1;
		public final UniStats stats2;
		public final double   covariance;
		public final double   correlation;
		
		public BiStats(UniStats stats1, UniStats stats2, double covariance, double correlation) {
			this.stats1 = stats1;
			this.stats2 = stats2;
			this.covariance  = covariance;
			this.correlation = correlation;
		}
		
		public BiStats(double data1[], double data2[]) {
			if (data1.length != data2.length) {
				logger.error("data1.length = {}  data2.length = {}", data1.length, data2.length);
				throw new SecuritiesException("data1.length != data2.length");
			}
			final int size = data1.length;
			
			double mean1 = mean(data1);
			double mean2 = mean(data2);
			double cov   = 0;
			double var1  = 0;
			double var2  = 0;
			double diff1[] = new double[size];
			double diff2[] = new double[size];
			for(int i = 0; i < size; i++) {
				double d1 = data1[i] - mean1;
				double d2 = data2[i] - mean2;
				cov  += d1 * d2;
				var1 += d1 * d1;
				var2 += d2 * d2;
				diff1[i] = d1;
				diff2[i] = d2;
			}
			correlation = cov / (Math.sqrt(var1) * Math.sqrt(var2));
			covariance  = cov /size;
			stats1      = new UniStats(size, mean1, var1 / size, diff1);
			stats2      = new UniStats(size, mean2, var2 / size, diff2);
		}
		
		public BiStats(UniStats stats1, UniStats stats2) {
			if (stats1.size != stats1.size) {
				logger.error("stats1.size != stats1.size", stats1.size, stats1.size);
				throw new SecuritiesException("stats1.size != stats1.size");
			}
			final int size = stats1.size;
			this.stats1 = stats1;
			this.stats2 = stats2;
			
			double cov = 0;
			for(int i = 0; i < size; i++) {
				cov  += stats1.diff[i] * stats2.diff[i];
			}
			this.covariance  = cov / size;
			this.correlation = this.covariance / (stats1.sd * stats2.sd);
		}
		
		@Override
		public String toString() {
			return String.format("{%s %s %8.4f %8.4f", stats1, stats2, covariance, correlation);
		}
	}
	
	public static BiStats[][] getMatrix(UniStats stats[]) {
		final int size = stats.length;
		if (stats.length == 0) {
			logger.error("stats.length == 0");
			throw new SecuritiesException("stats.length == 0");
		}
		{
			final int statsSize = stats[0].size;
			for(int i = 0; i < stats.length; i++) {
				if (stats[i].size != statsSize) {
					// TODO debug code
					logger.error("stats[i].size != statsSize");
					logger.error("stats[{}].size = {}  statsSize = {}", i, stats[i], statsSize);
					throw new SecuritiesException("stats[i].size != size");
				}
			}
		}
		BiStats ret[][] = new BiStats[size][size];
		for(int i = 0; i < size; i++) {
			for(int j = 0; j < size; j++) {
				ret[i][j] = ret[j][i] = new DoubleArray.BiStats(stats[i], stats[j]);
				if (i == j) break;
			}
		}
		
		return ret;
	}

	public static double cor(double[] data1, double data2[]) {
		if (data1.length != data2.length) {
			logger.error("data1.length = {}  data2.length = {}", data1.length, data2.length);
			throw new SecuritiesException("data1.length != data2.length");
		}
		final int size = data1.length;
		
		double mean1 = mean(data1);
		double mean2 = mean(data2);
		double cov   = 0;
		double var1  = 0;
		double var2  = 0;
		for(int i = 0; i < size; i++) {
			double diff1 = data1[i] - mean1;
			double diff2 = data2[i] - mean2;
			cov  += diff1 * diff2;
			var1 += diff1 * diff1;
			var2 += diff2 * diff2;
		}
		return cov / (Math.sqrt(var1) * Math.sqrt(var2));
	}
	
	public static double var(double[] data, double mean) {
		final int size = data.length;
		if (size == 0) return Double.NaN;
		double ret = 0;
		for(double value: data) {
			double diff = mean - value;
			ret += diff * diff;
		}
		return ret / size;
	}
	public static double var(double[] data) {
		return var(data, mean(data));
	}
	public static double sd(double[] data) {
		return sd(data, mean(data));
	}
	public static double sd(double[] data, double mean) {
		if (data.length == 0) return Double.NaN;
		return Math.sqrt(var(data, mean));
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
			
			double sd = var(save, sum / dataSize);

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
	
	static void testVarCovCor() {
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

		logger.info("");
		{
			DescriptiveStatistics desca = new DescriptiveStatistics();
			DescriptiveStatistics descb = new DescriptiveStatistics();
			for(double data: data_a) desca.addValue(data);
			for(double data: data_b) descb.addValue(data);
			double avga = desca.getMean();
			double avgb = descb.getMean();
			double vara = desca.getPopulationVariance();
			double varb = descb.getPopulationVariance();
			double cov  = new Covariance().covariance(data_a, data_b, false);
			double cor  = new PearsonsCorrelation().correlation(data_a, data_b);
			
			logger.info("math3 {}", String.format("%8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f", avga, avgb, vara, varb, Math.sqrt(vara), Math.sqrt(varb), cov, cor));
		}
		{
			double avga = mean(data_a);
			double avgb = mean(data_b);
			double vara = var(data_a);
			double varb = var(data_b);
			double sda  = sd(data_a);
			double sdb  = sd(data_b);
			double cov  = cov(data_a, data_b);
			double cor  = cor(data_a, data_b);
			
			logger.info("mine  {}", String.format("%8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f", avga, avgb, vara, varb, sda, sdb, cov, cor));
		}
		{
			UniStats statsa  = new UniStats(data_a);
			UniStats statsb  = new UniStats(data_b);
			BiStats  statsab = new BiStats(data_a, data_b);
			double avga = statsa.mean;
			double avgb = statsb.mean;
			double vara = statsa.variance;
			double varb = statsb.variance;
			double sda  = Math.sqrt(vara);
			double sdb  = Math.sqrt(varb);
			double cov  = statsab.covariance;
			double cor  = statsab.correlation;
			
			logger.info("uni   {}", String.format("%8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f", avga, avgb, vara, varb, sda, sdb, cov, cor));
		}
		{
			UniStats statsa  = new UniStats(data_a);
			UniStats statsb  = new UniStats(data_b);
			BiStats  statsab = new BiStats(data_a, data_b);
			double avga = statsa.mean;
			double avgb = statsb.mean;
			double vara = statsab.stats1.variance;
			double varb = statsab.stats2.variance;
			double sda  = Math.sqrt(vara);
			double sdb  = Math.sqrt(varb);
			double cov  = statsab.covariance;
			double cor  = statsab.correlation;
			
			logger.info("bi    {}", String.format("%8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f", avga, avgb, vara, varb, sda, sdb, cov, cor));
		}
		{
			UniStats statsa  = new UniStats(data_a);
			UniStats statsb  = new UniStats(data_b);
			BiStats  statsab = new BiStats(statsa, statsb);
			double avga = statsa.mean;
			double avgb = statsb.mean;
			double vara = statsab.stats1.variance;
			double varb = statsab.stats2.variance;
			double sda  = Math.sqrt(vara);
			double sdb  = Math.sqrt(varb);
			double cov  = statsab.covariance;
			double cor  = statsab.correlation;
			
			logger.info("bi    {}", String.format("%8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f  %8.5f", avga, avgb, vara, varb, sda, sdb, cov, cor));
		}
	}

	public static void main(String[] args) {
		testTable53();
		testTable55();
		testVarCovCor();
	}
}
