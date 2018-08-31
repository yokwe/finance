package yokwe.finance.securities.eod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.StockDividend.PayDiv;
import yokwe.finance.securities.iex.Dividends;
import yokwe.finance.securities.iex.IEXBase.Range;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateStockDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStockDividend.class);
	
	private static LocalDate toLocalDate(String date) {
		LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
		return localDate;
	}

	private static class Period {
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
	
	private static class Holding {
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
				
				double newQuantity = DoubleUtil.roundQuantity(lastPeriod.quantity + quantity);
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
				
				double newQuantity = DoubleUtil.roundQuantity(lastPeriod.quantity - quantity);
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
				if (period.periodStart.getYear() <= year && year <= period.periodStop.getYear()) return true;
			}
			return false;
		}
	}
	
	private static class Dividend implements Comparable<Dividend> {
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

		@Override
		public int compareTo(Dividend that) {
			int ret = this.recordDate.getMonthValue() - that.recordDate.getMonthValue();
			if (ret == 0) ret = this.recordDate.getDayOfMonth() - that.recordDate.getDayOfMonth();
			return ret;
		}
	}
	
	private static Map<String, List<Dividend>> buildDividendMap(List<String> symbolList) {
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
				logger.info("{}", String.format("%-8s (%d)%s", symbol, dividendList.size(), dividendList));
				if (!dividendList.isEmpty()) {
					Collections.sort(dividendList);
					dividmentMap.put(symbol, dividendList);
				}
			} else {
				logger.warn("{} not found", String.format("%-8s", symbol));
			}
		}
		return dividmentMap;
	}
	
	public static List<StockDividend> getStockDividendList() {
		LocalDate today = LocalDate.now();
		logger.info("today {}", today);
		int todayYear  = today.getYear();
		int todayMonth = today.getMonthValue();
		int todayDay   = today.getDayOfMonth();
		int todayMMDD  = todayMonth * 100 + todayDay;
		
		Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();
		logger.info("stockHistoryMap {}", stockHistoryMap.size());		
		
		// Build holding and symboList
		List<String> symbolList = new ArrayList<>();
		Holding holding = new Holding(stockHistoryMap);
		for(String symbol: holding.getSymbolList()) {
			if (holding.containsYear(symbol, todayYear)) {
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

		// Build StockDividendList
		List<StockDividend> stockDividendList = new ArrayList<>();
		
		for(String symbol: symbolList) {
			List<Dividend> dividendList;
			if (dividendMap.containsKey(symbol)) {
				dividendList = dividendMap.get(symbol);
			} else {
				dividendList = new ArrayList<>();
			}
			
			// Remove Dividend of last year if same month of this year exists.
			{				
				Map<Integer, List<Dividend>> monMap = new TreeMap<>();
				
				for(Dividend dividend: dividendList) {
					LocalDate paymentDate = dividend.paymentDate;
					int mon = paymentDate.getMonthValue();
					
					if (!monMap.containsKey(mon)) {
						monMap.put(mon, new ArrayList<>());
					}
					monMap.get(mon).add(dividend);
				}
				
				List<Dividend> newDividendList = new ArrayList<>();
				for(List<Dividend> divList: monMap.values()) {
					List<Dividend> thisYear = divList.stream().filter(o -> o.paymentDate.getYear() == todayYear).collect(Collectors.toList());
					List<Dividend> lastYear = divList.stream().filter(o -> o.paymentDate.getYear() != todayYear).collect(Collectors.toList());
					
					if ((!thisYear.isEmpty()) && (!lastYear.isEmpty())) {
						// Discard same month of last year
						newDividendList.addAll(thisYear);
					} else if (!thisYear.isEmpty()) {
						newDividendList.addAll(thisYear);
					} else if (!lastYear.isEmpty()) {
						newDividendList.addAll(lastYear);
					}
				}
				Collections.sort(newDividendList);
				dividendList = newDividendList;
			}
			
			double totalQuantity;
			double totalCost;
			{
				List<StockHistory> stockHistory = stockHistoryMap.get(symbol);
				StockHistory lastStockHistory = stockHistory.get(stockHistory.size() - 1);
				totalQuantity = lastStockHistory.totalQuantity;
				totalCost     = lastStockHistory.totalCost;
				
				// There are 2 days difference between trade date and settlement date.
				// So there can be a chance that stock was bought but not reach to settlement date as of today.
				
				// Sanity check
//				double quantity = holding.quantity(symbol, today);
//				if (!DoubleUtil.isAlmostEqual(totalQuantity, quantity)) {
//					logger.error("Unexpected {}", symbol);
//					logger.error("  totalQuantity {}", Double.toString(totalQuantity));
//					logger.error("  quantity      {}", Double.toString(quantity));
//					throw new SecuritiesException("Unexpected");
//				}
			}
			
			// Find out dividend for each month
			Map<Integer, List<PayDiv>> payDivMap = new TreeMap<>();
			{
				for(int month = 1; month <= 12; month++) {
					List<PayDiv> payDivList = new ArrayList<>();
					
					for(Dividend dividend: dividendList) {
						// Use paymentDate to find month value
						if (dividend.paymentDate.getMonthValue() == month) {
							// Use recordDate to calculate div
							LocalDate recordDate = dividend.recordDate.withYear(todayYear);
							int recMonth = recordDate.getMonthValue();
							int recDay   = recordDate.getDayOfMonth();
							int recMMDD  = recMonth * 100 + recDay;
							
							double quantity = holding.quantity(symbol, (recMMDD <= todayMMDD) ? recordDate : today);
							if (quantity == 0) continue;

							double div = DoubleUtil.roundPrice(quantity * dividend.amount);
							
							LocalDate paymentDate = dividend.paymentDate.withYear(todayYear);
							
							PayDiv payDiv = new PayDiv(paymentDate, quantity, dividend.amount, div);
							payDivList.add(payDiv);
						}
					}
					if (!payDivList.isEmpty()) {
						payDivMap.put(month, payDivList);
					}
				}
			}
			
			double totalDiv = 0;
			for(List<PayDiv> payDivList: payDivMap.values()) {
				totalDiv = DoubleUtil.roundPrice(totalDiv + payDivList.stream().mapToDouble(o -> o.div).sum());
			}
			
			Map<Integer, PayDiv> simplePayDivMap = new TreeMap<>();
			{
				for(Map.Entry<Integer, List<PayDiv>> entry: payDivMap.entrySet()) {
					Integer key = entry.getKey();
					List<PayDiv> value = entry.getValue();
					
					PayDiv payDiv = value.get(value.size() - 1);
					payDiv.div = value.stream().mapToDouble(o -> o.div).sum();
					simplePayDivMap.put(key, payDiv);
				}
			}

			StockDividend stockDividend = new StockDividend(symbol, totalQuantity, totalCost, totalDiv, simplePayDivMap);
			
			stockDividendList.add(stockDividend);
		}
		
		return stockDividendList;
	}
	
	public static void main(String[] args) {
		logger.info("START");

		List<StockDividend> stockDividendList = getStockDividendList();
		for(StockDividend stockDividend: stockDividendList) {
			logger.info("{}", stockDividend);
		}

		logger.info("STOP");
	}
}
