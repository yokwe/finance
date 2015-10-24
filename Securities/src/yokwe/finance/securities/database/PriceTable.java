package yokwe.finance.securities.database;

public final class PriceTable {
	public String date;
	public String symbol;
	public double close;
	public int    volume;
	
	public PriceTable(String date, String symbol, double close, int volume) {
		this.date   = date;
		this.symbol = symbol;
		this.close  = close;
		this.volume = volume;
	}
	public PriceTable() {
		this.date   = "";
		this.symbol = "";
		this.close  = 0;
		this.volume = 0;
	}
}
