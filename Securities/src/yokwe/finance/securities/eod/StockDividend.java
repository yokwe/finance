package yokwe.finance.securities.eod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.iex.Dividends;
import yokwe.finance.securities.iex.IEXBase.Range;
import yokwe.finance.securities.util.DoubleUtil;

public class StockDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockDividend.class);
	
	static LocalDate toLocalDate(String date) {
		LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
		return localDate;
	}

	static class Period {
		String symbol;
		
		double quantity;
		String periodStart;
		String periodStop;
		
		Period(String symbol, double quantity, String periodStart, String periodStop) {
			this.symbol      = symbol;
			this.quantity    = quantity;
			this.periodStart = periodStart;
			this.periodStop  = periodStop;
		}
		Period(Period that) {
			this.symbol      = that.symbol;
			this.quantity    = that.quantity;
			this.periodStart = that.periodStart;
			this.periodStop  = that.periodStop;
		}
		
		@Override
		public String toString() {
			return String.format("{%-6s  %6.2f %s %s}", symbol, quantity, periodStart, periodStop);
		}
		
		boolean contains(String date) {
			return 0 <= periodStart.compareTo(date) && 0 <= date.compareTo(periodStop);
		}
	}
	
	static class Holding {
		static final String DATE_FOREVER = "9999-12-31";
		//         symbol
		static Map<String, List<Period>> map = new TreeMap<>();
		
		static void buy(String symbol, String date, double quantity) {
			logger.info("buy  {}", String.format("%-8s  %s %6.2f", symbol, date, quantity));

			if (!map.containsKey(symbol)) {
				map.put(symbol, new ArrayList<>());
			}
			
			List<Period> periodList = map.get(symbol);
			if (periodList.isEmpty()) {
				// Special for first entry
				Period period = new Period(symbol, quantity, date, DATE_FOREVER);
				periodList.add(period);
			} else {
				LocalDate localDate = toLocalDate(date);
				String prevDate = localDate.minusDays(1).toString();

				Period lastPeriod = periodList.get(periodList.size() - 1);
				Period nextPeriod = new Period(lastPeriod);
				
				lastPeriod.periodStop = prevDate;
				
				double newQuantity = Transaction.roundQuantity(lastPeriod.quantity + quantity);
				nextPeriod.quantity    = newQuantity;
				nextPeriod.periodStart = date;
				
				periodList.add(nextPeriod);
			}
		}
		static void sell(String symbol, String date, double quantity) {
			logger.info("sell {}", String.format("%-8s  %s %6.2f", symbol, date, quantity));
			
			if (!map.containsKey(symbol)) {
				logger.error("No symbol in map {} {} {}", symbol, date, quantity);
				throw new SecuritiesException("No symbol in map");
			}
			
			List<Period> periodList = map.get(symbol);
			if (periodList.isEmpty()) {
				logger.error("Unexpected {} {} {}", symbol, date, quantity);
				throw new SecuritiesException("Unexpected");
			} else {
				LocalDate localDate = toLocalDate(date);
				String prevDate = localDate.minusDays(1).toString();

				Period lastPeriod = periodList.get(periodList.size() - 1);
				Period nextPeriod = new Period(lastPeriod);
				
				// Sanity check
				if (lastPeriod.quantity == 0) {
					logger.error("Unexpected {} {} {}", symbol, date, quantity);
					throw new SecuritiesException("Unexpected");
				}
				
				lastPeriod.periodStop = prevDate;
				
				double newQuantity = Transaction.roundQuantity(lastPeriod.quantity - quantity);
				if (DoubleUtil.isAlmostZero(newQuantity)) newQuantity = 0;
				
				// Sanity check
				if (newQuantity < 0) {
					logger.error("Unexpected {} {} {}", symbol, date, quantity);
					throw new SecuritiesException("Unexpected");
				}
				
				nextPeriod.quantity = newQuantity;
				nextPeriod.periodStart = date;
				
				periodList.add(nextPeriod);
			}
		}
		static double quantity(String symbol, String date) {
			if (!map.containsKey(symbol)) {
				logger.error("No symbol in map {} {}", symbol, date);
				throw new SecuritiesException("No symbol in map");
			}
			
			List<Period> periodList = map.get(symbol);
			if (periodList.isEmpty()) {
				logger.error("Unexpected {} {}", symbol, date);
				throw new SecuritiesException("Unexpected");
			} else {
				for(Period period: periodList) {
					if (period.contains(date)) {
						return period.quantity;
					}
				}
				return 0;
			}
		}
		static List<String> getSymbolList() {
			List<String> symbolList = new ArrayList<>(map.keySet());
			Collections.sort(symbolList);
			return symbolList;
		}
		static List<Period> getPeriod(String symbol) {
			return map.get(symbol);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");

		String targetYear = Integer.toString(LocalDate.now().getYear());
		String targetStart = String.format("%s-01-01", targetYear);
		String targetStop  = String.format("%s-12-31", targetYear);
		logger.info("target {}  {} - {}", targetStart, targetStop);
		
		// Cannot use Stock HistoryMap. Because HistoryMap is summarized.
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();
		logger.info("stockHistoryMap {}", stockHistoryMap.size());		
		
//		List<HoldingStock> holdingStockList = new ArrayList<>();
		
		for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
			List<StockHistory> stockHistoryList = entry.getValue();
			Collections.sort(stockHistoryList);
			
			for(StockHistory stockHistory: stockHistoryList) {
				if (stockHistory.buyQuantity != 0) {
					// stockHistory.date is tradindDate
					LocalDate tradeDate = toLocalDate(stockHistory.date);
					LocalDate settlementDate = Market.toSettlementDate(tradeDate);
					
					Holding.buy(stockHistory.group, settlementDate.toString(), stockHistory.buyQuantity);
				}
				if (stockHistory.sellQuantity != 0) {
					LocalDate tradeDate = toLocalDate(stockHistory.date);
					LocalDate settlementDate = Market.toSettlementDate(tradeDate);

					Holding.sell(stockHistory.group, settlementDate.toString(), stockHistory.sellQuantity);
				}
			}
		}
		for(String symbol: Holding.getSymbolList()) {
			List<Period> periodList = Holding.getPeriod(symbol);
			logger.info("Period {}", periodList);
		}
		
		Map<String, List<Dividends>> dividendsMap = new TreeMap<>();
		for(String symbol: Holding.getSymbolList()) {
			Map<String, Dividends[]> map = Dividends.getStock(Range.Y2, symbol);
			if (map.containsKey(symbol)) {
				Dividends[] divedendsArray = map.get(symbol);
				if (divedendsArray == null) {
					logger.error("Unexpected {}", symbol);
					throw new SecuritiesException("Unexpected");
				}
				List<Dividends> dividendsList = new ArrayList<>();
				for(Dividends divedends: divedendsArray) {					
					if (divedends.paymentDate.length() == 0) continue;
					
					String year = divedends.paymentDate.substring(0, 4);
					if (year.equals(targetYear)) {
						dividendsList.add(divedends);
					}
				}
				logger.info("{}", String.format("%-8s (%d)%s", symbol, dividendsList.size(), dividendsList));
				if (!dividendsList.isEmpty()) {
					dividendsMap.put(symbol, dividendsList);
				}
			} else {
				logger.warn("{} not found", String.format("%-8s", symbol));
			}
		}

		logger.info("STOP");
	}

}
