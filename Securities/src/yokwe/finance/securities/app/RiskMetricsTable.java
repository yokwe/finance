package yokwe.finance.securities.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiskMetricsTable {
	private static final Logger logger = LoggerFactory.getLogger(RiskMetricsTable.class);
	
	static double CONFIDENCE_95 = 1.65;
	static double CONFIDENCE_99 = 2.33;
	
	public static void main(String[] args) {
		logger.info("START");
		table_4_1();
		table_5_2();
		table_5_3();
		logger.info("STOP");
	}
	
	private static void table_4_1() {
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


		logger.info("table 4.1");
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
			
			logger.info("table 4.1  {}", String.format("%8.5f  %8.3f  %8.3f  %8.3f", P1, D1, R1, r1));
		}
	}
	
	private static void table_5_2() {
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
		final int T = data.length;
		final double DECAY_FACTOR = 0.94;
		
		final double expTable[] = new double[T];
		{
			double t = 1.0;
			for(int i = 0; i < T; i++) {
				expTable[T - i - 1] = (1 - DECAY_FACTOR) * t;
				t *= DECAY_FACTOR;
			}
		}

		logger.info("table 5.2");
		for(int i = 0; i < T; i++) {
			double a = data[i];
			double b = a * a;
			double c = 1.0 / T;
			double d = expTable[i];

			logger.info("table 5.2  {}", String.format("%8.5f  %8.3f  %8.3f  %8.3f  %8.3f  %8.3f", a, b, c, d, b * c, b * d));
		}
	}
	
	static void table_5_3() {
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
		final int T = data.length;
		final double DECAY_FACTOR = 0.94;
		
		logger.info("table 5.3");
		double b = data[0] * data[0];
		for(int i = 0; i < T; i++) {
			double a = data[i];
			b = (DECAY_FACTOR * b) + (1 - DECAY_FACTOR) * (a * a);

			logger.info("table 5.3  {}", String.format("%8.3f  %8.3f", a, b));
		}
	}
}
