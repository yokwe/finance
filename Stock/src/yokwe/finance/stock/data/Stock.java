package yokwe.finance.stock.data;

public class Stock implements Comparable<Stock> {
	public String symbol;
	public String exchange;
	public String issueType;
	public String sector;
	public String industry;
	public String name;
	
	public Stock(String symbol, String exchange, String issueType, String sector, String industry, String name) {
		this.symbol    = symbol;
		this.exchange  = exchange;
		this.issueType = issueType;
		this.sector    = sector;
		this.industry  = industry;
		this.name      = name;
	}
	
	public Stock() {
		this("", "", "", "", "", "");
	}

	@Override
	public int compareTo(Stock that) {
		return this.symbol.compareTo(that.symbol);
	}
		
	@Override
	public String toString() {
		return String.format("%s %s %s", exchange, symbol, name);
	}
}
