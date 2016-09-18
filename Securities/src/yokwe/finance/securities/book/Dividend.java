package yokwe.finance.securities.book;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Dividend {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Dividend.class);

	String transaction;
	String date;
	String symbol;
	String name;
	double quantity;
	double credit;
	double debit;
	double usdjpy;
	
	private Dividend(String transaction, String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		this.transaction = transaction;
		this.date        = date;
		this.symbol      = symbol;
		this.name        = name;
		this.quantity    = quantity;
		this.credit      = credit;
		this.debit       = debit;
		this.usdjpy      = usdjpy;
	}
	
	private static Map<String, Dividend> dividendMap = new LinkedHashMap<>();

	private static void transaction(String transaction, String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		String key = String.format("%s-%s", date, symbol);
		if (dividendMap.containsKey(key)) {
			Dividend dividend = dividendMap.get(key);
			// Sanity check
			if (!dividend.transaction.equals(transaction)) {
				logger.error("Unexpected transaction {} => {}", dividend.transaction, transaction);
				throw new SecuritiesException("Unexpected chagne");
			}
			if (dividend.quantity != quantity) {
				logger.error("Unknown quantity {} => {}", String.format("%.6f", dividend.quantity), String.format("%.6f", quantity));
				throw new SecuritiesException("Unexpected quantity");
			}
			
			dividend.credit   += credit;
			dividend.debit    += debit;
		} else {
			Dividend dividend = new Dividend(transaction, date, symbol, name, quantity, credit, debit, usdjpy);
			dividendMap.put(key, dividend);
		}
	}
	public static void dividend(String transaction, String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		transaction("dividend", date, symbol, name, quantity, credit, debit, usdjpy);
	}
	public static void mlp(String transaction, String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		transaction("mlp", date, symbol, name, quantity, credit, debit, usdjpy);
	}
	public static void nra(String transaction, String date, String symbol, String name, double quantity, double credit, double debit, double usdjpy) {
		transaction("nra", date, symbol, name, quantity, credit, debit, usdjpy);
	}
	
	public static void addRemaining(List<ReportDividend> reportList) {
		for(Map.Entry<String, Dividend> entry: dividendMap.entrySet()) {
			Dividend dividend = entry.getValue();
			double dividendJPY = dividend.usdjpy * dividend.credit;
			double taxWithholdingJPY = dividend.usdjpy * dividend.debit;
			ReportDividend report = ReportDividend.getInstance(
					dividend.date, dividend.symbol, dividend.name, dividend.quantity,
					dividend.credit, dividend.debit, dividend.usdjpy, dividendJPY, taxWithholdingJPY);
			reportList.add(report);
		}
	}
}
