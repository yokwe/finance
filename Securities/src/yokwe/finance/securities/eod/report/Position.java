package yokwe.finance.securities.eod.report;

import java.util.List;

import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class Position {
//	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Position.class);
	
	public static final double COMMISSION = 5;
	public static final double NO_VALUE   = -1;

	public final String symbol;
	public final double quantity;
	
	public Position(String symbol, double quantity) {
		this.symbol   = symbol;
		this.quantity = quantity;
	}
	
	@Override
	public String toString() {
		return String.format("[%s %.4f]", symbol, quantity);
	}

	
	public static double getValue(String date, Position position) {
		double ret = 0;
		
		String symbol   = position.symbol;
		double quantity = position.quantity;
		
		if (PriceUtil.contains(symbol, date)) {
			double price = PriceUtil.getClose(symbol, date);
			double value = (price * quantity) - COMMISSION;
			
			ret = DoubleUtil.round(ret + value, 2);
		} else {
			// price of symbol at the date is not available
			return NO_VALUE;
		}
		return ret;
	}

	public static double getValue(String date, List<Position> positionList) {
		double ret = 0;
		
		for(Position position: positionList) {
			double value = getValue(date, position);
			if (value == NO_VALUE) return NO_VALUE;
			
			ret = DoubleUtil.round(ret + value, 2);
		}
		return ret;
	}
}
