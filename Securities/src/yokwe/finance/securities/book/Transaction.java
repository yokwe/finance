package yokwe.finance.securities.book;

import java.util.List;

import org.slf4j.LoggerFactory;

@SheetData.SheetName("売買履歴")
public class Transaction extends SheetData {	
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);

	@ColumnName("取引明細書")
	String statement;
	@ColumnName("頁")
	String page;
	@ColumnName("受渡日")
	String settlementDate;
	@ColumnName("約定日")
	String tradeDate;
	@ColumnName("銘柄コード")
	String symbol;
	@ColumnName("銘柄")
	String name;
	@ColumnName("株数")
	int    quantity;
	@ColumnName("買値")
	double priceBuy;
	@ColumnName("売値")
	double priceSell;
	@ColumnName("手数料")
	double commission;
	@ColumnName("借方")
	double debit;
	@ColumnName("貸方")
	double credit;
	@ColumnName("為替レート")
	double usdjpy;
	
	public String toString() {
		return String.format("%s %-8s %8.4f %8.4f %4d %8.2f %8.2f %8.2f %8.2f", tradeDate, symbol, priceBuy, priceSell, quantity, commission, debit, credit, usdjpy);
	}

	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		try (LibreOffice libreOffice = new LibreOffice(url)) {			
			List<Transaction> transactionList = SheetData.getInstance(libreOffice, Transaction.class);
			for(Transaction transaction: transactionList) {
				logger.info("{}", transaction);
			}
		}
		logger.info("STOP");
		System.exit(0);
	}
}
