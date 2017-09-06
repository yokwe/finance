package yokwe.finance.securities.eod.report;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

// Stock class represents current holding of stocks
// Holding of stocks can be changed by invocation of Stock.buy() and Stock.sell().
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
	
	public static List<Position> getPositionList() {
		List<Position> ret = new ArrayList<>();
		
		for(Stock stock: map.values()) {
			String symbol   = stock.symbol;
			double quantity = stock.totalQuantity;
			
			if (quantity == 0) continue;
			
			ret.add(new Position(symbol, quantity));
		}
		return ret;
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
	
	public static void change(String date, String symbol, double quantity, String newSymbol, double newQuantity) {		
		Stock stock = Stock.get(symbol);
		if (DoubleUtil.isAlmostEqual(stock.totalQuantity, -quantity)) {
			if (DoubleUtil.isAlmostEqual(stock.totalQuantity, newQuantity)) {
				// Simple case: -quantity == newQuantity
				stock.symbol = newSymbol;
				stock.totalQuantity = newQuantity;
				map.remove(symbol);
				map.put(newSymbol, stock);
			} else {
				// Complex case: -quantity != newQuantity
				// TODO Is this correct?
				double totalQuantity = 0;
				double quantityRatio = stock.totalQuantity / newQuantity;
				for(History history: stock.history) {
					history.quantity *= quantityRatio;
					totalQuantity += history.quantity;
				}
				if (!DoubleUtil.isAlmostEqual(totalQuantity, newQuantity)) {
					logger.error("Unexpected {} {} {}", symbol, totalQuantity, newQuantity);
					throw new SecuritiesException("Unexpected");
				}
				stock.symbol = newSymbol;
				stock.totalQuantity = newQuantity;
				map.remove(symbol);
				map.put(newSymbol, stock);
				// TODO Very careful for TAX calculation.
				// TODO Add entry to transaction detail for change of stock buying cost.
				logger.error("Unexpected {} {} {}", symbol, stock.totalQuantity, quantity);
				throw new SecuritiesException("Unexpected");
			}
		} else {
			logger.error("Unexpected {} {} {}", symbol, stock.totalQuantity, quantity);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public static void buy(String date, String symbol, double quantity, double total) {
		Stock stock = Stock.getOrCreate(symbol);
		// Shortcut
		if (DoubleUtil.isAlmostZero(stock.totalQuantity + quantity)) {
			stock.reset();
			return;
		}

		stock.totalQuantity = Transaction.roundQuantity(stock.totalQuantity + quantity);
		stock.totalCost     = Transaction.roundPrice(stock.totalCost + total);
		
		stock.history.add(new History(date, quantity, total));
	}
	
	// Returns sellCost using LIFO method
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
					double cost = Transaction.roundPrice(history.cost * (quantitySell / history.quantity));
					history.quantity = Transaction.roundQuantity(history.quantity - quantitySell);
					history.cost     = Transaction.roundPrice(history.cost     - cost);
					
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
		stock.totalQuantity = Transaction.roundQuantity(totalQuantity);
		stock.totalCost     = Transaction.roundPrice(totalCost);
		
		return Transaction.roundPrice(totalCostBefore - stock.totalCost);
	}
}