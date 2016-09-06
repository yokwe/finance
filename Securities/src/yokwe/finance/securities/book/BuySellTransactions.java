package yokwe.finance.securities.book;

import java.util.List;

@SheetData.SheetName("Buy-Sell-Transactions-TEST")
public class BuySellTransactions extends SheetData {	
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
		try (LibreOffice libreOffice = new LibreOffice(url)) {			
			List<BuySellTransactions> transactionList = SheetData.getInstance(libreOffice, BuySellTransactions.class);
			for(BuySellTransactions transaction: transactionList) {
				logger.info("{}", transaction);
			}
		}
		logger.info("STOP");
		System.exit(0);
	}

}
