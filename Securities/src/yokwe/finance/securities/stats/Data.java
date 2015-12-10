package yokwe.finance.securities.stats;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class Data {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Data.class);

	public static class Daily {
		public final String date;
		public final double value;
		public Daily(String date, double value) {
			this.date  = date;
			this.value = value;
		}
		public Daily getInstance(double newValue) {
			return new Daily(this.date, newValue);
		}
	}
	
	public  final String symbol;
	private final Daily  daily[];
	public Data(String symbol, Daily that[]) {
		if (symbol == null) {
			logger.error("symbol == null");
			throw new SecuritiesException("symbol == null");
		}
		if (that == null) {
			logger.error("that == null");
			throw new SecuritiesException("that == null");
		}
		this.symbol = symbol;
		this.daily  = Arrays.copyOf(that, that.length);
	}
	public Data getInstance(Daily that[]) {
		return new Data(symbol, Arrays.copyOf(that, that.length));
	}
	public Data getInstance(double that[]) {
		if (that == null) {
			logger.error("that == null");
			throw new SecuritiesException("that == null");
		}
		if (that.length != daily.length) {
			logger.error("that.length != daily.length");
			logger.error("that.length = {}  daily.length = {}", that.length, daily.length);
			throw new SecuritiesException("that.length != daily.length");
		}
		Daily newDaily[] = new Daily[that.length];
		for(int i = 0; i < that.length; i++) {
			newDaily[i] = daily[i].getInstance(that[i]);
		}
		
		return new Data(symbol, newDaily);
	}
	public Daily[] toArray() {
		return Arrays.copyOf(daily, daily.length);
	}
	public double[] toDoubleArray() {
		return Arrays.stream(daily).mapToDouble(o -> o.value).toArray();
	}
}
