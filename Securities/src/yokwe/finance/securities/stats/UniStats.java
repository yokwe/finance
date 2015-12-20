package yokwe.finance.securities.stats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class UniStats {
	private static final Logger logger = LoggerFactory.getLogger(UniStats.class);

	public final int    size;
	public final double mean;
	public final double variance;
	public final double sd;       // standard deviation
	public final double rsd;      // relative standard deviation == coefficient of variation
	public final double diff[];
	
	public UniStats(int size, double mean, double variance, double diff[]) {
		this.size     = size;
		this.mean     = mean;
		this.variance = variance;
		this.sd       = Math.sqrt(variance);
		this.rsd      = sd / mean;
		this.diff     = diff;
	}
	public UniStats(double data[]) {
		if (data.length == 0) {
			logger.error("data.length == 0");
			throw new SecuritiesException("data.length == 0");
		}
		size = data.length;
		diff = new double[size];
		mean = DoubleArray.mean(data);
		double var = 0;
		for(int i = 0; i < size; i++) {
			double diff = mean - data[i];
			var += diff * diff;
			this.diff[i] = diff;
		}
		variance = var / size;
		sd       = Math.sqrt(variance);
		rsd      = sd / mean;
	}
	@Override
	public String toString() {
		return String.format("{%d %8.4f %8.4f %8.4f %8.4f}", size, mean, variance, sd, rsd);
	}
}