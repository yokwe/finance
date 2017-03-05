package yokwe.finance.securities.eod;

import java.io.File;
import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

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
		return String.format("[%-9s %s %8.4f]", symbol, date, dividend);
	}
	
	public static List<Dividend> load(String symbol) {
		UpdateProvider updateProvider = UpdateDividend.getProvider(UpdateProvider.YAHOO);
		File file = updateProvider.getFile(symbol);
		return load(file);
	}
	
	public static List<Dividend> load(File file) {
		return CSVUtil.loadWithHeader(file.getPath(), Dividend.class);
	}
	
	
	public static void save(List<Dividend> dividendList, String symbol) {
		UpdateProvider updateProvider = UpdateDividend.getProvider(UpdateProvider.YAHOO);
		File file = updateProvider.getFile(symbol);
		save(dividendList, file);
	}
	public static void save(List<Dividend> dividendList, File file) {
		CSVUtil.saveWithHeader(dividendList, file.getPath());
	}
}
