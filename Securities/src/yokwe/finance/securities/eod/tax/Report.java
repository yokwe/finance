package yokwe.finance.securities.eod.tax;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.ForexUtil;
import yokwe.finance.securities.eod.Market;
import yokwe.finance.securities.eod.report.Stock;
import yokwe.finance.securities.eod.report.Transaction;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Activity;
import yokwe.finance.securities.tax.Dividend;
import yokwe.finance.securities.tax.Interest;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TAX_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/EOD_TAX_%s.ods", TIMESTAMP);

	private static List<Transaction> getTransactionList(SpreadSheet docActivity) {
		List<Transaction> transactionList = new ArrayList<>();
		
		List<String> sheetNameList = docActivity.getSheetNameList();
		sheetNameList.sort((a, b) -> a.compareTo(b));
		
		for(String sheetName: sheetNameList) {
			if (!sheetName.matches("^20[0-9][0-9]$")) {
				logger.warn("Sheet {} skip", sheetName);
				continue;
			}
			logger.info("Sheet {}", sheetName);
			
			List<Activity> activityList = Sheet.extractSheet(docActivity, Activity.class, sheetName);
			// Need to sort activityList to adjust item order of activityList
			Collections.sort(activityList);
			
			for(Iterator<Activity> iterator = activityList.iterator(); iterator.hasNext();) {
				Activity activity = iterator.next();
				
				// Sanity check
				{
					if (activity.date != null && 0 < activity.date.length()) {
						String date = activity.date;
						if (Market.isClosed(date)) {
							logger.error("Market is closed - date -  {}", activity);
							throw new SecuritiesException("Market is closed");
						}
					}
					if (activity.tradeDate != null && 0 < activity.tradeDate.length()) {
						String date = activity.tradeDate;
						if (Market.isClosed(date)) {
							logger.error("Market is closed - tradeDate -  {}", activity);
							throw new SecuritiesException("Market is closed");
						}
					}
				}
				switch(activity.transaction) {
				// TODO temporary ignore old "NAME CHG"
				case "NAME CHG": {
					break;
				}
				// TODO use "NAME CHG" and "MERGER" after everything works as expected.
				case "*NAME CHG":
				case "*MERGER": {
					Activity nextActivity = iterator.next();
					if (nextActivity.date.equals(activity.date) && nextActivity.transaction.equals(activity.transaction)) {
						String date        = activity.date;
						String newSymbol   = activity.symbol;
						String newName     = activity.name;
						double newQuantity = activity.quantity;
						
						String symbol      = nextActivity.symbol;
						String name        = nextActivity.name;
						double quantity    = nextActivity.quantity;
						
						Stock.change(date, symbol, quantity, newSymbol, newQuantity);
						
						Transaction transaction = Transaction.change(date, symbol, name, quantity, newSymbol, newName, newQuantity, Stock.getPositionList());
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
					} else {
						logger.error("Unexpect transaction  {}  {}", activity.transaction, nextActivity);
						logger.error("activity  {}", activity);
						logger.error("next      {}", nextActivity);
						throw new SecuritiesException("Unexpected");
					}
					
					break;
				}
				case "BOUGHT": {
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = activity.quantity;
					double total    = DoubleUtil.round((activity.price * activity.quantity) + activity.commission, 2);
					
					Stock.buy(date, symbol, quantity, total);
					Transaction transaction = Transaction.buy(date, symbol, name, quantity, total, Stock.getPositionList());
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "SOLD":
				case "REDEEMED": {
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = activity.quantity;
					double total    = DoubleUtil.round((activity.price * activity.quantity) - activity.commission, 2);

					double sellCost = Stock.sell(date, symbol, quantity, total);
					Transaction transaction = Transaction.sell(date, symbol, name, quantity, total, sellCost, Stock.getPositionList());
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
					String name     = activity.name;
					double quantity = activity.quantity;
					double debit    = activity.debit;
					double credit   = activity.credit;

					Transaction transaction = Transaction.dividend(date, symbol, name, quantity, debit, credit);
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
		return transactionList;
	}
	
	static Map<String, Interest> getInterestMap(List<Transaction> transactionList) {
		Map<String, Interest> ret = new TreeMap<>();
		
		for(Transaction transaction: transactionList) {
			if (transaction.type == Transaction.Type.INTEREST) {
				String date = transaction.date;
				double interest = transaction.credit;
				double fxRate = ForexUtil.getUSD(transaction.date);
				
				ret.put(date, new Interest(date, interest, fxRate));
			}
		}
		return ret;
	}
	static Map<String, Dividend> getDividendMap(List<Transaction> transactionList) {
		Map<String, Dividend> ret = new TreeMap<>();
		
		for(Transaction transaction: transactionList) {
			if (transaction.type == Transaction.Type.DIVIDEND) {
				String key = String.format("%s-%s", transaction.date, transaction.symbol);
				if (ret.containsKey(key)) {
					Dividend dividend = ret.get(key);
					dividend.update(transaction.credit, transaction.debit);
				} else {
					double fxRate = ForexUtil.getUSD(transaction.date);

					Dividend dividend = Dividend.getInstance(transaction.date, transaction.symbol, transaction.name, transaction.quantity,
							transaction.credit, transaction.debit, fxRate);
					ret.put(key, dividend);
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			List<Transaction> transactionList = getTransactionList(docActivity);
			
			// key is date
			Map<String, Dividend> dividendMap = getDividendMap(transactionList);
			logger.info("dividendMap size = {}", dividendMap.size());

			// key is date
			Map<String, Interest> interestMap = getInterestMap(transactionList);

			SortedSet<String> yearSet = new TreeSet<>();

			yearSet.addAll(interestMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));

			for(String targetYear: yearSet) {
				// Dividend
				{
					List<Dividend> dividendList = new ArrayList<>();
					for(String key: dividendMap.keySet()) {
						if (key.startsWith(targetYear)) dividendList.add(dividendMap.get(key));
					}

					if (!dividendList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Dividend.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, dividendList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
				
				// Interest
				{
					List<Interest> interestList = new ArrayList<>();
					for(String key: interestMap.keySet()) {
						if (key.startsWith(targetYear)) interestList.add(interestMap.get(key));
					}

					if (!interestList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Interest.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, interestList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
			}
			
			// remove first sheet
			docSave.removeSheet(docSave.getSheetName(0));

			docSave.store(URL_REPORT);
			logger.info("output {}", URL_REPORT);
			docLoad.close();
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
