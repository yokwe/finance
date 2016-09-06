package yokwe.finance.securities.book;

import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Securities {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Securities.class);

	String symbol;
	String tradeDate;
	double quantities;
	int    value; // value in JPY for tax declaration
	
	private Securities(String symbol, String tradeDate, double quantities, int value) {
		this.symbol     = symbol;
		this.tradeDate  = tradeDate;
		this.quantities = quantities;
		this.value      = value;
	}
	
	public static class Map {
		private java.util.Map<String, Securities> map = new TreeMap<>();
		
		void buy(String symbol, double quantities, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				securities.tradeDate  = tradeDate;
				securities.quantities += quantities;
				securities.value      += value;
			} else {
				map.put(symbol, new Securities(symbol, tradeDate, quantities, value));
			}
		}
		
		void sell(String symbol, double sellQuantities, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				
				// See below for calculation of obtaining cost of sesurities.
				//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
				double unitCost = Math.ceil(securities.value / securities.quantities);
				double sellValue = Math.round(sellQuantities * unitCost);
				
				securities.tradeDate  = tradeDate;
				securities.quantities = securities.quantities - sellQuantities;
				securities.value      = (int)Math.round(securities.quantities * unitCost);
				
				logger.info("{}", String.format("%s %-8s  %10.5f  %8.0f  %8.0f", tradeDate, symbol, sellQuantities, unitCost, sellValue));
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
}
