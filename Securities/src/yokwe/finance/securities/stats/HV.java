package yokwe.finance.securities.stats;

public abstract class HV {
	// Calculate standard deviation using
	//   simple moving average of n data
	//   exponential moving average of alpha and n data
	//   recursive exponential moving average of alpha

	// Calculate series of standard deviation from series of data using parameter assigned in constructor
	//   data is log or relative return
	public abstract double[] getValues(double[] data);
	
	protected static double mean(double[] data) {
		if (data.length == 0) return Double.NaN;
		
		double ret = 0;
		for(double value: data) ret += value;
		return ret / data.length;
	}
	protected static double variance(double[] data) {
		if (data.length == 0) return Double.NaN;

		double mean = mean(data);
		double ret = 0;
		for(double value: data) {
			double t = mean - value;
			ret += t * t;
		}
		return ret / data.length;
	}
	protected static double standardDeviation(double[] data) {
		if (data.length == 0) return Double.NaN;

		return Math.sqrt(variance(data));
	}
	
	// Simple moving average
	public static HV sma(int size) {
		return null;
	}
	// Exponential moving average
	public static HV ema(double alpha) {
		return null;
	}
	// Recursive exponential moving average
	public static HV rema(double alpha) {
		return null;
	}
	
	private static class SMA extends HV {
		@Override
		public double[] getValues(double[] data) {
			
			return null;
		}
	}
}
