package yokwe.finance.stock.report;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.DoubleUtil;

public class Portfolio {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Portfolio.class);

	public static class Stock {
		String symbol;
		double quantity;
		double cost;
		
		Stock(String symbol) {
			this.symbol   = symbol;
			this.quantity = 0;
			this.cost     = 0;
		}
		Stock(String symbol, double quantity, double cost) {
			this.symbol   = symbol;
			this.quantity = quantity;
			this.cost     = cost;
		}
	}
	
	public Map<String, Stock> stockMap;
	
	public Portfolio() {
		stockMap = new TreeMap<>();
	}
	
	void change(String symbol, double quantity, String newSymbol, double newQuantity) {
		// Sanity check
		if (newQuantity <= 0) {
			logger.error("Unexpected  {} {}  {} {}", symbol, quantity, newSymbol, newQuantity);
			throw new UnexpectedException("Unexpected");							
		}
		if (newQuantity <= 0) {
			logger.error("Unexpected  {} {}  {} {}", symbol, quantity, newSymbol, newQuantity);
			throw new UnexpectedException("Unexpected");							
		}
		if (!stockMap.containsKey(symbol)) {
			logger.error("Unexpected  {} {}  {} {}", symbol, quantity, newSymbol, newQuantity);
			throw new UnexpectedException("Unexpected");				
		}
		Stock stock = stockMap.get(symbol);
		stockMap.remove(symbol);
		
		Stock newStock = new Stock(newSymbol, newQuantity, stock.cost);
		stockMap.put(newSymbol, newStock);
	}
	void buy(String symbol, double quantity, double cost) {
		// Sanity check
		if (quantity <= 0) {
			logger.error("Unexpected  {} {} {}", symbol, quantity, cost);
			throw new UnexpectedException("Unexpected");							
		}
		if (cost < 0) {
			logger.error("Unexpected  {} {} {}", symbol, quantity, cost);
			throw new UnexpectedException("Unexpected");							
		}
		
		if (!stockMap.containsKey(symbol)) {
			stockMap.put(symbol, new Stock(symbol));
		}
		Stock stock = stockMap.get(symbol);
		stock.quantity = stock.quantity + quantity;
		stock.cost = DoubleUtil.roundPrice(stock.cost + cost);
	}
	double sell(String symbol, double quantity) {
		// Sanity check
		if (quantity <= 0) {
			logger.error("Unexpected  {} {}", symbol, quantity);
			throw new UnexpectedException("Unexpected");							
		}
		if (!stockMap.containsKey(symbol)) {
			logger.error("Unexpected  {}", symbol);
			throw new UnexpectedException("Unexpected");				
		}
		
		Stock stock = stockMap.get(symbol);
		double ratio = quantity / stock.quantity;
		double sellCost = DoubleUtil.roundPrice(stock.cost * ratio);
		
		double newCost = DoubleUtil.roundPrice(stock.cost - sellCost);
		double newQuantity = stock.quantity - quantity;
		
		if (DoubleUtil.isAlmostZero(newQuantity)) stockMap.remove(symbol);

		// Sanity check
		if (newQuantity < 0) {
			logger.error("Unexpected  {} {}", symbol, quantity);
			throw new UnexpectedException("Unexpected");							
		}

		stock.quantity = newQuantity;
		stock.cost = newCost;
		
		return sellCost;
	}
}
