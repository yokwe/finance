package yokwe.finance.stock.sbi;

public class StockUS implements Comparable<StockUS> {
	public String ticker;
	public String name;
	public String nameJP;
	public String description;
	public String market;
	public String category;
	
	public StockUS(String ticker, String name, String nameJP, String description, String market, String category) {
		this.ticker      = ticker;
		this.name        = name;
		this.nameJP      = nameJP;
		this.description = description;
		this.market      = market;
		this.category    = category;
	}
	public StockUS() {
		this(null, null, null, null, null, null);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s %s %s %s", ticker, name, description, market, category);
	}

	@Override
	public int compareTo(StockUS that) {
		return this.ticker.compareTo(that.ticker);
	}
}
