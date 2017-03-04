package yokwe.finance.securities.eod;

import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class Forex {
	public String date;
	public double usd;
	public double gbp;
	public double eur;
	public double aud;
	public double nzd;
	
	@Override
	public String toString() {
		return String.format("%s %6.2f %6.2f %6.2f %6.2f %6.2f", date, usd, gbp, eur, aud, nzd);
	}
	
	public static List<Forex> load() {
		return CSVUtil.loadWithHeader(UpdateForex.PATH_FOREX, Forex.class);
	}
}
