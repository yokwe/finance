package yokwe.finance.securities.eod.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class Stock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);

	private static Map<String, Stock> map = new TreeMap<>();
	
	public static Stock get(String symbol) {
		if (map.containsKey(symbol)) {
			return map.get(symbol);
		} else {
			logger.error("Unknonw symbol {}", symbol);
			throw new SecuritiesException("Unknonw symbol");
		}
	}
	public static Stock getOrCreate(String symbol) {
		if (map.containsKey(symbol)) {
			return map.get(symbol);
		} else {
			Stock stock = new Stock(symbol);
			map.put(symbol, stock);
			return stock;
		}
	}
	public static Map<String, Stock> getMap() {
		return map;
	}
	
	static class History {
		String date;
		double quantity;
		double cost;
		
		History(String date, double quantity, double cost) {
			this.date     = date;
			this.quantity = quantity;
			this.cost     = cost;
		}
		
		@Override
		public String toString() {
			return String.format("%s %10.5f  %8.2f", date, quantity, cost);
		}
	}
	
	public String        symbol;
	//
	public double        totalQuantity;
	public double        totalCost;
	public List<History> history;
	
	public Stock(String symbol) {
		this.symbol        = symbol;
		this.totalQuantity = 0;
		this.totalCost     = 0;			
		this.history       = new ArrayList<>();
	}
	
	void reset() {
		this.totalQuantity = 0;
		this.totalCost     = 0;
		this.history.clear();
	}
	
	@Override
	public String toString() {
		return String.format("%-10s  %10.5f  %8.2f", symbol, totalQuantity, totalCost);
	}
	
	public static void buy(String date, String symbol, double quantity, double total) {
		Stock stock = Stock.getOrCreate(symbol);
		// Shortcut
		if (DoubleUtil.isAlmostZero(stock.totalQuantity + quantity)) {
			stock.reset();
			return;
		}

		stock.totalQuantity = DoubleUtil.round(stock.totalQuantity + quantity, 5);
		stock.totalCost     = DoubleUtil.round(stock.totalCost     + total,    2);
		
		stock.history.add(new History(date, quantity, total));
	}
	
	// Returns sellCost
	public static double sell(String date, String symbol, double quantity, double total) {
		Stock stock = Stock.get(symbol);
		double totalCostBefore = stock.totalCost;
		
		// Shortcut
		if (DoubleUtil.isAlmostEqual(stock.totalQuantity, quantity)) {
			stock.reset();
			return totalCostBefore;
		}
		
		// Update history
		{
			double quantitySell = quantity;
			for(Stock.History history: stock.history) {
				if (DoubleUtil.isAlmostZero(quantitySell)) break;				
				if (history.quantity == 0) continue;
				
				if (DoubleUtil.isAlmostEqual(history.quantity, quantitySell)) {
					quantitySell = 0;
					
					history.quantity = 0;
					history.cost     = 0;
				} else if (history.quantity < quantitySell) {
					quantitySell -= history.quantity;
					
					history.quantity = 0;
					history.cost     = 0;
				} else if (quantitySell < history.quantity) {
					double cost = DoubleUtil.round(history.cost * (quantitySell / history.quantity), 2);
					history.quantity = DoubleUtil.round(history.quantity - quantitySell, 5);
					history.cost     = DoubleUtil.round(history.cost     - cost,         2);
					
					quantitySell = 0;
				} else {
					logger.error("Unexpected history {}", history);
					throw new SecuritiesException("Unexpected");
				}
			}
		}
		
		// Calculate totalCost from history
		double totalCost     = 0;
		double totalQuantity = 0;
		for(Stock.History history: stock.history) {
			totalCost     += history.cost;
			totalQuantity += history.quantity;
		}
		stock.totalQuantity = DoubleUtil.round(totalQuantity, 5);
		stock.totalCost     = DoubleUtil.round(totalCost,     2);
		
		return DoubleUtil.round(totalCostBefore - stock.totalCost, 2);
	}
	
	public static double getUnrealizedGain(String date) {
		double commission = 5;
		double ret = 0;
		
		for(Stock stock: map.values()) {
			String symbol   = stock.symbol;
			double quantity = stock.totalQuantity;
			
			if (quantity == 0) continue;
			
			if (PriceUtil.contains(symbol, date)) {
				double cost  = stock.totalCost;
				double price = PriceUtil.getClose(symbol, date);
				double unrealizedGain = (price * quantity) - commission - cost;
				
				ret = DoubleUtil.round(ret + unrealizedGain, 2);
			} else {
				// price of symbol at the date is not available
				return 0;
			}
		}
		return ret;
	}
}