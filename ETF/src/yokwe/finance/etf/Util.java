package yokwe.finance.etf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

public final class Util {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Util.class);

	public static String getContents(File file) {
		char[] buffer = new char[65536];
		
		StringBuilder ret = new StringBuilder();
		
		try (BufferedReader bfr = new BufferedReader(new FileReader(file), buffer.length)) {
			for(;;) {
				int len = bfr.read(buffer);
				if (len == -1) break;
				
				ret.append(buffer, 0, len);
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new ETFException();
		}
		return ret.toString();
	}
	
	public static double StandardDeviationSample(List<Double> valueList) {
		return StandardDeviation(valueList, 2);
	}
	public static double StandardDeviationPopulation(List<Double> valueList) {
		return StandardDeviation(valueList, 1);
	}
	public static double StandardDeviation(List<Double> valueList, final int offset) {
	    double M = 0.0;
	    double S = 0.0;
	    int k = 1;
	    for (double value: valueList) {
	        double tmpM = M;
	        M += (value - tmpM) / k;
	        S += (value - tmpM) * (value - M);
	        k++;
	    }
	    return Math.sqrt(S / (k-offset));
	}
	
	public static void main(String[] args) {
		Double[] data = new Double[] {53.0, 61.0, 49.0, 67.0, 55.0, 63.0};
		logger.info("SD = {}", StandardDeviationSample(Arrays.asList(data)));
		logger.info("SD = {}", StandardDeviationPopulation(Arrays.asList(data)));
	}
}
