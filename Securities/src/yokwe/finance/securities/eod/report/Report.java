package yokwe.finance.securities.eod.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Activity;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_%s.ods", TIMESTAMP);

	// Create daily profit report
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			List<Transaction> transactionList = new ArrayList<>();
			
			List<String> sheetNameList = docActivity.getSheetNameList();
			sheetNameList.sort((a, b) -> a.compareTo(b));
			for(String sheetName: sheetNameList) {
				if (!sheetName.matches("^20[0-9][0-9]$")) {
					logger.warn("Sheet {} skip", sheetName);
					continue;
				}
				logger.info("Sheet {}", sheetName);
				for(Activity activity: Sheet.extractSheet(docActivity, Activity.class, sheetName)) {
					switch(activity.transaction) {
					case "BOUGHT":
					case "NAME CHG": {
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						String date     = activity.date;
						String symbol   = activity.symbol;
						double quantity = activity.quantity;
						double total    = DoubleUtil.round((activity.price * activity.quantity) + activity.commission, 2);
						
						Stock.buy(date, symbol, quantity, total);
						Transaction transaction = Transaction.buy(date, symbol, quantity, total);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "SOLD":
					case "REDEEMED": {
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						String date     = activity.date;
						String symbol   = activity.symbol;
						double quantity = activity.quantity;
						double total    = DoubleUtil.round((activity.price * activity.quantity) - activity.commission, 2);

						double sellCost = Stock.sell(date, symbol, quantity, total);
						Transaction transaction = Transaction.sell(date, symbol, quantity, total, sellCost);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "INTEREST": {
						String date     = activity.date;
						double credit   = activity.credit;

						Transaction transaction = Transaction.interest(date, credit);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "DIVIDEND":
					case "ADR":
					case "MLP":
					case "NRA":
					case "CAP GAIN": {
						String date     = activity.date;
						String symbol   = activity.symbol;
						double debit    = activity.debit;
						double credit   = activity.credit;

						Transaction transaction = Transaction.dividend(date, symbol, debit, credit);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "ACH": {
						String date     = activity.date;
						double debit    = activity.debit;
						double credit   = activity.credit;
						
						if (debit != 0) {
							Transaction transaction = Transaction.achOut(date, debit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						if (credit != 0) {
							Transaction transaction = Transaction.achIn(date, credit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						break;
					}
					case "WIRE": {
						String date     = activity.date;
						double debit    = activity.debit;
						double credit   = activity.credit;
						
						if (debit != 0) {
							Transaction transaction = Transaction.wireOut(date, debit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						if (credit != 0) {
							Transaction transaction = Transaction.wireIn(date, credit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						break;
					}
					default:
						logger.error("Unknown transaction {}", activity.transaction);
						throw new SecuritiesException("Unknown transaction");
					}
				}
			}
			
			// Current positions
			{
				Map<String, Stock> stockMap = Stock.getMap();
				
				logger.info("stockMap {}", stockMap.size());
				for(Stock stock: stockMap.values()) {
					if (stock.totalQuantity == 0) continue;
					logger.info("Stock  {}", stock);
				}
			}
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
