package yokwe.finance.securities.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RiskMetrics {
	private static final Logger logger = LoggerFactory.getLogger(RiskMetrics.class);

	public static double CONFIDENCE_95_PERCENT = 1.62;
	public static double CONFIDENCE_99_PERCENT = 2.33;

	public static double DEFAULT_DECAY_FACTOR = 0.94;
	public static double DEFAULT_CONFIDENCE   = CONFIDENCE_95_PERCENT;
	
	final int      numberOfSample;
	final double   decayFactor;
	final double[] decayArray;
	
	public RiskMetrics(int numberOfSample, double decayFactor) {
		this.numberOfSample = numberOfSample;
		this.decayFactor    = decayFactor;
		
		// build decayArray
		decayArray = new double[numberOfSample];
		double df = 1.0;
		for(int i = 0; i < decayArray.length; i++) {
			decayArray[decayArray.length - i - 1] = df;
			df *= decayFactor;
		}
	}
	public RiskMetrics(int numberOfSample) {
		this(numberOfSample, DEFAULT_DECAY_FACTOR);
	}
	
	public static double[] getLogReturn(double data[]) {
		double ret[] = new double[data.length];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = Math.log(data[i] / data[i - 1]);
		}
		return ret;
	}
	
	public static double[] getRecursiveVariance(double data[], double decayFactor) {
		double ret[] = new double[data.length];
		
		double b = data[0] * data[0];
		for(int i = 0; i < ret.length; i++) {
			double a = data[i];
			b = (decayFactor * b) + (1 - decayFactor) * (a * a);
			ret[i] = b;
		}
		return ret;
	}
	
	public double[] getRecursiveVariance(double data[]) {
		return getRecursiveVariance(data, decayFactor);
	}

	public static void main(String args[]) {
		final double[] data = {
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

		RiskMetrics rm = new RiskMetrics(data.length);
		
		double var[] = rm.getRecursiveVariance(data);
		
		for(int i = 0; i < var.length; i++) {
			logger.info("{}", String.format("%6.3f   %6.3f", data[i], var[i]));
		}
	}
	
	
}
