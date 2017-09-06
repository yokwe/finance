package yokwe.finance.securities.eod.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.DateMap;
import yokwe.finance.securities.eod.Market;
import yokwe.finance.securities.eod.UpdateProvider;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/EOD_REPORT_%s.ods", TIMESTAMP);
	
	private static List<Account> getAccountList(List<Transaction> transactionList) {
		List<Account> accountList = new ArrayList<>();
		
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
			case CHANGE:
				// Nothing is changed for account
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
			
//			logger.info("account {}", account);
		}
		
		return accountList;
	}

	private static List<StockGain> getStockGainList(List<Transaction> transactionList) {
		Map<String, StockGain>  stockGainMap = new TreeMap<>();
		DateMap<List<Position>> positionMap  = new DateMap<>();
		
		{
			double stockTotal = 0;
			double gainTotal  = 0;
			
			for(Transaction transaction: transactionList) {
				String date = transaction.date;
				
				switch(transaction.type) {
				case WIRE_IN:
				case WIRE_OUT:
				case ACH_IN:
				case ACH_OUT:
				case INTEREST:
				case DIVIDEND:
					break;
				case CHANGE:
					positionMap.put(date, transaction.positionList);
					break;
				case BUY: {
					positionMap.put(date, transaction.positionList);

					StockGain stockGain;
					if (stockGainMap.containsKey(date)) {
						stockGain = stockGainMap.get(date);
					} else {
						stockGain = new StockGain(date);
						stockGainMap.put(date, stockGain);
					}
					
					double buy    = transaction.debit;
					
					stockTotal = DoubleUtil.round(stockTotal + buy, 2);
					
					stockGain.stock      = stockTotal;
//					stockGain.unreal
//					stockGain.unrealGain
					stockGain.buy        = DoubleUtil.round(stockGain.buy + buy, 2);
//					stockGain.sell
//					stockGain.sellGain
					stockGain.realGain   = gainTotal;
					
					logger.info("buy  {}", stockGain);
					break;
				}
				case SELL: {
					positionMap.put(date, transaction.positionList);

					StockGain stockGain;
					if (stockGainMap.containsKey(date)) {
						stockGain = stockGainMap.get(date);
					} else {
						stockGain = new StockGain(date);
						stockGainMap.put(date, stockGain);
					}

					double sell   = transaction.credit;
					double cost   = transaction.sellCost;
					double gain   = sell - cost;

					stockTotal = DoubleUtil.round(stockTotal - cost, 2);
					gainTotal  = DoubleUtil.round(gainTotal + gain, 2);

					stockGain.stock      = stockTotal;
//					stockGain.unreal
//					stockGain.unrealGain
//					stockGain.buy
					stockGain.sell       = DoubleUtil.round(stockGain.sell + sell, 2);
					stockGain.sellGain   = DoubleUtil.round(stockGain.sellGain + gain, 2);
					stockGain.realGain   = gainTotal;

					logger.info("sell {}", stockGain);
					break;
				}
				default:
					logger.error("Unknown transaction type {}", transaction.type);
					throw new SecuritiesException("Unknown transaction type");
				}
			}
		}
		
		// Build stockGainList for last 3 month from stockGainMap
		List<StockGain> stockGainList = new ArrayList<>();
		{
			StockGain stockGain = null;
			LocalDate last = UpdateProvider.DATE_LAST;
			
			for(LocalDate date = UpdateProvider.DATE_FIRST; date.isBefore(last) || date.isEqual(last); date = date.plusDays(1)) {
				if (Market.isClosed(date)) continue;
				
				double unreal = Position.getValue(date.toString(), positionMap.get(date));
				
				if (stockGainMap.containsKey(date.toString())) {
					stockGain = new StockGain(stockGainMap.get(date.toString()));
				} else {
					if (stockGain == null) continue;
					
					// Before reuse, clear some fields.
					stockGain.buy      = 0;
					stockGain.sell     = 0;
					stockGain.sellGain = 0;
				}
				
				stockGain.date       = date.toString();
				
				if (unreal != Position.NO_VALUE) {
					stockGain.unreal     = unreal;
					stockGain.unrealGain = stockGain.unreal - stockGain.stock;
					stockGain.totalGain  = stockGain.unrealGain + stockGain.realGain;
				} else {
					stockGain.unreal     = 0;
					stockGain.unrealGain = 0;
					stockGain.totalGain  = 0;
				}

				stockGainList.add(new StockGain(stockGain));
			}
		}
		return stockGainList;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);

			// Build accountList
			List<Account> accountList = getAccountList(transactionList);

			List<StockGain> stockGainList = getStockGainList(transactionList);
			
			// Save accountList
			{
				String sheetName = Sheet.getSheetName(Account.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, accountList);
				
				String newSheetName = String.format("%s-%s",  "9999", "detail");
				logger.info("sheet {}", newSheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}
						
			// Save stockGainList
			{
				String sheetName = Sheet.getSheetName(StockGain.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, stockGainList);
				
				String newSheetName = String.format("%s-%s",  "9999", "stockGain");
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
