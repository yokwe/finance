package yokwe.finance.stock.data;

public class Forex {
	public String date;
	public double usd;
	
	public Forex(String date, double usd) {
		this.date = date;
		this.usd  = usd;
	}
	
	public Forex() {
		this("", 0);
	}
	
	@Override
	public String toString() {
		return String.format("%s %6.2f", date, usd);
	}
}
