package yokwe.finance.securities.eod;

public class Stock implements Comparable<Stock> {
	public String symbol;
	public String exchange;
	public String issueType;
	public String sector;
	public String industry;
	public String name;

	@Override
	public int compareTo(Stock that) {
		return this.symbol.compareTo(that.symbol);
	}
		
	@Override
	public String toString() {
		return String.format("%s %s %s", exchange, symbol, name);
	}
}
