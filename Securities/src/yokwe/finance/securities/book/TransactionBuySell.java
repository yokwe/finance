package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.List;

import yokwe.finance.securities.SecuritiesException;

@SheetData.SheetName("Transaction-Buy-Sell")
public class TransactionBuySell extends SheetData {	
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
	
	static TransactionBuySell getTestInstance(String transaction, String tradeDate, String symbol, double quantity, double price, double commission) {
		TransactionBuySell ret = new TransactionBuySell();
		ret.yyyyMM      = "2000-01";
		ret.page        = "1";
		ret.transaction = transaction;
		ret.tradeDate   = tradeDate;
		ret.symbol      = symbol;
		ret.quantity    = quantity;
		ret.price       = price;
		ret.commission  = commission;
		ret.debit       = transaction.equals("BOUGHT") ? (quantity * price) + commission : 0;
		ret.credit      = transaction.equals("SOLD")   ? (quantity * price) - commission : 0;
		return ret;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		// See page below
		//   https://www.nta.go.jp/taxanswer/shotoku/1466.htm
		//   2001-09-01  Acquisition cost = 815 x 3,000 = 2,445,000
		//   2002-07-01  Acquisition cost = 846 x 6,000 = 5,076,000
		List<TransactionBuySell> transactionList = new ArrayList<>();
		transactionList.add(getTestInstance("BOUGHT", "2001-05-01", "AAA", 5000, 800, 0));
		transactionList.add(getTestInstance("BOUGHT", "2001-08-01", "AAA", 2000, 850, 0));
		transactionList.add(getTestInstance("SOLD",   "2001-09-01", "AAA", 3000, 900, 0));
		transactionList.add(getTestInstance("BOUGHT", "2002-03-01", "AAA", 5000, 870, 0));
		transactionList.add(getTestInstance("SOLD",   "2002-07-01", "AAA", 6000, 950, 0));

		List<Report> reportList = new ArrayList<>();
		
		for(TransactionBuySell transaction: transactionList) {
			double usdjpy = 1.0;
			String symbolName = "";
			switch (transaction.transaction) {
			case "BOUGHT": {
				Securities.buy(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportList);
				break;
			}
			case "SOLD": {
				Securities.sell(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportList);
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
