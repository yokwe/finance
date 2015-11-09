package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.JDBCUtil;

public final class DividendTable {
	private static final Logger logger = LoggerFactory.getLogger(DividendTable.class);

	public static List<DividendTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, DividendTable.class);
	}
	public static List<DividendTable> getAllBySymbol(Connection connection, String symbol) {
		final String sql = String.format("select * from dividend where symbol = '%s'", symbol);
		return getAll(connection, sql);
	}
	
	public static class SymbolCountTable {
		public String symbol;
		public int    count;
	}
	public static Map<String, Integer> getSymbolCount(Connection connection, String dateLikeString) {
		String sql = String.format("select symbol, count(*) as count from dividend where date like '%s' group by symbol", dateLikeString);
		List<SymbolCountTable> result = JDBCUtil.getResultAll(connection, sql, SymbolCountTable.class);
		if (result.size() == 0) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		Map<String, Integer> ret = new TreeMap<>();
		result.stream().forEach(o -> ret.put(o.symbol, o.count));
		
		return ret;
	}


	public String date;
	public String symbol;
	public double dividend;
	
	public DividendTable(String date, String symbol, double dividend) {
		this.date      = date;
		this.symbol    = symbol;
		this.dividend  = dividend;
	}
	public DividendTable() {
		this.date     = "";
		this.symbol   = "";
		this.dividend = 0;
	}
	@Override
	public String toString() {
		return String.format("{%s %s %6.3f}", date, symbol, dividend);
	}
	
	public String toCSV() {
		return String.format("%s,%s,%.3f", date, symbol, dividend);
	}
}