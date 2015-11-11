package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.util.DoubleStreamUtil.MovingAverage;
import yokwe.finance.securities.util.DoubleStreamUtil.Sample;

public final class FindCorrelation {
	private static final Logger logger = LoggerFactory.getLogger(FindCorrelation.class);

	private static final String JDBC_URL    = "jdbc:sqlite:tmp/sqlite/securities.sqlite3";
	private static final String OUTPUT_PATH = "tmp/correlaton.csv";
	
	static final class CorrelationMap {
		public  final int                   length;
		public  final int                   size;
		private final Map<String, double[]> dataMap;
		private final Map<String, Integer>  indexMap; // map from name to index
		private final String[]              names;    // all name in dataMap
		private final double[][]            ccArray;  // double[size][size]
		
		CorrelationMap(Map<String, double[]> doubleMap) {
			length   = doubleMap.get("IBM").length;
			size     = doubleMap.size();

			// Sanity check
			for(String name: doubleMap.keySet()) {
				double[] data = doubleMap.get(name);
				if (data.length != length) {
					logger.error("name = {}  length = {}  data.length = {}", name, length, data.length);
					throw new SecuritiesException("length");
				}
			}

			dataMap  = new HashMap<>(doubleMap);
			indexMap = new HashMap<>();
			names    = new String[size];
			ccArray  = new double[size][size];

			double[][] devArray = new double[size][length];
			double[]   sdArray  = new double[size];
			
			{
				int index = 0;
				for(String name: dataMap.keySet()) {
					names[index++] = name;
				}
				// Sort names before use.  dataMap can be HashMap
				Arrays.sort(names);
			}
			
			for(int index = 0; index < names.length; index++) {
				final String name = names[index];
				indexMap.put(name, index);
				
				final double[] data = dataMap.get(name);
				final double[] dev  = devArray[index];
				
				double mean = 0;
				for(double e: data) mean += e;
				mean /= length;
				
				double var = 0;
				for(int i = 0; i < length; i++) {
					double t = data[i] - mean;
					dev[i] = t;
					var += t * t;
				}
				sdArray[index] = 1.0 / Math.sqrt(var);
			}
			// Compute ccArray from devArray and sdArray
			for(int x = 0; x < size; x++) {
				for(int y = 0; y < x; y++) {
					double[] dataX = devArray[x];
					double[] dataY = devArray[y];
					double   sdX   = sdArray[x];
					double   sdY   = sdArray[y];
					
					double cc = 0;
					for(int i = 0; i < dataX.length; i++) cc += dataX[i] * dataY[i];
					cc *= (sdX * sdY);
					ccArray[x][y] = cc;
					ccArray[y][x] = cc;
				}
				ccArray[x][x] = 1.0;
			}
		}
		private int getIndex(String name) {
			if (indexMap.containsKey(name)) {
				return indexMap.get(name);
			} else {
				logger.error("Unknown name = {}", name);
				throw new SecuritiesException("unknown name");
			}
		}
		double getCorrelation(String nameX, String nameY) {
			final int indexX = getIndex(nameX);
			final int indexY = getIndex(nameY);
			
			return ccArray[indexX][indexY];
		}
		
		double getCorrelationX(String nameX, String nameY) {
			double[] x = dataMap.get(nameX);
			double[] y = dataMap.get(nameY);
			return new PearsonsCorrelation().correlation(x, y);
		}
		Map<String, Double> getCorrelation(String name) {
			final int index = getIndex(name);
			Map<String, Double> ret = new TreeMap<>();
			for(int i = 0; i < names.length; i++) {
				ret.put(names[i], ccArray[index][i]);
			}
			return ret;
		}
	}
	
	static void calculate(final Connection connection, final Writer w, final int months) throws IOException {
		logger.info("month         = {}", months);

		// dateFrom and dateTo is range of days (both inclusive)
		LocalDate dateTo   = LocalDate.parse(PriceTable.getLastTradeDate(connection));;
		LocalDate dateFrom = dateTo.minusMonths(months).plusDays(1);
		logger.info("dateRange     = {} - {}", dateFrom, dateTo);		
		
		Map<String, NasdaqTable> nasdaqMap = NasdaqTable.getMap(connection);
		logger.info("nasdaqMap     = {}", nasdaqMap.size());

		List<PriceTable> dataListTo   = PriceTable.getAllByDate(connection, dateTo);
		List<PriceTable> dataListFrom = PriceTable.getAllByDate(connection, dateFrom);
		
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
		logger.info("symbolList    = {}", symbolList.size());
		
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
		logger.info("symbolList    = {}", symbolList.size());
		
//		// Filter out securities that has average volume for last 90 days is less than 50,000
//		{
//			List<String> newSymbolList = new ArrayList<>();
//
//			final int DURATION_DAYS = 90;
//			final int MIN_AVG_VOL   = 50_000;
//			logger.info("DURATION_DAYS = {}", DURATION_DAYS);
//			logger.info("MIN_AVG_VOL   = {}", MIN_AVG_VOL);
//			
//			for(String symbol: symbolList) {
//				double averageVolume = PriceTable.getAverageVolume(connection, symbol, dateFrom, dateTo);
//				if (averageVolume < MIN_AVG_VOL) continue;
//				
//				newSymbolList.add(symbol);
//			}
//			symbolList = newSymbolList;
//		}
//		logger.info("symbolList    = {}", symbolList.size());
		
		List<String> dateList = PriceTable.getAllBySymbolDateRange(connection, "IBM", dateFrom, dateTo).stream().map(o -> o.date).collect(Collectors.toList());
		logger.info("dateList      = {}", dateList.size());
		int sampleCount = 0;
		int skip        = 0;
		{
			final int dateCount = dateList.size();
			
			int reminder = dateCount;
			for(int i = 40; 20 <= i; i--) {
				skip = dateCount % i;
				if (skip == 0) {
					sampleCount = dateCount / i;
					reminder    = skip;
					break;
				}
				if (skip < reminder) {
					sampleCount = dateCount / i;
					reminder    = skip;
				}
			}
		}
		logger.info("skip          = {}", skip);
		logger.info("sampleCount   = {}", sampleCount);
		logger.info("sample        = {}", dateList.size() / sampleCount);
		Map<String, double[]> priceMap = new TreeMap<>();
		for(String symbol: symbolList) {
			List<PriceTable> list = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
			if (list.size() != dateList.size()) {
				logger.warn("SKIP  symbol = {}  list = {}  dateList = {}", symbol, list.size(), dateList.size());
				continue;
			}
			double[] value = list.stream().mapToDouble(o -> o.close).skip(skip).map(MovingAverage.getInstance(sampleCount)).flatMap(Sample.getInstance(sampleCount)).toArray();
			priceMap.put(symbol, value);
		}
		
		{
			logger.info("CorrelationMap");
			CorrelationMap cm = new CorrelationMap(priceMap);
			logger.info("cm   {} x {}", cm.size, cm.length);
			
			//  SPY  QQQ = 0.980560447
			logger.info("X SPY  QQQ = {}", String.format("%.9f", cm.getCorrelationX("SPY", "QQQ")));
			logger.info("  SPY  QQQ = {}", String.format("%.9f", cm.getCorrelation("SPY", "QQQ")));
			logger.info("  SPY  QQQ = {}", String.format("%.9f", cm.getCorrelation("SPY").get("QQQ")));
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		final int months = 48;
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection(JDBC_URL);
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				calculate(connection, bw, months);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
