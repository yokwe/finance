package yokwe.finance.securities.stats;

import java.util.Arrays;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class Data {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Data.class);

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
	
	public static double mean(double[] data) {
		if (data.length == 0) return Double.NaN;
		double ret = 0;
		for(double value: data) ret += value;
		return ret / data.length;
	}
	public static double variance(double[] data) {
		if (data.length == 0) return Double.NaN;
		double mean = mean(data);
		double ret = 0;
		for(double value: data) {
			double t = mean - value;
			ret += t * t;
		}
		return ret / data.length;
	}
	public static double standardDeviation(double[] data) {
		if (data.length == 0) return Double.NaN;
		return Math.sqrt(variance(data));
	}

	public static class Daily {
		public final String date;
		public final double value;
		public Daily(String date, double value) {
			this.date  = date;
			this.value = value;
		}
		private Daily logReturn(Daily yesterday) {
			return new Daily(this.date, Math.log(this.value / yesterday.value));
		}
		private Daily relativeReturn(Daily yesterday) {
			return new Daily(this.date, (this.value / yesterday.value) - 1.0);
		}
		private Daily getInstance(double newValue) {
			return new Daily(this.date, newValue);
		}
	}
	
	public  final String symbol;
	private final Daily  list[];
	private final int    size;
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
		this.list   = Arrays.copyOf(that, that.length);
		this.size   = list.length;
	}
	public Data getInstance(Daily that[]) {
		return new Data(symbol, Arrays.copyOf(that, that.length));
	}
	public Daily[] toArray() {
		return Arrays.copyOf(list, list.length);
	}
	public double[] toDoubleArray() {
		return Arrays.stream(list).mapToDouble(o -> o.value).toArray();
	}
	private Daily[] logReturnList() {
		Daily ret[] = new Daily[size - 1];
		int index = 0;
		Daily yesterday = null;
		for(Daily today: list) {
			if (index == 0) {
				yesterday = today;
				index++;
			} else {
				ret[index++] = today.logReturn(yesterday);
				yesterday = today;
			}
		}
		return ret;
	}
	public Data logReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		return getInstance(logReturnList());
	}
	private Daily[] relativeReturnList() {
		Daily ret[] = new Daily[size - 1];
		int index = 0;
		Daily yesterday = null;
		for(Daily today: list) {
			if (index == 0) {
				yesterday = today;
				index++;
			} else {
				ret[index++] = today.relativeReturn(yesterday);
				yesterday = today;
			}
		}
		return ret;
	}
	public Data relativeReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		return getInstance(relativeReturnList());
	}
	
	public Data historicalVolatility(double alpha) {
		if (alpha < 0.0 || 1.0 <= alpha) {
			logger.error("alpha = {}", alpha);
			throw new SecuritiesException("error in value of alpha");
		}
		
		Daily[] logReturn = logReturnList();
		Daily[] hv = new Daily[logReturn.length];
		MA ma = new MA.EMA(alpha);
		for(int i = 0; i < logReturn.length; i++) {
			Daily lr = logReturn[i];
			hv[i] = lr.getInstance(ma.applyAsDouble(lr.value * lr.value));
		}
		return getInstance(hv);
	}
	public Data historicalVolatility(int dataSize) {
		if (dataSize <= 0) {
			logger.error("alpha = {}", dataSize);
			throw new SecuritiesException("error in value of dataSize");
		}
		
		Daily[] logReturn = logReturnList();
		Daily[] hv = new Daily[logReturn.length];
		MA ma = new MA.SMA(dataSize);
		for(int i = 0; i < logReturn.length; i++) {
			Daily lr = logReturn[i];
			hv[i] = lr.getInstance(ma.applyAsDouble(lr.value));
		}
		return getInstance(hv);
	}
}
