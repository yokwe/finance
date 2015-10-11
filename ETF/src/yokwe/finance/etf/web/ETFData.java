package yokwe.finance.etf.web;

public final class ETFData {
	public String symbol;
	public String name;
	public String segment;
	public String score;
	public String next_ex_dividend;
	public double distribution_yield;
	
	public long   aum; // asset under management
	public long   adv; // average daily volume
	public double asp; // average spread
}