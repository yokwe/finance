package yokwe.finance.securities.database;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.JDBCUtil;

public final class PriceTable {
	private static final Logger logger = LoggerFactory.getLogger(PriceTable.class);

	public static List<PriceTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, PriceTable.class);
	}
	
	public static PriceTable getBySymbolDate(Connection connection, String symbol, LocalDate date) {
		String dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
		
		String sql = String.format("select * from price where symbol = '%s' and date = '%s'", symbol, dateString);
		List<PriceTable> result = JDBCUtil.getResultAll(connection, sql, PriceTable.class);
		if (result.size() == 0) {
			return null;
		}
		if (result.size() != 1) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		return result.get(0);
	}
		
	public static List<PriceTable>  getAllByDateRange(Connection connection, LocalDate dateFrom, LocalDate dateTo) {
		final String stringFrom = dateFrom.format(DateTimeFormatter.ISO_LOCAL_DATE);
		final String stringTo   = dateTo.format(DateTimeFormatter.ISO_LOCAL_DATE);

		String sql = String.format("select * from price where '%s' <= date and date <= '%s'", stringFrom, stringTo);
		List<PriceTable> result = JDBCUtil.getResultAll(connection, sql, PriceTable.class);
		return result;
	}
	
	public static List<PriceTable>  getAllBySymbolDateLike(Connection connection, String symbol, String dateLikeString) {
		String sql = String.format("select * from price where symbol = '%s' and date like '%s'", symbol, dateLikeString);
		List<PriceTable> result = JDBCUtil.getResultAll(connection, sql, PriceTable.class);
		return result;
	}
	
	public static class SymbolCountTable {
		public String symbol;
		public int    count;
	}
	public static Map<String, Integer> getSymbolCount(Connection connection, String dateLikeString) {
		String sql = String.format("select symbol, count(*) as count from price where date like '%s' group by symbol", dateLikeString);
		List<SymbolCountTable> result = JDBCUtil.getResultAll(connection, sql, SymbolCountTable.class);
		if (result.size() == 0) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		Map<String, Integer> ret = new TreeMap<>();
		result.stream().forEach(o -> ret.put(o.symbol, o.count));
		
		return ret;
	}
	
	public static Map<String, Integer> getAverageVolume(Connection connection, String dateFrom, String dateTo) {
		String sql = String.format("select symbol, cast(avg(volume) as INTEGER) as count from price where date between '%s' and '%s' group by symbol", dateFrom, dateTo);
		List<SymbolCountTable> result = JDBCUtil.getResultAll(connection, sql, SymbolCountTable.class);
		if (result.size() == 0) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		Map<String, Integer> ret = new TreeMap<>();
		result.stream().forEach(o -> ret.put(o.symbol, o.count));
		
		return ret;
	}
	
	public static class DateTable {
		public String date;
	}
	public static String getLastTradeDate(Connection connection) {
		String sql = "select max(date) as date from price";
		List<DateTable> result = JDBCUtil.getResultAll(connection, sql, DateTable.class);
		if (result.size() != 1) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		return result.get(0).date;
	}
	
	public static List<PriceTable> getAllByDate(Connection connection, String date) {
		String sql = String.format("select * from price where date = '%s'", date);
		return getAll(connection, sql);
	}

	public String date;
	public String symbol;
	public double close;
	public int    volume;
	
	public PriceTable(String date, String symbol, double close, int volume) {
		this.date   = date;
		this.symbol = symbol;
		this.close  = close;
		this.volume = volume;
	}
	public PriceTable() {
		this.date   = "";
		this.symbol = "";
		this.close  = 0;
		this.volume = 0;
	}
	@Override
	public String toString() {
		return String.format("{%s  %s  %6.2f, %d}", date, symbol, close, volume);
	}
}
