package yokwe.finance.stock.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.DoubleUtil;

public class StockHistory implements Comparable<StockHistory> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockHistory.class);
	
	private static int nextSession = 1;
	
	public String group;
	public int    session;

	// One record for one stock per day
	public String date;
	public String symbol;
	
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
	
	private StockHistory(String group, int session, String date, String symbol,
		double dividend, double dividendFee,
		double buyQuantity, double buyFee, double buy,
		double sellQuantity, double sellFee, double sell, double sellCost, double sellProfit,
		double totalQuantity, double totalCost, double totalValue, double totalDividend, double totalProfit) {
		this.group   = group;
		this.session = session;
		
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
	private StockHistory(int session, String date, String symbol) {
		this(symbol, session, date, symbol,
			0, 0,
			0, 0, 0,
			0, 0, 0, 0, 0,
			0, 0, 0, 0, 0);
	}
	public StockHistory() {
		this("", 0, "", "",
				0, 0,
				0, 0, 0,
				0, 0, 0, 0, 0,
				0, 0, 0, 0, 0);
	}
	
	@Override
	public int compareTo(StockHistory that) {
		int ret = this.group.compareTo(that.group);
		if (ret == 0) ret = this.session - that.session;
		if (ret == 0) ret = this.date.compareTo(that.date);
		return ret;
	}
	
	@Override
	public String toString() {
		return String.format("%-9s %4d %s %-9s %8.2f %8.2f   %8.2f %8.2f %8.2f   %8.2f %8.2f %8.2f %8.2f %8.2f   %8.2f %8.2f %8.2f   %8.2f %8.2f",
				group, session, date, symbol, dividend, dividendFee,
				buyQuantity, buyFee, buy,
				sellQuantity, sellFee, sell, sellCost, sellProfit,
				totalQuantity, totalCost, totalValue,
				totalDividend, totalProfit
				);
	}

	//                 key                  symbol      date
	private static Map<String, NavigableMap<String, StockHistory>> allStockMap = new TreeMap<>();
	public static List<StockHistory> getStockList() {
		List<StockHistory> ret = new ArrayList<>();
		allStockMap.values().stream().forEach(map -> ret.addAll(map.values()));		
		Collections.sort(ret);
		return ret;
	}
	private static NavigableMap<String, StockHistory> getStockMap(String symbol) {
		if (!allStockMap.containsKey(symbol)) {
			allStockMap.put(symbol, new TreeMap<>());
		}
		return allStockMap.get(symbol);
	}
	private static StockHistory getStock(String date, String symbol) {
		NavigableMap<String, StockHistory> stockMap = getStockMap(symbol);
		
		if (stockMap.containsKey(date)) {
			// Entry is already exists. use the entry.
		} else {
			Map.Entry<String, StockHistory>entry = stockMap.lowerEntry(date);
			StockHistory stock;
			if (entry == null) {
				stock = new StockHistory(nextSession++, date, symbol);
			} else {
				StockHistory lastStock = entry.getValue();
				// Use session in lastStock
				stock = new StockHistory(lastStock.session, date, symbol);
				
				// Copy totalXXX from lastStcok
				stock.totalQuantity = lastStock.totalQuantity;
				stock.totalCost     = lastStock.totalCost;
				stock.totalValue    = lastStock.totalValue;
				
				stock.totalDividend = lastStock.totalDividend;
				stock.totalProfit   = lastStock.totalProfit;
			}
			stockMap.put(date, stock);
		}
		
		StockHistory ret = stockMap.get(date);
		return ret;
	}
	public static List<String> getSymbolList() {
		List<String> ret = allStockMap.keySet().stream().collect(Collectors.toList());
		Collections.sort(ret);
		return ret;
	}
	public static List<StockHistory> getStockList(String symbol) {
		if (!allStockMap.containsKey(symbol)) {
			logger.error("No such symbol  {}", symbol);
			throw new UnexpectedException("No such stock");
		}
		NavigableMap<String, StockHistory> stockMap = allStockMap.get(symbol);
		List<StockHistory> ret = stockMap.values().stream().collect(Collectors.toList());
		Collections.sort(ret);
		return ret;
	}
	
	public static void dividend(String date, String symbol, double dividend, double dividendFee) {
		StockHistory stock = getStock(date, symbol);
		
		stock.dividend      = DoubleUtil.roundPrice(stock.dividend      + dividend);
		stock.dividendFee   = DoubleUtil.roundPrice(stock.dividendFee   + dividendFee);
		stock.totalDividend = DoubleUtil.roundPrice(stock.totalDividend + dividend - dividendFee);
	}

	public static void buy(String date, String symbol, double buyQuantity, double buy, double buyFee) {
//		logger.info("{}", String.format("buyQuantity  = %8.2f  buy  = %8.2f  buyFee  = %8.2f", buyQuantity, buy, buyFee));
		StockHistory stock = getStock(date, symbol);
		
		// If this is first buy for the stock and used before
		if (stock.totalQuantity == 0 && (stock.totalDividend != 0 || stock.totalProfit != 0)) {
			// Change session number
			stock.session       = nextSession++;
			
			// clear totalXXX
			stock.totalQuantity = 0;
			stock.totalCost     = 0;
			stock.totalValue    = 0;
					
			stock.totalDividend = 0;
			stock.totalProfit   = 0;
		}
		
		stock.buyQuantity = DoubleUtil.roundQuantity(stock.buyQuantity + buyQuantity);
		stock.buyFee      = DoubleUtil.roundPrice(stock.buyFee + buyFee);
		stock.buy         = DoubleUtil.roundPrice(stock.buy    + buy);
		
		stock.totalQuantity = DoubleUtil.roundQuantity(stock.totalQuantity + buyQuantity);
		stock.totalCost     = DoubleUtil.roundPrice(stock.totalCost + buy + buyFee);
	}
	
	public static void sell(String date, String symbol, double sellQuantity, double sell, double sellFee) {
//		logger.info("{}", String.format("sellQuantity = %8.2f  sell = %8.2f  sellFee = %8.2f", sellQuantity, sell, sellFee));
		StockHistory stock = getStock(date, symbol);
		
		double sellCost   = DoubleUtil.roundPrice((stock.totalCost / stock.totalQuantity) * sellQuantity);
		double sellProfit = DoubleUtil.roundPrice(sell - sellFee - sellCost);
//		logger.info("{}", String.format("sellCost = %8.2f  sellProfit = %8.2f", sellCost, sellProfit));
		
		stock.sellQuantity = DoubleUtil.roundQuantity(stock.sellQuantity + sellQuantity);
		stock.sellFee      = DoubleUtil.roundPrice(stock.sellFee    + sellFee);
		stock.sell         = DoubleUtil.roundPrice(stock.sell       + sell);
		
		stock.sellCost     = DoubleUtil.roundPrice(stock.sellCost   + sellCost);
		stock.sellProfit   = DoubleUtil.roundPrice(stock.sellProfit + sellProfit);
		
		stock.totalQuantity = DoubleUtil.roundQuantity(stock.totalQuantity - sellQuantity);
		stock.totalCost     = DoubleUtil.roundPrice(stock.totalCost - sellCost);
		
		stock.totalProfit = DoubleUtil.roundPrice(stock.totalProfit   + sellProfit);
		
		if (DoubleUtil.isAlmostZero(stock.totalQuantity)) {
			stock.totalQuantity = 0;
		}
	}

	public static void change(String date, String symbol, double quantity, String newSymbol, double newQuantity) {
		// Sanity check
		if (!allStockMap.containsKey(symbol)) {
			logger.error("No such symbol  {} {}", date, symbol);
			throw new UnexpectedException("No such symbol");
		}
		if ((!symbol.equals(newSymbol)) && allStockMap.containsKey(newSymbol)) {
			logger.error("Duplicate symbol  {}", newSymbol);
			throw new UnexpectedException("Duplicate symbol");
		}
		
		NavigableMap<String, StockHistory> stockMap = allStockMap.get(symbol);
		if (stockMap.containsKey(date)) {
			logger.error("Already entry exists.  {}  {}", date, symbol);
			throw new UnexpectedException("Already entry exists");
		}
		// Change group of stock in existing map
		for(StockHistory stock: stockMap.values()) {
			stock.group = newSymbol;
		}
		
		// Remove symbol in allStockMap
		allStockMap.remove(symbol);
		// Add stockMap with newSymbol in allStockMap
		allStockMap.put(newSymbol, stockMap);

		// Add entry for change of quantity
		StockHistory stock = getStock(date, newSymbol);
		stock.totalQuantity = newQuantity;
		stockMap.put(date, stock);
	}

	public static void updateTotalValue(String date, String symbol, double price) {
		NavigableMap<String, StockHistory> stockMap = allStockMap.get(symbol);
		if (stockMap.containsKey(date)) {
			// Entry is already exists. use the entry.
		} else {
			StockHistory stock = getStock(date, symbol);
			stockMap.put(date, stock);
		}
		StockHistory stock = stockMap.get(date);
		stock.totalValue = DoubleUtil.roundPrice(stock.totalQuantity * price);
	}
}
