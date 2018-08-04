package yokwe.finance.securities.eod;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.iex.Dividends;
import yokwe.finance.securities.iex.IEXBase.Range;

public class StockDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockDividend.class);

	public static class OneDividend {
		public int    month;
		public String exDiv;    // MM/dd
		public String pay;      // MM/dd
		public double amount;   // amount per stock
		public double dividend; // amount * quantity
	}
	
	static class HoldingStock {
		final String  group;
		final int     session;
		final String  symbol;
		final String  dateStart;
		final String  dateStop;
		final Dividends[] dividends;
		
		HoldingStock(String group, int session, String symbol, String dateStart, String dateStop, Dividends[] dividends) {
			this.group     = group;
			this.session   = session;
			this.symbol    = symbol;
			this.dateStart = dateStart;
			this.dateStop  = dateStop;
			this.dividends = dividends;
		}
		
		@Override
		public String toString() {
			return String.format("{%-8s %4d %-8s %s %s %2d}", group, session, symbol, dateStart, dateStop, dividends.length);
		}
	}
	
	public String symbol;
	public String name;
	public String date;
	public int    quantitity;
	public double buy;
	public double totalDividend;
	
	List<OneDividend> dividendList;
	
	
	public static void main(String[] args) {
		logger.info("START");

		String targetYear = Integer.toString(LocalDate.now().getYear());
		logger.info("targetYear {}", targetYear);
		
		// Cannot use Stock HistoryMap. Because HistoryMap is summarized.
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();
		logger.info("stockHistoryMap {}", stockHistoryMap.size());		
		
		List<HoldingStock> holdingStockList = new ArrayList<>();
		
		for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
			String symbol = entry.getKey();
			
			Map<String, Dividends[]> dividendsMap = Dividends.getStock(Range.Y2, symbol);
			// Skip if no dividends info or stock is delisted, skip this symbol
			if (!dividendsMap.containsKey(symbol)) {
				logger.warn("{} No dividends null", String.format("%-8s", symbol));
				continue;
			}
			
			Dividends[] dividends = dividendsMap.get(symbol);
//			logger.info("dividends {}", dividends.length);
			if (dividends.length == 0) {
				logger.warn("{} No dividends 0", String.format("%-8s", symbol));
				continue;
			}
			
			Map<Integer, List<StockHistory>> sessionStockHistoryMap = new TreeMap<>();
			for(StockHistory stockHistory: entry.getValue()) {
				Integer session = stockHistory.session;
				if (!sessionStockHistoryMap.containsKey(session)) {
					sessionStockHistoryMap.put(session, new ArrayList<>());
				}
				sessionStockHistoryMap.get(session).add(stockHistory);
			}
			for(Map.Entry<Integer, List<StockHistory>> sessionStockHistorEntry: sessionStockHistoryMap.entrySet()) {
//				Integer session = sessionStockHistorEntry.getKey();
				List<StockHistory> stockHistoryList = sessionStockHistorEntry.getValue();
				
				StockHistory firstStockHistory = stockHistoryList.get(0);
				int session = firstStockHistory.session;
				String group = firstStockHistory.group;
				int size = stockHistoryList.size();
				
				// dateStart
				String firstTradeDate = null;
				{
					// Find first sell and use it as firstTradeDate
					ListIterator<StockHistory> li = stockHistoryList.listIterator(0);
					while(li.hasNext()) {
						StockHistory stockHistory = li.next();
						if (stockHistory.buyQuantity != 0) {
							firstTradeDate = stockHistory.date;
							break;
						}
					}
					if (firstTradeDate == null) {
						// No buy record
						logger.warn("{} No buy record", String.format("%-8s", symbol));
						continue;
					}
				}
				
				// dateLast
				String lastTradeDate = null;
				{
					StockHistory lastStockHistory = stockHistoryList.get(size - 1);
					if (lastStockHistory.totalQuantity != 0) {
						lastTradeDate = "9999-12-31";
					} else {
						// Find last sell and use it as lastDate
						ListIterator<StockHistory> li = stockHistoryList.listIterator(stockHistoryList.size());
						while(li.hasPrevious()) {
							StockHistory stockHistory = li.previous();
							if (stockHistory.sellQuantity != 0) {
								lastTradeDate = stockHistory.date;
								break;
							}
						}
						if (lastTradeDate == null) {
							// No sell record
							logger.error("Unexpected {} {}", symbol, session);
							throw new SecuritiesException("Unexpected");
						}
					}
				}
				holdingStockList.add(new HoldingStock(group, session, symbol, firstTradeDate, lastTradeDate, dividends));
			}
		}
		
		for(HoldingStock holdingStock: holdingStockList) {
			logger.info("{}", holdingStock);
		}
		logger.info("holdingStokcList {}", holdingStockList.size());

		logger.info("STOP");
	}

}
