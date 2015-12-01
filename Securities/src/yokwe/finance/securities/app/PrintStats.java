package yokwe.finance.securities.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrintStats {
	private static final Logger logger = LoggerFactory.getLogger(PrintStats.class);
	
	static double CONFIDENCE_95 = 1.65;
	static double CONFIDENCE_99 = 2.33;
	
	static final double mean(double[] data) {
		final int LENGTH = data.length;
		double ret = 0;
		for(int i = 0; i < LENGTH; i++) {
			ret += data[i];
		}
		return ret / LENGTH;
	}
	
	static double sd_equal(double[] data) {
		final double mean = mean(data);
		final int LENGTH = data.length;
		double ret = 0;
		for(int i = 0; i < LENGTH; i++) {
			final double d = data[i] - mean;
			ret += d * d;
		}
		return Math.sqrt(ret / LENGTH);
	}
	// 0 < lambda < 1
	static double sd_exp(double[] data, final double lambda) {
		final double mean = mean(data);
		final int LENGTH = data.length;
		double ret = 0;
		double lambda_exp = 1;
		for(int i = 0; i < LENGTH; i++) {
			final double d = data[i] - mean;
			ret += lambda_exp * d * d;
			lambda_exp *= lambda;
		}
		return Math.sqrt((1 - lambda) * ret);
	}
	
	
	public static void main(String[] args) {
		logger.info("START");
		print1();
		print2();
		logger.info("STOP");
	}
	
	private static void print1() {
		final double[] data = {
				0.67654,
				0.67732,
				0.67422,
				0.67485,
				0.67604,
				0.67545,
				0.67449,
				0.67668,
				0.67033,
				0.66680,
				0.66609,
				0.66503,
		};


		for(int i = 1; i < data.length; i++) {
			double P0 = data[i - 1];
			double P1 = data[i];
			// absolute price change
			double D1 = (P1 - P0) * 100;
			// price percent change or price return
			double R1 = D1 / P0;
//			double r1 = (Math.log(P1) - Math.log(P0)) * 100;
			// log price change or continuously compound return
			double r1 = Math.log(P1 / P0) * 100;
			
			logger.info("print1 {}", String.format("%8.5f  %8.3f  %8.3f  %8.3f", P1, D1, R1, r1));
		}
	}
	
	private static void print2() {
		final double[] data = {
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

		logger.info("");
		for(int i = 1; i < data.length; i++) {
			double P0 = data[i - 1];
			double P1 = data[i];
			// absolute price change
			double D1 = (P1 - P0) * 100;
			// price percent change or price return
			double R1 = D1 / P0;
//			double r1 = (Math.log(P1) - Math.log(P0)) * 100;
			// log price change or continuously compound return
			double r1 = Math.log(P1 / P0) * 100;

			logger.info("print2 {}", String.format("%8.5f  %8.3f  %8.3f  %8.3f", P1, D1, R1, r1));
		}

	}
}
