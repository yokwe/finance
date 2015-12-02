package yokwe.finance.securities.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RiskMetrics {
	private static final Logger logger = LoggerFactory.getLogger(RiskMetrics.class);

	public static double CONFIDENCE_95_PERCENT = 1.62;
	public static double CONFIDENCE_99_PERCENT = 2.33;

	public static double DEFAULT_DECAY_FACTOR = 0.94;
	public static double DEFAULT_CONFIDENCE   = CONFIDENCE_95_PERCENT;
	
	final double   decayFactor;
	
	public RiskMetrics(double decayFactor) {
		this.decayFactor = decayFactor;
	}
	public RiskMetrics() {
		this(DEFAULT_DECAY_FACTOR);
	}
	
	public static double[] getLogReturn(double data[]) {
		double ret[] = new double[data.length];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = Math.log(data[i] / data[i - 1]);
		}
		return ret;
	}
	
	public static double getEMA(double ema, double value, double decayFactor) {
		return ema + decayFactor * (value - ema);
	}
	
	public static double getVAR_EWMA(double var, double value, double decayFactor) {
		// Assume sampled mean of value is zero
		return value + decayFactor * (var - value);
	}
	// This is a variance. So to get Standard Deviation, apply square root to it.
	public static double[] getVariance(double data[], double decayFactor) {
		double ret[] = new double[data.length];
		
		double var = data[0] * data[0];
		for(int i = 0; i < ret.length; i++) {
			final double value = data[i] * data[i];
			var = getVAR_EWMA(var, value, decayFactor);
			ret[i] = var;
		}
		return ret;
	}
	
	public static double[] getCovariance(double data1[], double data2[], double decayFactor) {
		double ret[] = new double[data1.length];
		
		double cova = data1[0] * data2[0];
		for(int i = 0; i < data1.length; i++) {
			double value = data1[i] * data2[i];
			cova = getVAR_EWMA(cova, value, decayFactor);
			ret[i] = cova;
		}
		return ret;
	}
	
	public static double[] getCorrelation(double data1[], double data2[], double decayFactor) {
		double var1[] = getVariance(data1, decayFactor);
		double var2[] = getVariance(data2, decayFactor);
		double cova[] = getCovariance(data1, data2, decayFactor);
		
		double ret[] = new double[data1.length];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = cova[i] / (Math.sqrt(var1[i]) * Math.sqrt(var2[i]));
		}
		return ret;
	}
	
	public double[] getVariance(double data[]) {
		return getVariance(data, decayFactor);
	}
	public double[] getCovariance(double data1[], double data2[]) {
		return getCovariance(data1, data2, decayFactor);
	}
	public double[] getCorrelation(double data1[], double data2[]) {
		return getCorrelation(data1, data2, decayFactor);
	}

	public static void main(String args[]) {
		final double[] usddem_data = {
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
	
		double sp500_data[] = {
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
		
		RiskMetrics rm = new RiskMetrics();
		double usddem_var[] = rm.getVariance(usddem_data);
		double sp500_var[] = rm.getVariance(sp500_data);
		double cova[] = rm.getCovariance(usddem_data, sp500_data);
		double corr[] = rm.getCorrelation(usddem_data, sp500_data);
		
		for(int i = 0; i < usddem_data.length; i++) {
			logger.info("{}", String.format("%8.3f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", usddem_data[i], sp500_data[i], usddem_var[i], sp500_var[i], cova[i], corr[i]));
		}
	}
}
