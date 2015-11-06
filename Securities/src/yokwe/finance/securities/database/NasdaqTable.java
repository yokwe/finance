package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.securities.util.JDBCUtil;

public final class NasdaqTable {
	public static List<NasdaqTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, NasdaqTable.class);
	}
	public static Map<String, NasdaqTable> getMap(Connection connection) {
		final Map<String, NasdaqTable> ret = new TreeMap<>();
		getAll(connection, "select * from nasdaq").stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}

	public String etf;
	public String exchange;
	public String symbol;
	public String base;
	public String yahoo;
	public String google;
	public String nasdaq;
	public String name;
	
	public NasdaqTable() {
		etf      = "";
		exchange = "";
		symbol   = "";
		base     = "";
		yahoo    = "";
		google   = "";
		nasdaq   = "";
		name     = "";
	}
	public NasdaqTable(String etf, String exchange, String symbol, String base, String yahoo, String google, String nasdaq, String name) {
		this.etf      = etf;
		this.exchange = exchange;
		this.symbol   = symbol;
		this.base     = base;
		this.yahoo    = yahoo;
		this.google   = google;
		this.nasdaq   = nasdaq;
		this.name     = name;
	}
	
	@Override
	public String toString() {
		return String.format("{%s %s %s %s %s %s %s %s}", etf, exchange, symbol, base, yahoo, google, nasdaq, name);
	}
}