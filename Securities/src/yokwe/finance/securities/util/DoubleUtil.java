package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

public final class DoubleUtil {
	//
	// Utility Methods for array of double
	//
	public static double mean(double data[]) {
		final int size = data.length;
		double ret = 0;
		for(int i = 0; i < size; i++) {
			ret += data[i];
		}
		return ret / size;
	}
	public static double[] multiply(double a[], double b[]) {
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] * b[i];
		}
		return ret;
	}
	public static double[] divide(double a[], double b[]) {
		double ret[] = new double[a.length];
		for(int i = 0; i < a.length; i++) {
			ret[i] = a[i] / b[i];
		}
		return ret;
	}

	//
	// DoublePredictor
	//
	private static final DoublePredicate skipNaN = new DoublePredicate() {
		public boolean test(double value) {
			return !Double.isNaN(value);
		}
	};
	public static DoublePredicate skipNan() {
		return skipNaN;
	}
	
	
	//
	// DoubleUnaryOperator
	//
	
	// square
	private static final DoubleUnaryOperator squareInstance = new DoubleUnaryOperator() {
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			return value * value;
		}
	};
	public static DoubleUnaryOperator square() {
		return squareInstance;
	}

	// sqrt
	private static final DoubleUnaryOperator sqrtInstance = new DoubleUnaryOperator() {
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			return Math.sqrt(value);
		}
	};
	public static DoubleUnaryOperator sqrt() {
		return sqrtInstance;
	}
	

	// relativeReturn
	private static final class RelativeReturn implements DoubleUnaryOperator {
		private double lastValue;
		public RelativeReturn() {
			lastValue = Double.NaN;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return Double.NaN;
			}
			final double ret = ((value / lastValue) - 1.0) * 100.0;
			lastValue = value;
			return ret;
		}
	}
	public static DoubleUnaryOperator relativeReturn() {
		return new RelativeReturn();
	}
	public static DoubleStream relativeReturn(double[] data) {
		return Arrays.stream(data).map(relativeReturn()).filter(skipNan());
	}
	
	// logReturn
	private static final class LogReturn implements DoubleUnaryOperator {
		private double lastValue;
		public LogReturn() {
			lastValue = Double.NaN;
		}
		@Override
		public double applyAsDouble(double value) {
			if (Double.isNaN(value)) return Double.NaN; 
			if (Double.isNaN(lastValue)) {
				lastValue = value;
				return Double.NaN;
			}
			final double ret = Math.log(value / lastValue) * 100.0;
			lastValue = value;
			return ret;
		}
	}
	public static DoubleUnaryOperator logReturn() {
		return new LogReturn();
	}
	public static DoubleStream logReturn(double[] data) {
		return Arrays.stream(data).map(logReturn()).filter(skipNan());
	}

	// EMA - exponential moving average
	// dataSize 32 => decayFactor 93.94
	public static double getAlpha(int dataSize) {
		// From alpha = 2 / (N + 1)
		return 2.0 / (dataSize + 1.0);
	}
	public static double getAlphaFromDecayFactor(double decayFactor) {
		return 1.0 - decayFactor;
	}
	public static int getDataSize(double alpha) {
		// From alpha = 2 / (N + 1)
		return (int)Math.round((2.0 / alpha) - 1.0);
	}
	// From k = log(0.01) / log (1 - alpha)
	public static int getDataSize99(double alpha) {
		return (int)Math.round(Math.log(0.01) / Math.log(1 - alpha));
	}
	private static final class EMA implements DoubleUnaryOperator {
		private final double alpha;
		private double       var;
		
		// dataSize 32 => decayFactor 93.94
		private EMA(int dataSize) {
			alpha = 2.0 / (dataSize + 1.0);
			var   = Double.NaN;
		}
		
		private EMA(double alpha) {
			this.alpha = alpha;
		}
		
		@Override
		public double applyAsDouble(double value) {
			// Ignore Nan
			if (Double.isNaN(value)) return Double.NaN;
			
			if (Double.isNaN(var)) {
				var = value;
			}
			var = var + alpha * (value - var);
			return var;
		}
	};
	public static DoubleUnaryOperator ema(double alpha) {
		return new EMA(alpha);
	}
	public static DoubleUnaryOperator emaFromDecayFactor(double decayFactor) {
		return new EMA(getAlphaFromDecayFactor(decayFactor));
	}
	public static DoubleUnaryOperator ema(int dataSize) {
		return new EMA(getAlpha(dataSize));
	}
	
}
