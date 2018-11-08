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
import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.DoubleUtil;


@Sheet.SheetName("履歴")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class StockHistory extends Sheet implements Comparable<StockHistory> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockHistory.class);

	@ColumnName("グループ")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String group;
	@ColumnName("番号")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER_BLANK)
	public int    session;

	// One record for one stock per day
	@ColumnName("年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	@ColumnName("銘柄コード")
	public String symbol;
	
	// Dividend detail
	@ColumnName("配当")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double dividend;
	@ColumnName("配当手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double dividendFee;
	
	// Buy detail
	@ColumnName("購入数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER_BLANK)
	public double buyQuantity;
	@ColumnName("購入手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double buyFee;
	@ColumnName("購入額")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double buy;
	
	// Sell detail
	@ColumnName("売却数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER_BLANK)
	public double sellQuantity;
	@ColumnName("売却手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sellFee;
	@ColumnName("売却額")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sell;
	@ColumnName("売却費用")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sellCost;
	@ColumnName("売却益")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double sellProfit;
	
	// Value of the date
	@ColumnName("合計数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER_BLANK)
	public double totalQuantity;
	@ColumnName("合計費用")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double totalCost;
	@ColumnName("合計手数料")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double totalFee;
	
	@ColumnName("合計配当")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double totalDividend; // from dividend
	@ColumnName("合計利益")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public double totalProfit;   // from buy and sell
	
	private StockHistory(String group, int session, String date, String symbol,
		double dividend, double dividendFee,
		double buyQuantity, double buyFee, double buy,
		double sellQuantity, double sellFee, double sell, double sellCost, double sellProfit,
		double totalQuantity, double totalCost, double totalFee, double totalDividend, double totalProfit) {
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
		this.totalFee      = totalFee; // unrealized gain = totalValue - totalCost 
		
		// Realized gain
		this.totalDividend = totalDividend;
		this.totalProfit   = totalProfit;
	}
	public StockHistory(int session, String date, String symbol) {
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
			totalQuantity, totalCost, totalFee,
			totalDividend, totalProfit
			);
	}
	
	public static class Builder {
		private int nextSession = 1;
		
		//          symbol               date
		private Map<String, NavigableMap<String, StockHistory>> allStockMap = new TreeMap<>();
		
		public List<StockHistory> getStockList() {
			List<StockHistory> ret = new ArrayList<>();
			allStockMap.values().stream().forEach(map -> ret.addAll(map.values()));		
			Collections.sort(ret);
			return ret;
		}
		private NavigableMap<String, StockHistory> getStockMap(String symbol) {
			if (!allStockMap.containsKey(symbol)) {
				allStockMap.put(symbol, new TreeMap<>());
			}
			return allStockMap.get(symbol);
		}
		private StockHistory getStock(String date, String symbol) {
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
					stock.totalFee      = lastStock.totalFee;
					
					stock.totalDividend = lastStock.totalDividend;
					stock.totalProfit   = lastStock.totalProfit;
				}
				stockMap.put(date, stock);
			}
			
			StockHistory ret = stockMap.get(date);
			return ret;
		}
		public List<String> getSymbolList() {
			List<String> ret = allStockMap.keySet().stream().collect(Collectors.toList());
			Collections.sort(ret);
			return ret;
		}
		public List<StockHistory> getStockList(String symbol) {
			if (!allStockMap.containsKey(symbol)) {
				logger.error("No such symbol  {}", symbol);
				throw new UnexpectedException("No such stock");
			}
			NavigableMap<String, StockHistory> stockMap = allStockMap.get(symbol);
			List<StockHistory> ret = stockMap.values().stream().collect(Collectors.toList());
			Collections.sort(ret);
			return ret;
		}
		
		public void dividend(String date, String symbol, double debit, double credit) {
			StockHistory stock = getStock(date, symbol);
			
			double amount = DoubleUtil.roundPrice(credit - debit);
			
			stock.dividend      = DoubleUtil.roundPrice(stock.dividend      + amount);
			stock.dividendFee   = DoubleUtil.roundPrice(stock.dividendFee   + debit);
			stock.totalDividend = DoubleUtil.roundPrice(stock.totalDividend + amount);
			
			stock.totalFee      = DoubleUtil.roundPrice(stock.totalFee + debit);
		}

		public void buy(String date, String symbol, double buyQuantity, double fee, double debit) {
//			logger.info("{}", String.format("buyQuantity  = %8.2f  buy  = %8.2f  buyFee  = %8.2f", buyQuantity, buy, buyFee));
			StockHistory stock = getStock(date, symbol);
			
			// If this is first buy for the stock and used before
			if (stock.totalQuantity == 0 && (stock.totalDividend != 0 || stock.totalProfit != 0)) {
				// Change session number
				stock.session       = nextSession++;
				
				// clear totalXXX
				stock.totalQuantity = 0;
				stock.totalCost     = 0;
				stock.totalFee      = 0;
						
				stock.totalDividend = 0;
				stock.totalProfit   = 0;
			}
			
			stock.buyQuantity = DoubleUtil.roundQuantity(stock.buyQuantity + buyQuantity);
			stock.buyFee      = DoubleUtil.roundPrice(stock.buyFee + fee);
			stock.buy         = DoubleUtil.roundPrice(stock.buy    + debit);
			
			stock.totalQuantity = DoubleUtil.roundQuantity(stock.totalQuantity + buyQuantity);
			stock.totalCost     = DoubleUtil.roundPrice(stock.totalCost + debit);
			stock.totalFee      = DoubleUtil.roundPrice(stock.totalFee + fee);
		}
		
		public void sell(String date, String symbol, double sellQuantity, double fee, double credit) {
//			logger.info("{}", String.format("sellQuantity = %8.2f  sell = %8.2f  sellFee = %8.2f", sellQuantity, sell, sellFee));
			StockHistory stock = getStock(date, symbol);
			
			double sellCost   = DoubleUtil.roundPrice((stock.totalCost / stock.totalQuantity) * sellQuantity);
			double sellProfit = DoubleUtil.roundPrice(credit - sellCost);
//			logger.info("{}", String.format("sellCost = %8.2f  sellProfit = %8.2f", sellCost, sellProfit));
			
			stock.sellQuantity = DoubleUtil.roundQuantity(stock.sellQuantity + sellQuantity);
			stock.sellFee      = DoubleUtil.roundPrice(stock.sellFee    + fee);
			stock.sell         = DoubleUtil.roundPrice(stock.sell       + credit);
			
			stock.sellCost     = DoubleUtil.roundPrice(stock.sellCost   + sellCost);
			stock.sellProfit   = DoubleUtil.roundPrice(stock.sellProfit + sellProfit);
			
			stock.totalQuantity = DoubleUtil.roundQuantity(stock.totalQuantity - sellQuantity);
			stock.totalCost     = DoubleUtil.roundPrice(stock.totalCost - sellCost);
			stock.totalFee      = DoubleUtil.roundPrice(stock.totalFee + fee);
			
			stock.totalProfit = DoubleUtil.roundPrice(stock.totalProfit   + sellProfit);
			
			if (DoubleUtil.isAlmostZero(stock.totalQuantity)) {
				stock.totalQuantity = 0;
			}
		}

		public void change(String date, String symbol, double quantity, String newSymbol, double newQuantity) {
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
	}
}
