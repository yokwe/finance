package yokwe.finance.securities.stats;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleUnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSI implements DoubleUnaryOperator, DoubleConsumer {
	private static final Logger logger = LoggerFactory.getLogger(RSI.class);

	public static final int DEFAULT_PERIDO = 14;
	
	public static final int DEFAULT_OVERSOLD_VALUE   = 70;
	public static final int DEFAULT_OVERBOUGHT_VALUE = 30;
	
	
	private final int  period;
	
	private int    count = 0;
	private double last  = Double.NaN;
	private double gain  = 0;
	private double loss  = 0;
	private MA.EMA gainEMA;
	private MA.EMA lossEMA;
	
	public RSI() {
		this(DEFAULT_PERIDO);
	}
	
	public RSI(int period) {
		this.period = period;
		
		double alpha = 1.0 / period;
		gainEMA = MA.ema(alpha);
		lossEMA = MA.ema(alpha);
	}
	
	private double getRS() {
		double lossValue = lossEMA.getValue();
		double gainValue = gainEMA.getValue();
		
		if (lossValue == 0) return 100;
		if (gainValue == 0) return 0;
		return gainValue / lossValue;
	}
	
	public double getValue() {
		if (count <= period) return Double.NaN;
		double rs = getRS();
		if (rs == 0)   return 0;
		if (rs == 100) return 100;
		return 100.0 - 100.0 / (1.0 + rs);
	}

	@Override
	public void accept(final double value) {
		count++;
		if (Double.isNaN(last)) {
			last = value;
			return;
		}
		
		final double change = value - last;
		last = value;
		
		if (count <= period) {
			if (change < 0.0) {
				loss += -change;
			}
			if (0.0 < change) {
				gain += change;
			}
			if (count == period) {
				lossEMA.accept(loss / period);
				gainEMA.accept(gain / period);
//				logger.info("{}", String.format("%2d  change = %5.2f  gain = %6.2f  loss = %6.2f  rs = %6.2f  rsi = %6.2f", count, change, gain, loss, getRS(), getValue()));
			}
			return;
		}
		
		double lossValue = 0;
		double gainValue = 0;
		if (change < 0.0) {
			lossValue = -change;
		} else if (0.0 < change) {
			gainValue = change;
		}
		
		gainEMA.accept(gainValue);
		lossEMA.accept(lossValue);
	}

	@Override
	public double applyAsDouble(double value) {
		accept(value);
		return getValue();
	}
	
	public static void main(String args[]) {
		// See http://stockcharts.com/school/doku.php?id=chart_school:technical_indicators:relative_strength_index_rsi
		double data[] = {
			44.3389,
			44.0902,
			44.1497,
			43.6124,
			44.3278,
			44.8264,
			45.0955,
			45.4245,
			45.8433,
			46.0826,
			45.8931,
			46.0328,
			45.6140,
			46.2820,
			46.2820,
			46.0028,
			46.0328,
			46.4116,
			46.2222,
			45.6439,
			46.2122,
			46.2521,
			45.7137,
			46.4515,
			45.7835,
			45.3548,
			44.0288,
			44.1783,
			44.2181,
			44.5672,
			43.4205,
			42.6628,
			43.1314,
		};

		RSI rsi = new RSI();
		for(int i = 0; i < data.length; i++) {
			double v = rsi.applyAsDouble(data[i]);
			logger.info("{}", String.format("%2d  %5.2f  %6.2f", i + 1, data[i], v));
		}
	}
}
