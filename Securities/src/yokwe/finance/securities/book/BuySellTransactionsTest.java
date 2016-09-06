package yokwe.finance.securities.book;

import java.util.List;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.book.Securities.Map;

@SheetData.SheetName("Buy-Sell-Transactions-TEST")
public class BuySellTransactionsTest extends SheetData {	
	@ColumnName("YYYY-MM")
	String yyyyMM;
	@ColumnName("Page")
	String page;
	@ColumnName("Transaction")
	String transaction;
	@ColumnName("Date")
	String date;
	@ColumnName("TradeDate")
	String tradeDate;
	@ColumnName("Symbol")
	String symbol;
	@ColumnName("Name")
	String name;
	@ColumnName("Quantity")
	double quantity;
	@ColumnName("Price")
	double price;
	@ColumnName("Commission")
	double commission;
	@ColumnName("Debit")
	double debit;
	@ColumnName("Credit")
	double credit;
	
	public String toString() {
		return String.format("%s %s %-8s %s %s %-8s %-60s %7.2f %8.4f %5.2f %8.2f %8.2f",
				yyyyMM, page, transaction, date, tradeDate, symbol, name, quantity, price, commission, debit, credit);
	}
	
	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		Map securitiesMap = new Map();
		
		try (LibreOffice libreOffice = new LibreOffice(url)) {
			List<BuySellTransactionsTest> transactionList = SheetData.getInstance(libreOffice, BuySellTransactionsTest.class);
			for(BuySellTransactionsTest transaction: transactionList) {
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
		}
		logger.info("STOP");
		System.exit(0);
	}

}
