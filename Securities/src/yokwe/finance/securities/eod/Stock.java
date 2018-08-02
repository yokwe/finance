package yokwe.finance.securities.eod;

public class Stock {
	public String symbol;
	public String exchange;
	public String issueType;
	public String sector;
	public String industry;
	public String name;
		
	@Override
	public String toString() {
		return String.format("%s %s %s", exchange, symbol, name);
	}
}
