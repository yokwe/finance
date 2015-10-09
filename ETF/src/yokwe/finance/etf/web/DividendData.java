package yokwe.finance.etf.web;

import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import yokwe.finance.etf.util.JDBCUtil;

public final class DividendData extends Data {
	private static String SQL = "select date, symbol, dividend from yahoo_dividend where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
	private static String getSQL(String symbol, String fromDate, String toDate) {
		return String.format(SQL, symbol, fromDate, toDate);
	}
	
	public String date;
	public String symbol;
	public double dividend;
	
	public List<DailyData> generate(Statement statement, String symbol, Period period) {
		List<DailyData> ret = JDBCUtil.getResultAll(statement, getSQL(symbol, period.dateStart, period.dateEnd), DividendData.class).stream().map(o -> o.toDailyData()).collect(Collectors.toList());
		return ret;
	}
	
	private DailyData toDailyData() {
		return new DailyData(date, symbol, dividend);
	}
}