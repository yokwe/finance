package yokwe.finance.securities.eod;

public class Price {
	public String date;
	public String symbol;
	public double open;
	public double high;
	public double low;
	public double close;
	public long   volume;
	
	public Price() {
		this("", "", 0, 0, 0, 0, 0);
	}
	
	public Price(String date, String symbol, double open, double high, double low, double close, long volume) {
		this.date   = date;
		this.symbol = symbol;
		this.open   = open;
		this.high   = high;
		this.low    = low;
		this.close  = close;
		this.volume = volume;
	}
	
	@Override
	public String toString() {
//		return String.format("[%s  %6.2f %6.2f %6.2f %6.2f %d]", date, open, high, low, close, volume);
		return String.format("[%-9s %s %6.2f]", symbol, date, close);
	}
}
