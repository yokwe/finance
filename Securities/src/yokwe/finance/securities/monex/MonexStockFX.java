package yokwe.finance.securities.monex;

public class MonexStockFX {
	public String date;
	
	public double tts;
	public double ttb;
	
	public MonexStockFX(String date, double tts, double ttb) {
		this.date = date;
		this.tts  = tts;
		this.ttb  = ttb;
	}
	
	@Override
	public String toString() {
		return String.format("{%s %6.2f %6.2f}", date, tts, ttb);
	}
}
