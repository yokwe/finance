package yokwe.finance.securities.eod;

import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class Stock {
	public String symbol;
	public String symbolGoogle;
	public String symbolNasdaq;
	public String symbolYahoo;
	public String exchange;
	public String etf;
	
	public long   marketCap;
	public String country;
	public String sector;
	public String industry;
	public String name;
		
	@Override
	public String toString() {
		return String.format("%s %s %s", exchange, symbol, name);
	}
	
	public static void save(List<Stock> stockList) {
		CSVUtil.saveWithHeader(stockList, UpdateStock.PATH_STOCK);
	}
	public static List<Stock> load() {
		return CSVUtil.loadWithHeader(UpdateStock.PATH_STOCK, Stock.class);
	}
}
