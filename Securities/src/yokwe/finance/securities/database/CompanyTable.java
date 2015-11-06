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
	public String sector;
	public String industry;
	public String name;
	
	public CompanyTable() {
		symbol   = "";
		sector   = "";
		industry = "";
		name     = "";
	}
	public CompanyTable(String symbol, String sector, String industry, String name) {
		this.symbol   = symbol;
		this.sector   = sector;
		this.industry = industry;
		this.name     = name;
	}
	@Override
	public String toString() {
		return String.format("{%s  %s  %s  %s}", symbol, sector, industry, name);
	}
	
	private static String quote(String string) {
		String ret = new String(string);
		if (ret.contains("\"")) ret = ret.replaceAll("\"", "\"\"");
		if (ret.contains(","))  ret = "\"" + ret + "\"";
		
		return ret;
	}
	
	public String toCSV() {
		return String.format("%s,%s,%s,%s", symbol, quote(sector), quote(industry), quote(name));
	}
}