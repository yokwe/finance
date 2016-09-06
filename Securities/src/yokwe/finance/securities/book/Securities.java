package yokwe.finance.securities.book;

import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Securities {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Securities.class);

	public String symbol;
	public String tradeDate;
	public double quantity;
	public int    value; // value in JPY for tax declaration
	
	private Securities(String symbol, String tradeDate, double quantity, int value) {
		this.symbol    = symbol;
		this.tradeDate = tradeDate;
		this.quantity  = quantity;
		this.value     = value;
	}
	
	public static class Map {
		private java.util.Map<String, Securities> map = new TreeMap<>();
		
		void buy(String symbol, double quantity, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				securities.tradeDate  = tradeDate;
				securities.quantity  += quantity;
				securities.value     += value;
			} else {
				map.put(symbol, new Securities(symbol, tradeDate, quantity, value));
			}
		}
		
		void sell(String symbol, double sellQuantity, String tradeDate, int value) {
			if (map.containsKey(symbol)) {
				Securities securities = map.get(symbol);
				
				// See below for calculation of obtaining cost of securities.
				//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
				double unitCost = Math.ceil(securities.value / securities.quantity);
				double sellValue = Math.round(sellQuantity * unitCost);
				
				securities.tradeDate = tradeDate;
				securities.quantity  = securities.quantity - sellQuantity;
				securities.value     = (int)Math.round(securities.quantity * unitCost);
				
				logger.info("{}", String.format("%s %-8s  %10.5f  %8.0f  %8.0f", tradeDate, symbol, sellQuantity, unitCost, sellValue));
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
}
