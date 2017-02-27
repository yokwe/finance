package yokwe.finance.securities.eod;

import java.io.File;
import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class Price {
	public String date;
	public String symbol;
	public double open;
	public double high;
	public double low;
	public double close;
	public long   volume;
	
	public Price() {
		this("", "", 0, 0, 0, 0, 0);
	}
	
	public Price(String date, String symbol, double open, double high, double low, double close, long volume) {
		this.date   = date;
		this.symbol = symbol;
		this.open   = open;
		this.high   = high;
		this.low    = low;
		this.close  = close;
		this.volume = volume;
	}
	
	@Override
	public String toString() {
//		return String.format("[%s  %6.2f %6.2f %6.2f %6.2f %d]", date, open, high, low, close, volume);
		return String.format("[%-9s %s %6.2f]", symbol, date, close);
	}
	
	public static List<Price> load(String provider, String symbol) {
		UpdateProvider updateProvider = UpdatePrice.getProvider(provider);
		File file = updateProvider.getFile(symbol);
		if (file.canRead()) {
			return CSVUtil.loadWithHeader(file.getPath(), Price.class);
		} else {
			return null;
		}
	}

	public static List<Price> load(File file) {
		return CSVUtil.loadWithHeader(file.getPath(), Price.class);
	}
}
