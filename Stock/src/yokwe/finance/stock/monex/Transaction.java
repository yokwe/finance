package yokwe.finance.stock.monex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.DoubleUtil;

public class Transaction implements Comparable<Transaction> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);
	
	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動_monex.ods";

	public enum Type {
		JPY_IN, JPY_OUT, USD_IN, USD_OUT,
		DIVIDEND,
		BUY, SELL,
	}
	
	public final Type   type;
	public final String date; // settlement date
	
	public final String symbol;
	public final int    quantity;
	public final double price;
	public final double fee;
	public final double total;   // Actual amount subtract/add from/to account - contains fee
	
	public final int    jpy;
	public final double usd;
	public final double fxRate;
	
	private Transaction(
		Type type, String date,
		String symbol, int quantity, double price, double fee, double total,
		int jpy, double usd, double fxRate) {
		this.type     = type;
		this.date     = date;
		
		this.symbol   = symbol;
		this.quantity = quantity;
		this.price    = price;
		this.fee      = fee;
		this.total    = total;
		
		this.jpy      = jpy;
		this.usd      = usd;
		this.fxRate   = fxRate;
	}
	
	@Override
	public String toString() {
		return String.format("%s %-8s %-8s %4d %6.2f %6.2f %8.2f %8d %8.2f %6.2f",
				date, type, symbol, quantity, price, fee, total, jpy, usd, fxRate);
	}

	@Override
	public int compareTo(Transaction that) {
		int ret = this.date.compareTo(that.date);
		if (ret == 0) ret = this.type.compareTo(that.type);
		if (ret == 0) ret = this.symbol.compareTo(that.symbol);
		return ret;
	}

	private static Transaction jpyIn(String date, int jpy) {
		return new Transaction(Type.JPY_IN, date, "", 0, 0, 0, 0, jpy, 0, 0);
	}
	private static Transaction jpyOut(String date, int jpy) {
		return new Transaction(Type.JPY_OUT, date, "", 0, 0, 0, 0, jpy, 0, 0);
	}
	private static Transaction usdIn(String date, int jpy, double usd, double fxRate) {
		return new Transaction(Type.USD_IN, date, "", 0, 0, 0, 0, jpy, usd, fxRate);
	}
	private static Transaction usdOut(String date, int jpy, double usd, double fxRate) {
		return new Transaction(Type.USD_OUT, date, "", 0, 0, 0, 0, jpy, usd, fxRate);
	}
	private static Transaction buy(String date, String symbol, int quantity, double price, double fee, double total) {
		return new Transaction(Type.BUY, date, symbol, quantity, price, fee, total, 0, 0, 0);
	}
	private static Transaction sell(String date, String symbol, int quantity, double price, double fee, double total) {
		return new Transaction(Type.SELL, date, symbol, quantity, price, fee, total, 0, 0, 0);
	}
	private static Transaction dividend(String date, String symbol, int quantity, double price, double fee, double total) {
		return new Transaction(Type.DIVIDEND, date, symbol, quantity, price, fee, total, 0, 0, 0);
	}

	public static List<Transaction> getTransactionList(SpreadSheet docActivity) {
		List<Transaction> transactionList = new ArrayList<>();
		
		List<String> sheetNameList = docActivity.getSheetNameList();
		sheetNameList.sort((a, b) -> a.compareTo(b));
		
		// Process Account
		for(String sheetName: sheetNameList) {
			if (!sheetName.equals("口座")) continue;
			logger.info("Sheet {}", sheetName);
			
			List<Activity.Account> activityList = Sheet.extractSheet(docActivity, Activity.Account.class, sheetName);
			for(Activity.Account activity: activityList) {
				// Sanity check
				if (activity.settlementDate == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.transaction == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.jpy == 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				
				switch(activity.transaction) {
				case Activity.Account.TRANSACTION_FROM_SOUGOU:
					// Sanity check
					if (activity.fxRate != 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.usd != 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.jpy <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					transactionList.add(Transaction.jpyIn(activity.settlementDate, activity.jpy));
					break;
				case Activity.Account.TRANSACTION_TO_SOUGOU:
					// Sanity check
					if (activity.fxRate != 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.usd != 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (0 <= activity.jpy) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					transactionList.add(Transaction.jpyOut(activity.settlementDate, activity.jpy));
					break;
				case Activity.Account.TRANSACTION_TO_USD_DEPOSIT:
					// Sanity check
					if (activity.fxRate <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.usd <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (0 <= activity.jpy) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					transactionList.add(Transaction.usdIn(activity.settlementDate, activity.jpy, activity.usd, activity.fxRate));
					break;
				case Activity.Account.TRANSACTION_FROM_USD_DEPOSIT:
					// Sanity check
					if (activity.fxRate <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (0 <= activity.usd) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.jpy <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					transactionList.add(Transaction.usdOut(activity.settlementDate, activity.jpy, activity.usd, activity.fxRate));
					break;
				default:
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
			}			
		}
		
		// Process Dividend
		for(String sheetName: sheetNameList) {
			if (!sheetName.equals("配当")) continue;
			logger.info("Sheet {}", sheetName);
			
			List<Activity.Dividend> activityList = Sheet.extractSheet(docActivity, Activity.Dividend.class, sheetName);
			for(Activity.Dividend activity: activityList) {
				// Sanity check
				if (activity.payDateUS == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				
				double fee = DoubleUtil.roundPrice(activity.withholdingTaxUS + activity.withholdingTaxJPUS);
//				double fee = DoubleUtil.roundPrice(activity.taxBaseUS - activity.amount2);
				transactionList.add(Transaction.dividend(activity.payDateUS, activity.symbol, activity.quantity, activity.unitPrice, fee, activity.amount2));
			}
		}
		
		for(String sheetName: sheetNameList) {
			if (!sheetName.matches("^20[0-9][0-9]$")) continue;
			logger.info("Sheet {}", sheetName);
			
			List<Activity.Trade> activityList = Sheet.extractSheet(docActivity, Activity.Trade.class, sheetName);
			
			for(Activity.Trade activity: activityList) {
				// Sanity check
				if (activity.settlementDate == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.tradeDate == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.securityCode == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.symbol == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.transaction == null) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.quantity <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.unitPrice <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.price <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.tax < 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.fee < 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.other < 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.subTotalPrice <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.fxRate <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.feeJP == 0 && activity.consumptionTaxJP == 0) {
					// If both have zero, it is OK.
				} else {
					if (activity.feeJP <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
					if (activity.consumptionTaxJP <= 0) {
						logger.error("Unexpected  {}", activity);
						throw new UnexpectedException("Unexpected");
					}
				}
				if (activity.withholdingTaxJP < 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.total <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
				if (activity.totalJPY <= 0) {
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}

				switch (activity.transaction) {
				case Activity.Trade.TRANSACTION_BUY: {
					double fee = DoubleUtil.round(activity.total - activity.price, 2);
					transactionList.add(Transaction.buy(activity.settlementDate, activity.symbol, activity.quantity,
							activity.unitPrice, fee, activity.total));
				}
					break;
				case Activity.Trade.TRANSACTION_SELL: {
					double fee = DoubleUtil.round(activity.price - activity.total, 2);
					transactionList.add(Transaction.sell(activity.settlementDate, activity.symbol, activity.quantity,
							activity.unitPrice, fee, activity.total));
				}
					break;
				default:
					logger.error("Unexpected  {}", activity);
					throw new UnexpectedException("Unexpected");
				}
			}
		}

		Collections.sort(transactionList);
		return transactionList;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		String url = URL_ACTIVITY;
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			List<Transaction> transactionList = getTransactionList(docActivity);
			for(Transaction transaction: transactionList) {
				logger.info("{}", transaction);
			}
			logger.info("transactionList {}", transactionList.size());
		}
		logger.info("STOP");
		System.exit(0);
	}
}
