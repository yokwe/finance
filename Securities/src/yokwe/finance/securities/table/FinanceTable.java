package yokwe.finance.securities.table;

public final class FinanceTable {
	public String symbol;
	public double price;
	public long   vol;
	public long   avg_vol;
	public long   mkt_cap;
	
	public FinanceTable() {
		this.symbol   = "";
		this.price    = 0;
		this.vol      = 0;
		this.avg_vol  = 0;
		this.mkt_cap  = 0;
	}
}