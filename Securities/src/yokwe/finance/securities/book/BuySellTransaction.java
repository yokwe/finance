package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.List;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.book.Securities.Map;

@SheetData.SheetName("Buy-Sell-Transaction")
public class BuySellTransaction extends SheetData {	
	@ColumnName("YYYY-MM")
	public String yyyyMM;
	@ColumnName("Page")
	public String page;
	@ColumnName("Transaction")
	public String transaction;
	@ColumnName("Date")
	public String date;
	@ColumnName("TradeDate")
	public String tradeDate;
	@ColumnName("Symbol")
	public String symbol;
	@ColumnName("Name")
	public String name;
	@ColumnName("Quantity")
	public double quantity;
	@ColumnName("Price")
	public double price;
	@ColumnName("Commission")
	public double commission;
	@ColumnName("Debit")
	public double debit;
	@ColumnName("Credit")
	public double credit;
	
	public String toString() {
		return String.format("%s %s %-8s %s %s %-8s %-60s %7.2f %8.4f %5.2f %8.2f %8.2f",
				yyyyMM, page, transaction, date, tradeDate, symbol, name, quantity, price, commission, debit, credit);
	}
	
	static BuySellTransaction getTestInstance(String transaction, String tradeDate, String symbol, double quantity, double price) {
		BuySellTransaction ret = new BuySellTransaction();
		ret.yyyyMM      = "2000-01";
		ret.page        = "1";
		ret.transaction = transaction;
		ret.tradeDate   = tradeDate;
		ret.symbol      = symbol;
		ret.quantity    = quantity;
		ret.price       = price;
		ret.debit       = transaction.equals("BOUGHT") ? quantity * price : 0;
		ret.credit      = transaction.equals("SOLD") ? quantity * price : 0;
		return ret;
	}
	static List<BuySellTransaction> getTestData() {
		List<BuySellTransaction> ret = new ArrayList<>();
		ret.add(getTestInstance("BOUGHT", "2001-05-01", "AAA", 5000, 800));
		ret.add(getTestInstance("BOUGHT", "2001-08-01", "AAA", 2000, 850));
		ret.add(getTestInstance("SOLD",   "2001-09-01", "AAA", 3000, 900));
		ret.add(getTestInstance("BOUGHT", "2002-03-01", "AAA", 5000, 870));
		ret.add(getTestInstance("SOLD",   "2001-09-01", "AAA", 6000, 950));
		return ret;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		Map securitiesMap = new Map();
		
		List<BuySellTransaction> transactionList = getTestData();
		for(BuySellTransaction transaction: transactionList) {
			switch (transaction.transaction) {
			case "BOUGHT": {
				securitiesMap.buy(transaction.symbol, transaction.quantity, transaction.tradeDate, (int)Math.round(transaction.debit));
				break;
			}
			case "SOLD": {
				securitiesMap.sell(transaction.symbol, transaction.quantity, transaction.tradeDate, (int)Math.round(transaction.credit));
				break;
			}
			default: {
				logger.error("Unknown transaction = {}", transaction.transaction);
				throw new SecuritiesException("Unexpected");
			}
			}
		}
		logger.info("STOP");
		System.exit(0);
	}
}
