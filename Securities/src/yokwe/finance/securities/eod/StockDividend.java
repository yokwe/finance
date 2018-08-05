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
		
		double    quantity;
		LocalDate periodStart; // inclusive
		LocalDate periodStop;  // inclusive
		
		Period(String symbol, double quantity, LocalDate periodStart, LocalDate periodStop) {
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
		
		boolean contains(LocalDate date) {
			return date.equals(periodStart) || date.equals(periodStop) || (date.isAfter(periodStart) && date.isBefore(periodStop));
		}
	}
	
	static class Holding {
		static final LocalDate DATE_FOREVER = LocalDate.of(9999, 9, 9);
		//  symbol
		Map<String, List<Period>> map = new TreeMap<>();
		
		Holding(Map<String, List<StockHistory>> stockHistoryMap) {
			for(Map.Entry<String, List<StockHistory>> entry: stockHistoryMap.entrySet()) {
				List<StockHistory> stockHistoryList = entry.getValue();
				Collections.sort(stockHistoryList);
				
				for(StockHistory stockHistory: stockHistoryList) {
					if (stockHistory.buyQuantity != 0) {
						// stockHistory.date is tradindDate
						LocalDate tradeDate = toLocalDate(stockHistory.date);
						LocalDate settlementDate = Market.toSettlementDate(tradeDate);
						
						buy(stockHistory.group, settlementDate, stockHistory.buyQuantity);
					}
					if (stockHistory.sellQuantity != 0) {
						LocalDate tradeDate = toLocalDate(stockHistory.date);
						LocalDate settlementDate = Market.toSettlementDate(tradeDate);

						sell(stockHistory.group, settlementDate, stockHistory.sellQuantity);
					}
				}
			}

		}
		
		private void buy(String symbol, LocalDate date, double quantity) {
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
				LocalDate prevDate = date.minusDays(1);

				Period lastPeriod = periodList.get(periodList.size() - 1);
				Period nextPeriod = new Period(lastPeriod);
				
				lastPeriod.periodStop = prevDate;
				
				double newQuantity = Transaction.roundQuantity(lastPeriod.quantity + quantity);
				nextPeriod.quantity    = newQuantity;
				nextPeriod.periodStart = date;
				
				periodList.add(nextPeriod);
			}
		}
		private void sell(String symbol, LocalDate date, double quantity) {
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
				LocalDate prevDate = date.minusDays(1);

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
				
				nextPeriod.quantity    = newQuantity;
				nextPeriod.periodStart = date;
				
				periodList.add(nextPeriod);
			}
		}
		public double quantity(String symbol, LocalDate date) {
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
		public int size() {
			return map.size();
		}
		public List<String> getSymbolList() {
			List<String> symbolList = new ArrayList<>(map.keySet());
			Collections.sort(symbolList);
			return symbolList;
		}
		public List<Period> getPeriod(String symbol) {
			if (!map.containsKey(symbol)) {
				logger.error("Unknown symbol {}", symbol);
				throw new SecuritiesException("Unknown symbol");
			}
			return map.get(symbol);
		}
		public boolean containsYear(String symbol, int year) {
			List<Period> periodList = getPeriod(symbol);
			for(Period period: periodList) {
				if (period.quantity == 0) continue;
				if (period.periodStart.getYear() == year) return true;
				if (period.periodStop.getYear() == year) return true;
			}
			return false;
		}
	}
	
	static class Dividend {
		LocalDate recordDate;
		LocalDate paymentDate;
		double    amount;
		
		Dividend(Dividends dividends) {
			this.recordDate  = toLocalDate(dividends.recordDate);
			this.paymentDate = toLocalDate(dividends.paymentDate);
			this.amount      = dividends.amount;
		}
		
		@Override
		public String toString() {
			return String.format("{%s %s %s}", recordDate, paymentDate, Double.toString(amount));
		}
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof Dividend) {
				Dividend that = (Dividend)o;
				return this.recordDate.equals(((Dividend) o).recordDate) && this.paymentDate.equals(that.paymentDate) && DoubleUtil.isAlmostEqual(this.amount, that.amount);
			} else {
				return false;
			}
		}
	}
	
	static Map<String, List<Dividend>> buildDividendMap(List<String> symbolList) {
		Map<String, List<Dividend>> dividmentMap = new TreeMap<>();
		for(String symbol: symbolList) {
			Map<String, Dividends[]> map = Dividends.getStock(Range.Y1, symbol);
			if (map.containsKey(symbol)) {
				Dividends[] divedendsArray = map.get(symbol);
				if (divedendsArray == null) {
					logger.error("Unexpected {}", symbol);
					throw new SecuritiesException("Unexpected");
				}
				List<Dividend> dividendList = new ArrayList<>();
				
				Dividend lastDividend = null;
				for(Dividends divedends: divedendsArray) {					
					if (DoubleUtil.isAlmostZero(divedends.amount)) {
						logger.warn("{} zero amount       {}", String.format("%-8s", symbol), divedends);
						continue;
					}
					if (divedends.recordDate.length() == 0) {
						logger.warn("{} empty recordDate  {}", String.format("%-8s", symbol), divedends);
						continue;
					}
					if (divedends.paymentDate.length() == 0) {
						logger.warn("{} empty paymentDate {}", String.format("%-8s", symbol), divedends);
						continue;
					}
					
					Dividend dividend = new Dividend(divedends);
					if (lastDividend != null) {
						if (dividend.equals(lastDividend)) {
							logger.warn("{} skip same         {}", String.format("%-8s", symbol), divedends);
							continue;
						}
					}
					dividendList.add(dividend);
					lastDividend = dividend;
				}
//				logger.info("{}", String.format("%-8s (%d)%s", symbol, dividendList.size(), dividendList));
				if (!dividendList.isEmpty()) {
					dividmentMap.put(symbol, dividendList);
				}
			} else {
				logger.warn("{} not found", String.format("%-8s", symbol));
			}
		}
		return dividmentMap;
	}
	
	public static void main(String[] args) {
		logger.info("START");

		int year = LocalDate.now().getYear();
		logger.info("year {}", year);
		
		// Cannot use Stock HistoryMap. Because HistoryMap is summarized.
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();
		logger.info("stockHistoryMap {}", stockHistoryMap.size());		
		
		// Build holding and symboList
		List<String> symbolList = new ArrayList<>();
		Holding holding = new Holding(stockHistoryMap);
		for(String symbol: holding.getSymbolList()) {
			if (holding.containsYear(symbol, year)) {
				symbolList.add(symbol);
				logger.info("Period {}", holding.getPeriod(symbol));
			} else {
				logger.info("skip   {}", symbol);
			}
		}
		logger.info("holding    {}", holding.size());
		logger.info("symbolList {}", symbolList.size());
		
		// Build dividendMap with symbolList
		Map<String, List<Dividend>> dividendMap = buildDividendMap(symbolList);
		for(Map.Entry<String, List<Dividend>> entry: dividendMap.entrySet()) {
			logger.info("Dividend {} ({}) {}", String.format("%-8s", entry.getKey()), entry.getValue().size(), entry.getValue());
		}

		logger.info("STOP");
	}

}
