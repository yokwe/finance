package yokwe.finance.etf.web;

import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import yokwe.finance.etf.util.JDBCUtil;

public final class VolumeData extends Data {
	private static String SQL = "select date, symbol, volume from yahoo_daily where symbol = '%s' and '%s' <= date and date <= '%s' order by date";
	private static String getSQL(String symbol, String fromDate, String toDate) {
		return String.format(SQL, symbol, fromDate, toDate);
	}
	
	public String date;
	public String symbol;
	public int    volume;
	
	public List<DailyData> generate(Statement statement, String symbol, Period period) {
		List<DailyData> ret = JDBCUtil.getResultAll(statement, VolumeData.getSQL(symbol, period.dateStart, period.dateEnd), VolumeData.class).stream().map(o -> o.toDailyData()).collect(Collectors.toList());
		return ret;
	}

	private DailyData toDailyData() {
		return new DailyData(date, symbol, volume);
	}
}