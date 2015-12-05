package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.CompanyTable;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;
import yokwe.finance.securities.util.DoubleUtil.Stats;
import yokwe.finance.securities.util.NasdaqUtil;

public class ScreenByDividend {
	private static final Logger logger = LoggerFactory.getLogger(ScreenByDividend.class);
	
	// Remove out of bound value from values data
	static double[] adjust(final double[] values) {
		// Handle special case
		if (values.length <= 2) return values;
		{
			final Stats stats = new Stats();
			for(double value: values) {
				if (0.0001 <= value) stats.accept(value);
			}
			if (Double.isNaN(stats.getMean())) {
//				logger.info("AAA  values = {}", Arrays.stream(values).mapToObj(o -> new Double(o)).collect(Collectors.toList()));
				return values;
			}
		}
		
		final double min;
		final double max;
		for(int j = 0;; j++) {
			final Stats stats = new Stats();
			for(double value: values) {
				if (0.0001 <= value) stats.accept(value);
			}
			
			final double mean = stats.getMean();
			final double sd   = stats.getStandardDeviation();
			
			if (Double.isNaN(mean)) {
				logger.info("XXX {}  values = {}", j, Arrays.stream(values).mapToObj(o -> new Double(o)).collect(Collectors.toList()));
				throw new SecuritiesException();
			}
			
			// Half of values is set to -1
			if (j == (values.length / 2)) {
				min = mean - (2 * sd);
				max = mean + (2 * sd);
//				logger.info("XXX {}  values = {}", j, Arrays.stream(values).mapToObj(o -> new Double(o)).collect(Collectors.toList()));
				break;
			}
			
			if ((sd * 1) < mean) { // Normal case
				min = mean - (2 * sd);
				max = mean + (2 * sd);
				break;
			}
			final double diffMin = stats.getMean() - stats.getMin();
			final double diffMax = stats.getMax()  - stats.getMean();
			if (diffMin < diffMax) {
				for(int i = 0; i < values.length; i++) {
					if (values[i] == stats.getMax()) values[i] = -1;
				}
			} else {
				for(int i = 0; i < values.length; i++) {
					if (values[i] == stats.getMin()) values[i] = -1;
				}
			}
		}

		double avg = 0;
		double n   = 0;
		for(int i = 0; i < values.length; i++) {
			final double value = values[i];
			if (min <= value && value <= max) {
				avg = ((avg * n) + values[i]) / (n + 1);
				n += 1.0;
			} else {
				values[i] = -1;
			}
		}
		
		// replace out of bound value with average
		for(int i = 0; i < values.length; i++) {
			if (values[i] == -1) values[i] = avg;
		}
		
		return values;
	}
	
	static void calculate(Connection connection, Writer w) throws IOException {
		final LocalDate lastTradeDate;
		{
			String dateString = PriceTable.getLastTradeDate(connection);
			lastTradeDate = LocalDate.parse(dateString);
		}
		logger.info("lastTradeDate = {}", lastTradeDate);
		final String thisYear = String.format("%d", lastTradeDate.getYear());
		
		Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
		logger.info("nasdaqMap     = {}", nasdaqMap.size());
		
		Map<String, CompanyTable> companyMap = CompanyTable.getMap(connection);
		logger.info("companyMap     = {}", companyMap.size());
		
		// candidateList has all
		List<String> candidateList = nasdaqMap.keySet().stream().collect(Collectors.toList());
		
		// Build priceMap
		Map<String, Double> priceMap = new TreeMap<>();
		PriceTable.getAllByDate(connection, lastTradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE)).stream().forEach(o -> priceMap.put(o.symbol, o.close));

