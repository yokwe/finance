package yokwe.finance.securities.eod;

public class Stock {
	public String symbol;
	public String symbolGoogle;
	public String symbolNasdaq;
	public String symbolYahoo;
	public String exchange;
	public String etf;
	
	public long   marketCap;
	public String country;
	public String sector;
	public String industry;
	public String name;
		
	@Override
	public String toString() {
		return String.format("%s %s", exchange, name);
	}
}
