package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.securities.util.JDBCUtil;

public final class FinanceTable {
	public static List<FinanceTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, FinanceTable.class);
	}
	public static Map<String, FinanceTable> getMap(Connection connection) {
		final Map<String, FinanceTable> ret = new TreeMap<>();
		getAll(connection, "select * from finance").stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}
	
	public String symbol;
	public double price;
	public long   vol;
	public long   avg_vol;
	public long   mkt_cap;
	
	public FinanceTable() {
		this.symbol   = "";
		this.price    = 0;
		this.vol      = 0;
		this.avg_vol  = 0;
		this.mkt_cap  = 0;
	}
	
	public String toString() {
		return String.format("%s  %6.2f  %d  %d  %d", symbol, price, vol, avg_vol, mkt_cap);
	}
}