package yokwe.finance.securities.eod;

public class Dividend {
	public String date;
	public String symbol;
	public double dividend;
	
	public Dividend() {
		this("", "", 0);
	}
	
	public Dividend(String date, String symbol, double dividend) {
		this.date     = date;
		this.symbol   = symbol;
		this.dividend = dividend;
	}
	
	@Override
	public String toString() {
		return String.format("{%s %-9s %8.4f}", date, symbol, dividend);
	}
}
