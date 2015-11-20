package yokwe.finance.securities.database;

import java.sql.Connection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.JDBCUtil;

public final class CorrelationTable {
	private static final Logger logger = LoggerFactory.getLogger(CorrelationTable.class);

	public static List<CorrelationTable> getAll(Connection connection, String sql) {
		return JDBCUtil.getResultAll(connection, sql, CorrelationTable.class);
	}
	public static List<CorrelationTable> getAllByAB(Connection connection, String a, String b) {
		String sql = String.format("select * from cc where a = '%s' and b = '%s'", a, b);
		return getAll(connection, sql);
	}
	public static CorrelationTable getByMonthAB(Connection connection, int month, String a, String b) {
		String sql = String.format("select * from cc where month = %d and a = '%s' and b = '%s'", month, a, b);
		List<CorrelationTable> result = getAll(connection, sql);
		if (result.size() == 0) {
			return null;
		}
		if (result.size() != 1) {
			logger.error("result = {}", result);
			throw new SecuritiesException("result");
		}
		return result.get(0);
	}

	public int    month;
	public String a;
	public String b;
	public double cc;
	
	public CorrelationTable() {
		month = 0;
		a     = "";
		b     = "";
		cc    = 0;
	}
	public CorrelationTable(int month, String a, String b, double cc) {
		this.month = month;
		this.a     = a;
		this.b     = b;
		this.cc    = cc;
	}
	@Override
	public String toString() {
		return String.format("{%2d  %s  %s  %5.2f}", month, a, b, cc);
	}
	public String toCSV() {
		return String.format("%d,%s,%s,%.2f", month, a, b, cc);
	}
}