package yokwe.finance.securities.eod.report;

import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class Position {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Position.class);
	
	public static final double COMMISSION = 5;

	public final String symbol;
	public final double quantity;
	
	public Position(String symbol, double quantity) {
		this.symbol   = symbol;
		this.quantity = quantity;
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
			logger.warn("price of {} at {} is missing", symbol, date);
			return 0;
		}
		return ret;
	}

	public static double getValue(String date, List<Position> positionList) {
		double ret = 0;
		
		for(Position position: positionList) {
			double value = getValue(date, position);
			ret = DoubleUtil.round(ret + value, 2);
		}
		return ret;
	}
}
