package yokwe.finance.stock.data;

import yokwe.finance.stock.util.CSVUtil.ColumnName;

public class YahooPrice {
	public static final String HEADER = "Date,Open,High,Low,Close,Volume,Adj Close";

	@ColumnName("Date")
	public String date;
	@ColumnName("Open")
	public double open;
	@ColumnName("High")
	public double high;
	@ColumnName("Low")
	public double low;
	@ColumnName("Close")
	public double close;
	@ColumnName("Adj Close")
	public double adjClose;
	@ColumnName("Volume")
	public long volume;
	
	public YahooPrice(String date, double open, double high, double low, double close, double adjClose, long volume) {
		this.date      = date;
		this.open      = open;
		this.high      = high;
		this.low       = low;
		this.close     = close;
		this.adjClose  = adjClose;
		this.volume    = volume;
	}
	public YahooPrice() {
		this("", 0, 0, 0, 0, 0, 0);
	}
}
