package yokwe.finance.securities.eod;

public class Stock {
	public String symbol;
	public String symbolGoogle;
	public String symbolYahoo;
	public String exchange;
	public String etf;
	
	public long   marketCap;
	public String country;
	public String sector;
	public String industry;
	public String name;
	
	public Stock(String symbol, String symbolGoogle, String symbolYahoo, String exchange, String etf,
			long marketCap, String country, String sector, String industry, String name) {
		this.symbol       = symbol;
		this.symbolGoogle = symbolGoogle;
		this.symbolYahoo  = symbolYahoo;
		this.exchange     = exchange;
		this.etf          = etf;
		
		this.marketCap    = marketCap;
		this.country      = country;
		this.sector       = sector;
		this.industry     = industry;
		this.name         = name;
	}
	public Stock() {
		this("", "", "", "", "", 0, "", "", "", "");
	}
	
	@Override
	public String toString() {
		return String.format("%s %s", exchange, name);
	}
}
