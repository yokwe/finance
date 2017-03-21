package yokwe.finance.securities.eod.report;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.DateMap;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Activity;
import yokwe.finance.securities.tax.Price;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_%s.ods", TIMESTAMP);

	private static void readActivity(SpreadSheet docActivity, List<Transaction> transactionList, DateMap<List<Stock.Position>> positionMap) {
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
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					double quantity = activity.quantity;
					double total    = DoubleUtil.round((activity.price * activity.quantity) + activity.commission, 2);
					
					Stock.buy(date, symbol, quantity, total);
					Transaction transaction = Transaction.buy(date, symbol, quantity, total);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					//
					positionMap.put(date, Stock.getPositionList());
					break;
				}
				case "SOLD":
				case "REDEEMED": {
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					double quantity = activity.quantity;
					double total    = DoubleUtil.round((activity.price * activity.quantity) - activity.commission, 2);

					double sellCost = Stock.sell(date, symbol, quantity, total);
					Transaction transaction = Transaction.sell(date, symbol, quantity, total, sellCost);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					//
					positionMap.put(date, Stock.getPositionList());
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
	}
	
	private static void addDummySellTransaction(List<Transaction> transactionList) {
		for(Stock stock: Stock.getMap().values()) {
			if (stock.totalQuantity == 0) continue;
			logger.info("Stock  {}", stock);
			
			String date       = "9999-12-31";
			String symbol     = stock.symbol;
			double price      = Price.getLastPrice(symbol).close;
			double quantity   = stock.totalQuantity;
			double commission = 5;
			double total      = DoubleUtil.round((price * quantity) - commission, 2);

			double sellCost = Stock.sell(date, symbol, quantity, total);
			Transaction transaction = Transaction.sell(date, symbol, quantity, total, sellCost);
			logger.info("transaction {}", transaction);
			transactionList.add(transaction);
		}
		
		// Save cache for later use
		Price.saveCache();
	}
	
	private static void buildCostMap(List<Transaction> transactionList, DateMap<Double> costMap) {
		double stockTotal = 0;
		
		for(Transaction transaction: transactionList) {
			switch(transaction.type) {
			case WIRE_IN:
			case WIRE_OUT:
			case ACH_IN:
			case ACH_OUT:
			case INTEREST:
			case DIVIDEND:
				break;
			case BUY:
				stockTotal = DoubleUtil.round(stockTotal + transaction.debit, 2);
				costMap.put(transaction.date, stockTotal);
				break;
			case SELL:
				stockTotal = DoubleUtil.round(stockTotal - transaction.sellCost, 2);
				costMap.put(transaction.date, stockTotal);
				break;
			default:
				logger.error("Unknown transaction type {}", transaction.type);
				throw new SecuritiesException("Unknown transaction type");
			}
		}
	}
	
	private static void buildAccountList(List<Transaction> transactionList, List<Account> accountList) {
		double fundTotal  = 0;
		double cashTotal  = 0;
		double stockTotal = 0;
		
		for(Transaction transaction: transactionList) {
			Account account = new Account();
			
			switch(transaction.type) {
			case WIRE_IN:
				account.wireIn = transaction.credit;
				
				fundTotal = DoubleUtil.round(fundTotal + account.wireIn, 2);
				cashTotal = DoubleUtil.round(cashTotal + account.wireIn, 2);
				break;
			case WIRE_OUT:
				account.wireOut = transaction.debit;
				
				fundTotal = DoubleUtil.round(fundTotal - account.wireOut, 2);
				cashTotal = DoubleUtil.round(cashTotal - account.wireOut, 2);
				break;
			case ACH_IN:
				account.achIn = transaction.credit;
				
				fundTotal = DoubleUtil.round(fundTotal + account.achIn, 2);
				cashTotal = DoubleUtil.round(cashTotal + account.achIn, 2);
				break;
			case ACH_OUT:
				account.achOut = transaction.debit;
				
				fundTotal = DoubleUtil.round(fundTotal - account.achOut, 2);
				cashTotal = DoubleUtil.round(cashTotal - account.achOut, 2);
				break;
			case INTEREST:
				account.interest = transaction.credit;
				
				cashTotal = DoubleUtil.round(cashTotal + account.interest, 2);
				break;
			case DIVIDEND:
				account.dividend = transaction.credit - transaction.debit;
				account.symbol   = transaction.symbol;
				
				cashTotal = DoubleUtil.round(cashTotal + account.dividend, 2);
				break;
			case BUY:
				account.buy    = transaction.debit;
				account.symbol = transaction.symbol;
				
				cashTotal  = DoubleUtil.round(cashTotal  - account.buy, 2);
				stockTotal = DoubleUtil.round(stockTotal + account.buy, 2);
				break;
			case SELL:
				account.sell     = transaction.credit;
				account.symbol   = transaction.symbol;
				account.sellCost = transaction.sellCost;
				account.sellGain = account.sell - account.sellCost;
				
				cashTotal  = DoubleUtil.round(cashTotal  + account.sell, 2);
				stockTotal = DoubleUtil.round(stockTotal - transaction.sellCost, 2);
				break;
			default:
				logger.error("Unknown transaction type {}", transaction.type);
				throw new SecuritiesException("Unknown transaction type");
			}
			
			account.date       = transaction.date;
			account.fundTotal  = fundTotal;
			account.cashTotal  = cashTotal;
			account.stockTotal = stockTotal;
			account.gainTotal  = cashTotal + stockTotal - fundTotal;
			accountList.add(new Account(account));
			
			logger.info("account {}", account);
		}
	}

	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			List<Transaction> transactionList = new ArrayList<>();
			DateMap<List<Stock.Position>> positionMap = new DateMap<>();
			
			// Build transactionList
			readActivity(docActivity, transactionList, positionMap);
			addDummySellTransaction(transactionList);
			
			// Sort transactionList
			Collections.sort(transactionList);

			// Build costMap
			DateMap<Double> costMap = new DateMap<>();
			buildCostMap(transactionList, costMap);

			// Build accountList
			List<Account> accountList = new ArrayList<>();
			buildAccountList(transactionList, accountList);


			// Save accountList
			{
				String sheetName = Sheet.getSheetName(Account.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, accountList);
				
				String newSheetName = String.format("%s-%s",  "9999", "detail");
				logger.info("sheet {}", newSheetName);
				docSave.renameSheet(sheetName, newSheetName);
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
