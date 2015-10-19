package yokwe.finance.securities.app;

public final class FinanceData {
	public String exchange;
	public String symbol;
	public double price;
	public long   avg_vol;
	public long   shares;
	public long   mkt_cap;
	public String name;
	
	public FinanceData() {
		this.exchange = "";
		this.symbol   = "";
		this.price    = 0;
		this.avg_vol  = 0;
		this.shares   = 0;
		this.mkt_cap  = 0;
		this.name     = "";
	}
}