package yokwe.finance.securities.table;

public final class NasdaqTable {
	public String etf;
	public String exchange;
	public String symbol;
	public String yahoo;
	public String google;
	public String nasdaq;
	public String name;
	
	public NasdaqTable() {
		etf      = "";
		exchange = "";
		symbol   = "";
		yahoo    = "";
		google   = "";
		nasdaq   = "";
		name     = "";
	}
	public NasdaqTable(String etf, String exchange, String symbol, String yahoo, String google, String nasdaq, String name) {
		this.etf      = etf;
		this.exchange = exchange;
		this.symbol   = symbol;
		this.yahoo    = yahoo;
		this.google   = google;
		this.nasdaq   = nasdaq;
		this.name     = name;
	}
}