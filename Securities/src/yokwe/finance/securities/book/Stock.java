package yokwe.finance.securities.book;

import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Stock {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);

	public String symbol;
	public String tradeDate;
	public double quantity;
	public double value; // value in JPY for tax declaration
	
	private Stock(String symbol, String tradeDate, double quantity, double value) {
		this.symbol    = symbol;
		this.tradeDate = tradeDate;
		this.quantity  = quantity;
		this.value     = value;
	}
	
	public static class Map {
		private java.util.Map<String, Stock> stockMap = new TreeMap<>();
		
		void buy(String symbol, double quantity, String tradeDate, double value) {
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				stock.tradeDate  = tradeDate;
				stock.quantity  += quantity;
				stock.value     += value;
			} else {
				stockMap.put(symbol, new Stock(symbol, tradeDate, quantity, value));
			}
		}
		
		void sell(String symbol, double sellQuantity, String tradeDate, int value) {
			if (stockMap.containsKey(symbol)) {
				Stock stock = stockMap.get(symbol);
				
				// See below for calculation of obtaining cost of securities.
				//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
				double unitCost = Math.ceil(stock.value / stock.quantity);
				double sellValue = Math.round(sellQuantity * unitCost);
				
				stock.tradeDate = tradeDate;
				stock.quantity  = stock.quantity - sellQuantity;
				stock.value     = (int)Math.round(stock.quantity * unitCost);
				
				logger.info("{}", String.format("%s %-8s  %10.5f  %8.0f  %8.0f", tradeDate, symbol, sellQuantity, unitCost, sellValue));
			} else {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexptected");
			}
		}
	}
}
