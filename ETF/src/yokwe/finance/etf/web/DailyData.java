package yokwe.finance.etf.web;

public final class DailyData {
	public String date;
	public String symbol;
	public double value;
	
	public DailyData(String date, String symbol, double value) {
		this.date   = date;
		this.symbol = symbol;
		this.value  = value;
	}
	public DailyData() {
		this.date   = "";
		this.symbol = "";
		this.value  = 0;
	}
}