		// Filter out securities that has no price in lastTradedDate
		{
			List<String> newCandidateList = new ArrayList<>();
			
			for(String symbol: candidateList) {
				if (priceMap.containsKey(symbol)) {
					newCandidateList.add(symbol);
				}
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());
		
		// Filter out securities that has average volume for last 90 days is less than 50,000
		{
			List<String> newCandidateList = new ArrayList<>();

			final int DURATION_DAYS = 90;
			final int MIN_AVG_VOL   = 50_000;
			logger.info("DURATION_DAYS = {}", DURATION_DAYS);
			logger.info("MIN_AVG_VOL   = {}", MIN_AVG_VOL);
			
			String dateTo   = lastTradeDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
			String dateFrom = lastTradeDate.plusDays(-DURATION_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE);
			Map<String, Integer> avgVolumeMap = PriceTable.getAverageVolume(connection, dateFrom, dateTo);
			logger.info("avgVolumeMap  = {}", avgVolumeMap.size());
			for(String symbol: candidateList) {
				if (!avgVolumeMap.containsKey(symbol)) continue;
				
				final int averageVolume = avgVolumeMap.get(symbol);
				if (averageVolume < MIN_AVG_VOL) continue;
				
				newCandidateList.add(symbol);
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());

		// Build dividendMap
		Map<String, Map<String, List<Double>>> dividendMap = new TreeMap<>();
		for(String symbol: candidateList) {
			List<DividendTable> dividendList = DividendTable.getAllBySymbol(connection, symbol);
			Map<String, List<Double>> yearMap = new TreeMap<>();
			for(DividendTable dividendTable: dividendList) {
				// Skip very small dividend
				if (dividendTable.dividend < 0.001) continue;
				String year = dividendTable.date.substring(0, 4);
				if (!yearMap.containsKey(year)) yearMap.put(year, new ArrayList<>());
				yearMap.get(year).add(dividendTable.dividend);
			}
			dividendMap.put(symbol,  yearMap);
		}
		logger.info("dividendMap   = {}", dividendMap.size());
		
		// Filter out securities that has no dividend for this year, last year and last last year
		{
			final String lastLastYear = String.format("%d", lastTradeDate.plusYears(-2).getYear());
			final String lastYear = String.format("%d", lastTradeDate.plusYears(-1).getYear());

			List<String> newCandidateList = new ArrayList<>();
			
			for(String symbol: candidateList) {
				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				if (yearMap.containsKey(lastLastYear) && yearMap.containsKey(lastYear) && yearMap.containsKey(thisYear)) {
					newCandidateList.add(symbol);
				};
			}
			candidateList = newCandidateList;
		}
		logger.info("candidateList = {}", candidateList.size());
		
				
		// Calculate dividend of last year and this year
		final int LAST_N_YEARS = 10;  // Y4 Y3 Y2 Y1 Y0
		{
			final int y0Number = LocalDate.now().getYear();
			final String y0 = String.format("%d", y0Number);
			final String y1 = String.format("%d", y0Number - 1);
			
			// Output title line
			w.append("symbol,sector,industry,name,freq,price");
			for(int i = LAST_N_YEARS - 1; 0 <= i; i--) w.append(String.format(",y%d", i));
			w.append(",last");
			w.append("\n");

			int count = 0;
			for(String symbol: candidateList) {
				if (!priceMap.containsKey(symbol)) {
					continue;
				}
				
				final double price = priceMap.get(symbol);
				final double priceRatio = 1000.0 / price;

				Map<String, List<Double>> yearMap = dividendMap.get(symbol);
				
				final String name;
				final int    freq;
				{
					NasdaqTable nasdaqTable = NasdaqUtil.get(symbol);
					freq = yearMap.get(y1).size();
					
					String nasdaqName = nasdaqTable.name;
					if (nasdaqName.contains("\"")) nasdaqName = nasdaqName.replace("\"", "\"\"");
					if (nasdaqName.contains(",")) nasdaqName = "\"" + nasdaqName + "\"";
					name = nasdaqName;
				}
				
				double[] profit  = new double[LAST_N_YEARS];
				for(int i = 0; i < profit.length; i++) profit[i] = -1;
				
				// special for Y0
				{
					// Should we remove exceptional value?
					double[] y0Array = yearMap.get(y0).stream().mapToDouble(o -> o).toArray();
					double[] y1Array = yearMap.get(y1).stream().mapToDouble(o -> o).toArray();
					
					y0Array = adjust(y0Array);
					y1Array = adjust(y1Array);
					
					double y0Profit = Arrays.stream(y0Array).sum();
					for(int i = y0Array.length; i < y1Array.length; i++) y0Profit += y1Array[i];
					y0Profit *= priceRatio;
					profit[0] = y0Profit;
				}
				
				for(int i = 1; i < LAST_N_YEARS; i++) {
					String yyyy = String.format("%d", y0Number - i);
					if (yearMap.containsKey(yyyy)) {
						double[] yyyyArray = yearMap.get(yyyy).stream().mapToDouble(o -> o).toArray();
						yyyyArray = adjust(yyyyArray);

						profit[i] = priceRatio * Arrays.stream(yyyyArray).sum();
					}
				}
				
				w.append(String.format("%s", symbol));
				{
					CompanyTable company = companyMap.get(symbol);
					if (company == null) {
						logger.error("Unknown symbol = {}", symbol);
						throw new SecuritiesException("Unknown symbol");
					}
					String sector   = company.sector;
					if (sector.contains("\"")) sector = sector.replace("\"", "\"\"");
					if (sector.contains(","))  sector = "\"" + sector + "\"";

					String industry = company.industry;
					if (industry.contains("\"")) industry = industry.replace("\"", "\"\"");
					if (industry.contains(","))  industry = "\"" + industry + "\"";
					
					w.append(",").append(sector);
					w.append(",").append(industry);
				}
				w.append(String.format(",%s", name));
				w.append(String.format(",%d,%.2f", freq, price));

				for(int i = LAST_N_YEARS - 1; 0 <= i; i--) {
					final double p = profit[i];
					w.append((0 <= p) ? String.format(",%.3f", p) : ",");
				}
				
				// Calculate profit from last dividend within 1 month
				{
					LocalDate oneMonthBefore = lastTradeDate.minusMonths(1);
					List<DividendTable> dividendList = DividendTable.getAllBySymbolDateRange(connection, symbol, oneMonthBefore, lastTradeDate);
					w.append(",");
					if (dividendList.size() == 1) {
						// estimate dividend profit from last dividend
						final double lastDividend = dividendList.get(0).dividend;
						double profitFromLastDividend = (lastDividend * freq) * priceRatio;
						w.append(String.format("%.3f", profitFromLastDividend));
					}
				}
				
				w.append("\n");
				count++;
			}
			
			logger.info("count = {}", count);
		}
	}
	
	private static final String OUTPUT_PATH = "tmp/screenByDividend.csv";
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				calculate(connection, bw);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
