package yokwe.finance.stock.util;

import java.util.Arrays;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

public class HV implements DoubleUnaryOperator, DoubleConsumer {
	public  static final double CONFIDENCE_95_PERCENT = 1.65;
	public  static final double CONFIDENCE_99_PERCENT = 2.33;
	
	public  static final int TIME_HORIZON_DAY     =   1;
	public  static final int TIME_HORIZON_WEEK    =   5;
	public  static final int TIME_HORIZON_MONTH   =  21;
	public  static final int TIME_HORIZON_YEAR    = 252;
	
	public  static final double DEFAULT_ALPHA   = 0.06; // 1 - 0.94 
	
	private final MA ema;
	private double last  = Double.NaN;
	
	public HV(double alpha) {
		ema = MA.ema(alpha);
	}
	public HV() {
		this(DEFAULT_ALPHA);
	}
	public HV(double[] values) {
		this();
		Arrays.stream(values).forEach(this);
	}
	public double getValue() {
		return Math.sqrt(ema.getValue());
	}
	
	public double getVaR(double confidence, int timeHorizon) {
		return getValue() * confidence * Math.sqrt(timeHorizon);
	}
	public double getVaR95(int timeHorizon) {
		return getVaR(CONFIDENCE_95_PERCENT, timeHorizon);
	}
	public double getVaR99(int timeHorizon) {
		return getVaR(CONFIDENCE_99_PERCENT, timeHorizon);
	}

	@Override
	public void accept(double value) {
		if (Double.isNaN(last)) {
			last = value;
			return;
		}
		
		final double change = Math.log(value / last);
		last = value;
		
		ema.accept(change * change);
	}

	@Override
	public double applyAsDouble(double value) {
		accept(value);
		return getValue();
	}
}
