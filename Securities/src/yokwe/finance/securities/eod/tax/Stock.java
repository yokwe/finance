package yokwe.finance.securities.eod.tax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

public class Stock implements Comparable<Stock> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Stock.class);
	
	public final String group;

	// One record for one stock per day
	public final String date;
	public final String symbol;
	
	// Dividend detail
	public double dividend;
	public double dividendFee;
	
	// Buy detail
	public double buyQuantity;
	public double buyFee;
	public double buy;
	
	// Sell detail
	public double sellQuantity;
	public double sellFee;
	public double sell;
	public double sellCost;
	public double sellProfit;
	
	// Value of the date
	public double totalQuantity;
	public double totalCost;
	public double totalValue;
	
	public double totalDividend; // from dividend
	public double totalProfit;   // from buy and sell
	
	private Stock(String group, String date, String symbol,
		double dividend, double dividendFee,
		double buyQuantity, double buyFee, double buy,
		double sellQuantity, double sellFee, double sell, double sellCost, double sellProfit,
		double totalQuantity, double totalCost, double totalValue, double totalDividend, double totalProfit) {
		this.group = group;
		
		this.date   = date;
		this.symbol = symbol;
				
		// Dividend detail
		this.dividend    = dividend;
		this.dividendFee = dividendFee;
				
		// Buy detail
		this.buyQuantity = buyQuantity;
		this.buyFee      = buyFee;
		this.buy         = buy;
				
		// Sell detail
		this.sellQuantity = sellQuantity;
		this.sellFee      = sellFee;
		this.sell         = sell;
		this.sellCost     = sellCost;
		this.sellProfit   = sellProfit;
				
		// Value of the date
		this.totalQuantity = totalQuantity;
		this.totalCost     = totalCost;
		this.totalValue    = totalValue; // unrealized gain = totalValue - totalCost 
		
		// Realized gain
		this.totalDividend = totalDividend;
		this.totalProfit   = totalProfit;
	}
	private Stock(String date, String symbol) {
		this(symbol, date, symbol,
			0, 0,
			0, 0, 0,
			0, 0, 0, 0, 0,
			0, 0, 0, 0, 0);
	}
	private Stock(String group, Stock stock) {
		this(group, stock.date, stock.symbol,
			stock.dividend, stock.dividendFee,
			stock.buyQuantity, stock.buyFee, stock.buy,
			stock.sellQuantity, stock.sellFee, stock.sell, stock.sellCost, stock.sellProfit,
			stock.totalQuantity, stock.totalCost, stock.totalValue,
			stock.totalDividend, stock.totalProfit);
	}
	
	@Override
	public int compareTo(Stock that) {
		int ret = this.group.compareTo(that.group);
		if (ret == 0) ret = this.date.compareTo(that.date);
		return ret;
	}
	
	@Override
	public String toString() {
		return String.format("%-9s %s %-9s %8.2f %8.2f   %8.2f %8.2f %8.2f   %8.2f %8.2f %8.2f %8.2f %8.2f   %8.2f %8.2f %8.2f   %8.2f %8.2f",
				group, date, symbol, dividend, dividendFee,
				buyQuantity, buyFee, buy,
				sellQuantity, sellFee, sell, sellCost, sellProfit,
				totalQuantity, totalCost, totalValue,
				totalDividend, totalProfit
				);
	}

	// key     symbol      date
	static Map<String, NavigableMap<String, Stock>> allStockMap = new TreeMap<>();
	static List<Stock> getStockList() {
		List<Stock> ret = new ArrayList<>();
		allStockMap.values().stream().forEach(map -> ret.addAll(map.values()));		
		Collections.sort(ret);
		return ret;
	}
	static NavigableMap<String, Stock> getStockMap(String symbol) {
		if (!allStockMap.containsKey(symbol)) {
			allStockMap.put(symbol, new TreeMap<>());
		}
		return allStockMap.get(symbol);
	}
	static Stock getStock(String date, String symbol) {
		NavigableMap<String, Stock> stockMap = getStockMap(symbol);
		
		if (stockMap.containsKey(date)) {
			// Entry is already exists. use the entry.
		} else {
			Stock stock = new Stock(date, symbol);
			
			Map.Entry<String, Stock>entry = stockMap.lowerEntry(date);
			if (entry != null) {
				Stock lastStock = entry.getValue();
				
				// Copy totalXXX from lastStcok
				stock.totalQuantity = lastStock.totalQuantity;
				stock.totalCost     = lastStock.totalCost;
				stock.totalValue    = lastStock.totalValue;
				
				stock.totalDividend = lastStock.totalDividend;
				stock.totalProfit   = lastStock.totalProfit;
			}
			stockMap.put(date, stock);
		}
		
		Stock ret = stockMap.get(date);
		return ret;
	}
	public static List<String> getSymbolList() {
		List<String> ret = allStockMap.keySet().stream().collect(Collectors.toList());
		Collections.sort(ret);
		return ret;
	}
	public static List<Stock> getStockList(String symbol) {
		if (!allStockMap.containsKey(symbol)) {
			logger.error("No such symbol  {}", symbol);
			throw new SecuritiesException("No such stock");
		}
		NavigableMap<String, Stock> stockMap = allStockMap.get(symbol);
		List<Stock> ret = stockMap.values().stream().collect(Collectors.toList());
		Collections.sort(ret);
		return ret;
	}
	
	public static void dividend(String date, String symbol, double dividend, double dividendFee) {
		Stock stock = getStock(date, symbol);
		
		stock.dividend      = Transaction.roundPrice(stock.dividend      + dividend);
		stock.dividendFee   = Transaction.roundPrice(stock.dividendFee   + dividendFee);
		stock.totalDividend = Transaction.roundPrice(stock.totalDividend + dividend - dividendFee);
	}

	public static void buy(String date, String symbol, double buyQuantity, double buy, double buyFee) {
//		logger.info("{}", String.format("buyQuantity  = %8.2f  buy  = %8.2f  buyFee  = %8.2f", buyQuantity, buy, buyFee));
		Stock stock = getStock(date, symbol);
		
		// If this is first buy for the stock, clear totalXXX
		if (stock.totalQuantity == 0) {
//			stock.totalQuantity = 0;
			stock.totalCost     = 0;
			stock.totalValue    = 0;
					
			stock.totalDividend = 0;
			stock.totalProfit   = 0;
		}
		
		stock.buyQuantity = Transaction.roundQuantity(stock.buyQuantity + buyQuantity);
		stock.buyFee      = Transaction.roundPrice(stock.buyFee + buyFee);
		stock.buy         = Transaction.roundPrice(stock.buy    + buy);
		
		stock.totalQuantity = Transaction.roundQuantity(stock.totalQuantity + buyQuantity);
		stock.totalCost     = Transaction.roundPrice(stock.totalCost + buy + buyFee);
	}
	
	public static void sell(String date, String symbol, double sellQuantity, double sell, double sellFee) {
//		logger.info("{}", String.format("sellQuantity = %8.2f  sell = %8.2f  sellFee = %8.2f", sellQuantity, sell, sellFee));
		Stock stock = getStock(date, symbol);
		
		double sellCost   = Transaction.roundPrice((stock.totalCost / stock.totalQuantity) * sellQuantity);
		double sellProfit = Transaction.roundPrice(sell - sellFee - sellCost);
//		logger.info("{}", String.format("sellCost = %8.2f  sellProfit = %8.2f", sellCost, sellProfit));
		
		stock.sellQuantity = Transaction.roundQuantity(stock.sellQuantity + sellQuantity);
		stock.sellFee      = Transaction.roundPrice(stock.sellFee    + sellFee);
		stock.sell         = Transaction.roundPrice(stock.sell       + sell);
		
		stock.sellCost     = Transaction.roundPrice(stock.sellCost   + sellCost);
		stock.sellProfit   = Transaction.roundPrice(stock.sellProfit + sellProfit);
		
		stock.totalQuantity = Transaction.roundQuantity(stock.totalQuantity - sellQuantity);
		stock.totalCost     = Transaction.roundPrice(stock.totalCost - sellCost);
		
		stock.totalProfit = Transaction.roundPrice(stock.totalProfit   + sellProfit);
		
		if (DoubleUtil.isAlmostZero(stock.totalQuantity)) {
			stock.totalQuantity = 0;
		}
	}

	public static void change(String date, String symbol, double quantity, String newSymbol, double newQuantity) {
		// Sanity check
		if (!allStockMap.containsKey(symbol)) {
			logger.error("No such symbol  {} {}", date, symbol);
			throw new SecuritiesException("No such symbol");
		}
		if ((!symbol.equals(newSymbol)) && allStockMap.containsKey(newSymbol)) {
			logger.error("Duplicate symbol  {}", newSymbol);
			throw new SecuritiesException("Duplicate symbol");
		}
		
		NavigableMap<String, Stock> stockMap = allStockMap.get(symbol);
		if (stockMap.containsKey(date)) {
			logger.error("Already entry exists.  {}  {}", date, symbol);
			throw new SecuritiesException("Already entry exists");
		}
		NavigableMap<String, Stock> newStockMap = new TreeMap<>();
		for(Stock stock: stockMap.values()) {
			Stock newStock = new Stock(newSymbol, stock);
			newStockMap.put(stock.date, newStock);
		}
		
		allStockMap.remove(symbol);
		allStockMap.put(newSymbol, newStockMap);

		Stock stock = getStock(date, newSymbol);
		
		// Change totalQuantity of newSymbol
		stock.totalQuantity = newQuantity;

		newStockMap.put(date, stock);
	}

	public static void updateTotalValue(String date, String symbol, double price) {
		NavigableMap<String, Stock> stockMap = allStockMap.get(symbol);
		if (stockMap.containsKey(date)) {
			// Entry is already exists. use the entry.
		} else {
			Stock stock = getStock(date, symbol);
			stockMap.put(date, stock);
		}
		Stock stock = stockMap.get(date);
		stock.totalValue = Transaction.roundPrice(stock.totalQuantity * price);
	}
}
