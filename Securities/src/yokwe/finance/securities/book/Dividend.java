package yokwe.finance.securities.book;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;

public class Dividend {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Dividend.class);

	String date;
	String symbol;
	String name;
	double quantity;
	double credit;
	double debit;
	double usdjpy;
	
	private Dividend(String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		this.date     = date;
		this.symbol   = symbol;
		this.name     = name;
		this.quantity = quantity;
		this.credit   = credit;
		this.debit    = debit;
		this.usdjpy   = usdjpy;
	}
	
	private static Map<String, Dividend> dividendMap = new LinkedHashMap<>();

	public static void dividend(String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		String key = date + symbol;
		if (dividendMap.containsKey(key)) {
			Dividend dividend = dividendMap.get(key);
			dividend.quantity += quantity; // TODO how to process multiple NRA for a securities of same date.
			dividend.credit   += credit;
			dividend.debit    += debit;
		} else {
			Dividend dividend = new Dividend(date, symbol, name, quantity, credit, debit, usdjpy);
			dividendMap.put(key, dividend);
		}
	}
}
