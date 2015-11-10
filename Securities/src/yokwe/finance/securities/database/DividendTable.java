package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;

import yokwe.finance.securities.util.JDBCUtil;

public final class DividendTable {
	public static List<DividendTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, DividendTable.class);
	}
	public static List<DividendTable> getAllBySymbol(Connection connection, String symbol) {
		final String sql = String.format("select * from dividend where symbol = '%s'", symbol);
		return getAll(connection, sql);
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