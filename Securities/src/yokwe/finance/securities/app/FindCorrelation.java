package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.util.Correlation;
import yokwe.finance.securities.util.DoubleStreamUtil.MovingAverage;
import yokwe.finance.securities.util.DoubleStreamUtil.Sample;

public final class FindCorrelation {
	private static final Logger logger = LoggerFactory.getLogger(FindCorrelation.class);

	private static final String JDBC_URL    = "jdbc:sqlite:tmp/sqlite/securities.sqlite3";
	private static final String OUTPUT_PATH = "tmp/database/correlaton-%d.csv";
	
//	private static final int MIN_SAMPLE_COUNT =  80;
//	private static final int MAX_SAMPLE_COUNT = 120;
	
	private static final int MIN_SAMPLE_COUNT = 400;
	private static final int MAX_SAMPLE_COUNT = 600;
	
//	private static final int MIN_SAMPLE_COUNT =  980;
//	private static final int MAX_SAMPLE_COUNT = 1020;
	
	private static final int DURATION_DAYS = 90;
	private static final int MIN_AVG_VOL   = 50_000;
	
	private static LocalDate getTradingDate(Connection connection, final LocalDate date) {
		LocalDate tradingDate = date.minusDays(0);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		tradingDate = tradingDate.minusDays(1);
		if (PriceTable.isTradingDay(connection, tradingDate)) return tradingDate;
		
		logger.error("date = {}  tradingDate = {}", date, tradingDate);
		throw new SecuritiesException("tradingDate");
	}
	
	static void calculate(final Connection connection, final int months) throws IOException {
		logger.info("month         = {}", months);

		// dateFrom and dateTo is range of days (both inclusive)
		LocalDate dateTo   = LocalDate.parse(PriceTable.getLastTradeDate(connection));;
		LocalDate dateFrom = dateTo.minusMonths(months).plusDays(1);
		if (!PriceTable.isTradingDay(connection, dateFrom)) {
			dateFrom = getTradingDate(connection, dateFrom);
		}
//		logger.info("dateRange     = {} - {}", dateFrom, dateTo);		
		
		List<PriceTable> dataListTo   = PriceTable.getAllByDate(connection, dateTo);
		List<PriceTable> dataListFrom = PriceTable.getAllByDate(connection, dateFrom);
//		logger.info("dataListTo    = {}", dataListTo.size());
//		logger.info("dataListFrom  = {}", dataListFrom.size());
		
		// symbolList has all symbols
		List<String> symbolList;
		{
			List<String> symbolFrom = dataListFrom.stream().map(o -> o.symbol).collect(Collectors.toList());
			List<String> symbolTo   = dataListTo.stream().map(o -> o.symbol).collect(Collectors.toList());
			logger.info("symbolFrom    {} {}", dateFrom, symbolFrom.size());
			logger.info("symbolTo      {} {}", dateTo,   symbolTo.size());
			Set<String> set = new HashSet<>(symbolFrom);
			set.retainAll(symbolTo);
			symbolList = new ArrayList<>(set);
			Collections.sort(symbolList);
		}
//		logger.info("symbolList    = {}", symbolList.size());
		
		// filter symbol
		{
			List<String> newSymbolList = new ArrayList<>(symbolList.size());
			for(PriceTable data: dataListTo) {
				if (data.close < 5.0)                  continue;				
				if (!symbolList.contains(data.symbol)) continue;
				newSymbolList.add(data.symbol);
			}
			
			symbolList = newSymbolList;
		}
//		logger.info("symbolList    = {}", symbolList.size());
		
		{
//			logger.info("Filter by average volume", symbolList.size());
			LocalDate dateFromFilter = dateTo.minusDays(DURATION_DAYS);
			List<String> newSymbolList = new ArrayList<>();

			for(String symbol: symbolList) {
				double averageVolume = PriceTable.getAverageVolume(connection, symbol, dateFromFilter, dateTo);
				if (averageVolume < MIN_AVG_VOL) continue;
				
				newSymbolList.add(symbol);
			}
			symbolList = newSymbolList;
		}
//		logger.info("symbolList    = {}", symbolList.size());
		
		List<String> dateList = PriceTable.getAllBySymbolDateRange(connection, "IBM", dateFrom, dateTo).stream().map(o -> o.date).collect(Collectors.toList());
//		logger.info("dateList      = {}", dateList.size());
		final int totalCount = dateList.size();
		int sample = -1;
		int skip   = totalCount;
		{
			for(int i = MAX_SAMPLE_COUNT; MIN_SAMPLE_COUNT <= i; i--) {
				final int tempSample = totalCount / i;
				if (tempSample == 0) continue;
				
				final int tempCount  = totalCount / tempSample;
				if (tempCount < MIN_SAMPLE_COUNT || MAX_SAMPLE_COUNT < tempCount) continue;
				
				final int tempSkip   = totalCount % tempSample;

				if (tempSkip == 0) {
					sample = tempSample;
					skip   = tempSkip;
					break;
				}
				if (tempSkip < skip) {
					sample  = tempSample;
					skip    = tempSkip;
				}
			}
			if (sample == -1) {
				sample = 1;
				skip   = 0;
			}
		}
		logger.info("totalCount    {}  =  {} (sample) *  {} (count)  +  {} (skip)", totalCount, sample, totalCount / sample, skip);
		
		Map<String, double[]> priceMap = new TreeMap<>();
		for(String symbol: symbolList) {
			List<PriceTable> list = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
			if (list.size() != dateList.size()) {
//				logger.warn("SKIP  symbol = {}  list = {}  dateList = {}", symbol, list.size(), dateList.size());
				continue;
			}
			double[] value = list.stream().mapToDouble(o -> o.close).skip(skip).map(MovingAverage.getInstance(sample)).flatMap(Sample.getInstance(sample)).toArray();
			priceMap.put(symbol, value);
		}
		
		{
//			logger.info("Correlation");
			Correlation correlation = new Correlation(priceMap);
			logger.info("correation   {} x {}", correlation.size, correlation.length);
			
			//  SPY  QQQ = 0.980560447
			logger.info("X SPY  QQQ = {}", String.format("%.9f", correlation.getCorrelationX("SPY", "QQQ")));
			logger.info("  SPY  QQQ = {}", String.format("%.9f", correlation.getCorrelation("SPY", "QQQ")));
			
			String csvPath = String.format(OUTPUT_PATH, months);
			logger.info("OUTPUT CSV START  {}", csvPath);
			try (BufferedWriter csv = new BufferedWriter(new FileWriter(csvPath))) {
				for(String symbolA: correlation.getNames()) {
					Map<String, Double> map = correlation.getCorrelation(symbolA);
					
					for(String symbolB: map.keySet()) {
						double v = map.get(symbolB);
						// symbolA, symbolB, correlation
						csv.append(String.format("%s,%s,%.2f\n", symbolA, symbolB, v));
					}
				}
			}
			logger.info("OUTPUT CSV STOP");
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		logger.info("DURATION_DAYS    = {}", DURATION_DAYS);
		logger.info("MIN_AVG_VOL      = {}", MIN_AVG_VOL);
		logger.info("MIN_SAMPLE_COUNT = {}", MIN_SAMPLE_COUNT);
		logger.info("MAX_SAMPLE_COUNT = {}", MAX_SAMPLE_COUNT);
		
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection(JDBC_URL)) {
				calculate(connection, 3);
				calculate(connection, 12);
				calculate(connection, 36);
				calculate(connection, 60);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
