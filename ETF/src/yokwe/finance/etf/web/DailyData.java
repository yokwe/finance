package yokwe.finance.etf.web;

public class DailyData {
	public String date;
	public String symbol;
	public double value;
	
	public DailyData(String date, String symbol, double value) {
		this.date   = date;
		this.symbol = symbol;
		this.value  = value;
	}
}