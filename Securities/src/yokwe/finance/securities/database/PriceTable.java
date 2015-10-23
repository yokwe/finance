package yokwe.finance.securities.database;

public final class PriceTable {
	public String date;
	public String symbol;
	public double open;
	public double high;
	public double low;
	public double close;
	public int    volume;
	
	public PriceTable(String date, String symbol, double open, double high, double low, double close, int volume) {
		this.date   = date;
		this.symbol = symbol;
		this.open   = open;
		this.high   = high;
		this.low    = low;
		this.close  = close;
		this.volume = volume;
	}
	public PriceTable() {
		this.date   = "";
		this.symbol = "";
		this.open   = 0;
		this.high   = 0;
		this.low    = 0;
		this.close  = 0;
		this.volume = 0;
	}
}
