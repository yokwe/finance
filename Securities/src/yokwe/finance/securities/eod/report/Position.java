package yokwe.finance.securities.eod.report;

import java.util.List;

import yokwe.finance.securities.eod.PriceUtil;

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

	
	public static double getUnrealizedValue(String date, Position position) {
		double ret = 0;
		
		String symbol   = position.symbol;
		double quantity = position.quantity;
		
		if (PriceUtil.contains(symbol, date)) {
			double price = PriceUtil.getClose(symbol, date);
			double value = (price * quantity) - COMMISSION;
			
			ret = Transaction.roundPrice(ret + value);
		} else {
			// price of symbol at the date is not available
			return NO_VALUE;
		}
		return ret;
	}

	// This method is used to get unrealized value of stocks. Value is not accurate. Because commission is fixed values.
	public static double getUnrealizedValue(String date, List<Position> positionList) {
		double ret = 0;
		
		for(Position position: positionList) {
			double value = getUnrealizedValue(date, position);
			if (value == NO_VALUE) return NO_VALUE;
			
			ret = Transaction.roundPrice(ret + value);
		}
		return ret;
	}
}
