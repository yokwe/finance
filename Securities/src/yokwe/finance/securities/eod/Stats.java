package yokwe.finance.securities.eod;

public class Stats {
//	public String exchange;
	public String symbol;
	public String name;
	
	// last values
	public String date;
	public double price;
	
	// price
	public int    pricec;
	
	public double sd;
	public double hv;
	public double rsi;

	public double min;
	public double max;
	public double minpct;
	public double maxpct;

	// dividend
	public double div;
	public int    divc;
	public double yield;
	
	// volume
	public long   vol;
	public long   vol5;
	public long   vol30;
	
	// price change detection
	public double sma5;
	public double sma20;
	public double sma50;
	public double sma200;
	
	public double sma5pct;
	public double sma20pct;
	public double sma50pct;
	public double sma200pct;

	public double last;
	public double lastpct;
}
