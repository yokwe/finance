package yokwe.finance.securities.app;

public final class PriceData {
	public String date;
	public String symbol;
	public double value;
	
	public PriceData(String date, String symbol, double value) {
		this.date   = date;
		this.symbol = symbol;
		this.value  = value;
	}
	public PriceData() {
		this.date   = "";
		this.symbol = "";
		this.value  = 0;
	}
}