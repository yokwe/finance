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
}