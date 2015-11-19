package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.securities.util.JDBCUtil;

public final class CompanyTable {
	public static List<CompanyTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, CompanyTable.class);
	}
	public static Map<String, CompanyTable> getMap(Connection connection) {
		final Map<String, CompanyTable> ret = new TreeMap<>();
		getAll(connection, "select * from company").stream().forEach(o -> ret.put(o.symbol, o));
		return ret;
	}

	public String symbol;
	public String country;
	public String sector;
	public String industry;
	
	public CompanyTable() {
		symbol   = "";
		country  = "";
		sector   = "";
		industry = "";
	}
	public CompanyTable(String symbol, String country, String sector, String industry) {
		this.symbol   = symbol;
		this.country  = country;
		this.sector   = sector;
		this.industry = industry;
	}
	@Override
	public String toString() {
		return String.format("{%s  %s  %s  %s}", symbol, country, sector, industry);
	}
	
	private static String quote(String string) {
		String ret = new String(string);
		if (ret.contains("\"")) ret = ret.replaceAll("\"", "\"\"");
		if (ret.contains(","))  ret = "\"" + ret + "\"";
		
		return ret;
	}
	
	public String toCSV() {
		return String.format("%s,%s,%s,%s", symbol, quote(country), quote(sector), quote(industry));
	}
}