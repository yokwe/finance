package yokwe.finance.securities.eod;

import yokwe.finance.securities.util.CSVUtil.ColumnName;

public class YahooPortfolio {
	@ColumnName("Symbol")
	public String symbol;
	@ColumnName("Current Price")
	public double currentPrice;
	@ColumnName("Date")
	public String date;
	@ColumnName("Time")
	public String time;
	@ColumnName("Change")
	public double change;
	@ColumnName("Open")
	public double open;
	@ColumnName("High")
	public double high;
	@ColumnName("Low")
	public double low;
	@ColumnName("Volume")
	public long   volume;
	@ColumnName("Trade Date")
	public String tradeDate;
	@ColumnName("Purchase Price")
	public double purchasePrice;
	@ColumnName("Quantity")
	public double quantity;
	@ColumnName("Commision")
	public double commision;
	@ColumnName("High Limit")
	public double highLimit;
	@ColumnName("Low Limit")
	public double lowLimit;
	@ColumnName("Commit")
	public String comment;
	
	public YahooPortfolio() {
		this.symbol        = "";
		this.currentPrice  = 0;
		this.date          = "";
		this.time          = "";
		this.change        = 0;
		this.open          = 0;
		this.high          = 0;
		this.low           = 0;
		this.volume        = 0;
		this.tradeDate     = "";
		this.purchasePrice = 0;
		this.quantity      = 0;
		this.commision     = 0;
		this.highLimit     = 0;
		this.lowLimit      = 0;
		this.comment       = "";
	}
	public YahooPortfolio(String symbol, double purchasePrice, double quantity) {
		this.symbol        = symbol;
		this.currentPrice  = 0;
		this.date          = "";
		this.time          = "";
		this.change        = 0;
		this.open          = 0;
		this.high          = 0;
		this.low           = 0;
		this.volume        = 0;
		this.tradeDate     = "";
		this.purchasePrice = purchasePrice;
		this.quantity      = quantity;
		this.commision     = 0;
		this.highLimit     = 0;
		this.lowLimit      = 0;
		this.comment       = "";
	}
}
