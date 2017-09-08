package yokwe.finance.securities.eod.tax;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.ForexUtil;
import yokwe.finance.securities.eod.Price;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Dividend;
import yokwe.finance.securities.tax.Interest;
import yokwe.finance.securities.tax.Transfer;
import yokwe.finance.securities.tax.TransferDetail;
import yokwe.finance.securities.tax.TransferSummary;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TAX_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/EOD_TAX_%s.ods", TIMESTAMP);

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
	static Map<String, BuySell>  getBuySellMap(List<Transaction> transactionList) {
		Map<String, BuySell> ret = new TreeMap<>();
		
		// To calculate correct Japanese tax,
		// If buy and sell happen in same day, treat as all buy first then sell
		// Order of transaction should be change, buy and sell per stock for one day
		List<Transaction> reorderedTransactionList = new ArrayList<>();
		{
			String lastDate   = "*NA*";
			String lastSymbol = "*NA*";
			List<Transaction> buyList    = new ArrayList<>();
			List<Transaction> sellList   = new ArrayList<>();
			List<Transaction> changeList = new ArrayList<>();
			
			for(Transaction transaction: transactionList) {
				String date   = transaction.date;
				String symbol = transaction.symbol;
				
				if (!date.equals(lastDate) || !symbol.equals(lastSymbol)) {
					reorderedTransactionList.addAll(changeList);
					reorderedTransactionList.addAll(buyList);
					reorderedTransactionList.addAll(sellList);
					
					buyList.clear();
					sellList.clear();
					changeList.clear();
				}

				switch(transaction.type) {
				case BUY:
					buyList.add(transaction);
					break;
				case SELL:
					sellList.add(transaction);
					break;
				case CHANGE:
					changeList.add(transaction);
					break;
				default:
					break;
				}
				
				lastDate = date;
				lastSymbol = symbol;
			}
			
			reorderedTransactionList.addAll(buyList);
			reorderedTransactionList.addAll(sellList);
			reorderedTransactionList.addAll(changeList);
		}

		for(Transaction transaction: reorderedTransactionList) {
			if (transaction.type == Transaction.Type.BUY) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					buySell = new BuySell(transaction.symbol, transaction.name);
					ret.put(key, buySell);
				}
				buySell.buy(transaction);
			}
			if (transaction.type == Transaction.Type.SELL) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new SecuritiesException("Unexpected");
				}
				
				buySell.sell(transaction);
			}
			if (transaction.type == Transaction.Type.CHANGE) {
				String key = transaction.symbol;
				
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					logger.error("Unknonw symbol {}", key);
					throw new SecuritiesException("Unexpected");
				}
				ret.remove(key);
				
				buySell.change(transaction);
				ret.put(buySell.symbol, buySell);
			}
		}
		
		return ret;
	}
	
	static void addDummySell(Map<String, BuySell> buySellMap) {
		double fee = 5.0;
		String theDate = "9999-12-31";
		for(BuySell buySell: buySellMap.values()) {
			if (buySell.isAlmostZero()) continue;
			
			String symbol   = buySell.symbol;
			String name     = buySell.name;
			double quantity = buySell.totalQuantity;
			
			// Get latest price of symbol
			Price price = PriceUtil.getLastPrice(symbol);
//			logger.info("price {} {} {}", price.date, price.symbol, price.close);

			// Add dummy sell record
			Transaction transaction = Transaction.sell(theDate, symbol, name, quantity, price.close, fee, Transaction.roundPrice(quantity * price.close));	
			buySell.sell(transaction);
		}
	}
	
	static Map<String, TransferSummary> getSummaryMap(Map<String, BuySell> buySellMap) {
		Map<String, TransferSummary> ret = new TreeMap<>();
		
		for(BuySell buySell: buySellMap.values()) {
			String symbol = buySell.symbol;
			for(List<Transfer> pastTransferList: buySell.past) {
				Transfer lastTransfer = pastTransferList.get(pastTransferList.size() - 1);
				if (lastTransfer.sell == null) {
					logger.error("lastTransfer is null");
					throw new SecuritiesException("lastTransfer is null");
				}
				String key = String.format("%s-%s", lastTransfer.sell.date, symbol);
				ret.put(key, new TransferSummary(lastTransfer.sell));
			}
		}
		return ret;
	}
	static Map<String, List<TransferDetail>> getDetailMap(Map<String, BuySell> buySellMap) {
		Map<String, List<TransferDetail>> ret = new TreeMap<>();
		
		for(BuySell buySell: buySellMap.values()) {
			String symbol = buySell.symbol;
			for(List<Transfer> pastTransferList: buySell.past) {
				Transfer lastTransfer = pastTransferList.get(pastTransferList.size() - 1);
				if (lastTransfer.sell == null) {
					logger.error("lastTransfer is null");
					throw new SecuritiesException("lastTransfer is null");
				}
				String key = String.format("%s-%s", lastTransfer.sell.date, symbol);
				
				List<TransferDetail> detailList = new ArrayList<>();
				for(Transfer transfer: pastTransferList) {
					detailList.add(TransferDetail.getInstance(transfer));
				}
				ret.put(key, detailList);
			}
		}
		return ret;
	}

	public static void generateReport(String url) {
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			
			// key is date
			Map<String, Dividend> dividendMap = getDividendMap(transactionList);

			// key is date
			Map<String, Interest> interestMap = getInterestMap(transactionList);
			
			// key is symbol
			Map<String, BuySell> buySellMap = getBuySellMap(transactionList);
			addDummySell(buySellMap);
			
			// key is date-symbol
			Map<String, TransferSummary> summaryMap = getSummaryMap(buySellMap);
			Map<String, List<TransferDetail>> detailMap = getDetailMap(buySellMap);


			SortedSet<String> yearSet = new TreeSet<>();

			yearSet.addAll(detailMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));
			yearSet.addAll(dividendMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));
			yearSet.addAll(interestMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));

			for(String targetYear: yearSet) {
				// Detail
				{
					Map<String, List<TransferDetail>> workMap = new TreeMap<>();
					for(String key: detailMap.keySet()) {
						if (!key.startsWith(targetYear)) continue;
						
						List<TransferDetail> aList = detailMap.get(key);
						if (aList.isEmpty()) continue;
						
						String symbol = aList.get(0).symbol;
						if (!workMap.containsKey(symbol)) {
							workMap.put(symbol, new ArrayList<>());
						}
						workMap.get(symbol).addAll(aList);
					}
					
					List<TransferDetail> transferList = new ArrayList<>();
					for(String key: workMap.keySet()) {
						transferList.addAll(workMap.get(key));
					}
					
					if (!transferList.isEmpty()) {
						String sheetName = Sheet.getSheetName(TransferDetail.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, transferList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}

				// Summary
				{
					List<TransferSummary> summaryList = new ArrayList<>();
					for(String key: summaryMap.keySet()) {
						if (key.startsWith(targetYear)) summaryList.add(summaryMap.get(key));
					}
					// Sort with symbol name and dateSell
					summaryList.sort((a, b) -> (a.symbol.equals(b.symbol)) ? a.dateSell.compareTo(b.dateSell) : a.symbol.compareTo(b.symbol));
					if (!summaryList.isEmpty()) {
						String sheetName = Sheet.getSheetName(TransferSummary.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, summaryList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
				
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
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		// For test
//		ForexUtil.enableTestMode();
//		generateReport(URL_ACTIVITY_TEST);
		
		// Not test
		generateReport(URL_ACTIVITY);

		logger.info("STOP");
		System.exit(0);
	}
}
