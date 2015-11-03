package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.database.PriceTable;

public class CheckPrice {
	private static final Logger logger = LoggerFactory.getLogger(CheckPrice.class);
	
	static final int YEAR_FIRST = 1975;
	static final int YEAR_LAST  = LocalDate.now().getYear();
	
	static void check(Connection connection, final BufferedWriter wr, final Map<String, NasdaqTable>  nasdaqMap, final LocalDate dateFrom, final LocalDate dateTo) throws IOException {
		String saveFilePath = String.format("tmp/database/price-%d.csv", dateFrom.getYear());
		File saveFile = new File(saveFilePath);
		if (saveFile.isFile()) {
			logger.info("# skip {}", saveFilePath);
			return;
		}
		
		List<PriceTable> data = PriceTable.getAllByDateRange(connection, dateFrom, dateTo);
		logger.info("data {}  {}  {}", dateFrom, dateTo, data.size());
		wr.append(String.format("# data  %s  %s  %d\n", dateFrom, dateTo, data.size()));

		List<String> dateList = data.stream().filter(o -> o.symbol.equals("IBM")).map(o -> o.date).collect(Collectors.toList());
		Collections.sort(dateList);
		Set<String> dateSet = new HashSet<>(dateList);
		
		// check duplicate
		{
			String lastDate = null;
			for(String date: dateList) {
				if (lastDate != null && date.equals(lastDate)) {
					logger.error("duplicate dateList {}", date);
					throw new SecuritiesException("duplicate date");
				}
				lastDate = date;
			}
		}
		
		// check with NYT
		{
			List<String> dateList2 = data.stream().filter(o -> o.symbol.equals("NYT")).map(o -> o.date).collect(Collectors.toList());
			Set<String> dateSet2 = new HashSet<>(dateList2);
			for(String date: dateList) {
				if (dateSet2.contains(date)) continue;
				logger.info("dateSet date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
			for(String date: dateList2) {
				if (dateSet.contains(date)) continue;
				logger.info("dateSet2 date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
		}
		
		// check with PG
		{
			List<String> dateList2 = data.stream().filter(o -> o.symbol.equals("PG")).map(o -> o.date).collect(Collectors.toList());
			Set<String> dateSet2 = new HashSet<>(dateList2);
			for(String date: dateList) {
				if (dateSet2.contains(date)) continue;
				logger.info("dateSet date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
			for(String date: dateList2) {
				if (dateSet.contains(date)) continue;
				logger.info("dateSet2 date is missing  date = {}", date);
				throw new SecurityException("dateSet");
			}
		}
		
		int errorCount = 0;
		for(String symbol: nasdaqMap.keySet()) {
			List<String> dateList2 = data.stream().filter(o -> o.symbol.equals(symbol)).map(o -> o.date).collect(Collectors.toList());
			if (dateList2.size() == 0) continue;
			Set<String> dateSet2 = new HashSet<>(dateList2);
			
			Collections.sort(dateList2);
			{
				String lastDate = null;
				for(String date: dateList2) {
					if (lastDate != null && date.equals(lastDate)) {
						logger.info("dup      {} {}", date, symbol);
						wr.append(String.format("dup      %s  %s\n", date, symbol));
						errorCount++;
//						throw new SecuritiesException("duplicate date");
					}
					lastDate = date;
				}
			}

			String dateFirst = dateList2.get(0);
			
			for(String date: dateList) {
				if (date.compareTo(dateFirst) < 0) continue;
				if (dateSet2.contains(date)) continue;
				logger.info("missing  {} {}", date, symbol);
				wr.append(String.format("missing  %s  %s\n", date, symbol));
				errorCount++;
//				throw new SecurityException("dateSet");
			}
			for(String date: dateList2) {
				if (dateSet.contains(date)) continue;
				logger.info("surplus  {} {}", date, symbol);
				wr.append(String.format("surplus  %s  %s\n", date, symbol));
				errorCount++;
//				throw new SecurityException("dateSet");
			}
		}
		
		if (errorCount == 0) {
			// Save content of data to saveFile
			logger.info("# save {}", saveFilePath);
			try (BufferedWriter save = new BufferedWriter(new FileWriter(saveFile))) {
				for(PriceTable table: data) {
					// 1975-10-27,AA,36.25,276800
					save.write(String.format("%s,%s,%.2f,%d\n", table.date, table.symbol, table.close, table.volume));
				}
			}
		}
	}

	private static final String OUTPUT_PATH = "tmp/checkPrice.log";
	
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/securities.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
				Map<String, NasdaqTable>  nasdaqMap  = NasdaqTable.getMap(connection);
				logger.info("nasdaqMap     = {}", nasdaqMap.size());

				for(int year = YEAR_FIRST; year <= YEAR_LAST; year++) {
					LocalDate dateFrom = LocalDate.of(year, 1, 1);
					LocalDate dateTo = dateFrom.plusYears(1).minusDays(1);
					check(connection, bw, nasdaqMap, dateFrom, dateTo);
				}
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
