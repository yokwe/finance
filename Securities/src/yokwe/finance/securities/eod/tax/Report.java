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
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_EOD_TAX.ods";
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
	
	static List<Account> getAccountList(List<Transaction> transactionList) {
		List<Account> ret = new ArrayList<>();
		
		double fundTotal  = 0;
		double cashTotal  = 0;
		double stockTotal = 0;
		double gainTotal  = 0;
		
		for(Transaction transaction: transactionList) {
			String date = transaction.date;
			
			switch(transaction.type) {
			case WIRE_IN:
			{
				double credit = transaction.credit;
				fundTotal += credit;
				cashTotal += credit;
				
				ret.add(Account.wireIn(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case WIRE_OUT:
			{
				double debit = transaction.debit;
				fundTotal -= debit;
				cashTotal -= debit;
				
				ret.add(Account.wireOut(date, fundTotal, cashTotal, stockTotal, gainTotal, debit));
			}
				break;
			case ACH_IN:
			{
				double credit = transaction.credit;
				fundTotal += credit;
				cashTotal += credit;
				
				ret.add(Account.achIn(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case ACH_OUT:
			{
				double debit = transaction.debit;
				fundTotal -= debit;
				cashTotal -= debit;
				
				ret.add(Account.achOut(date, fundTotal, cashTotal, stockTotal, gainTotal, debit));
			}
				break;
			case INTEREST:
			{
				double credit = transaction.credit;
				cashTotal += credit;
				gainTotal += credit;
				
				ret.add(Account.interest(date, fundTotal, cashTotal, stockTotal, gainTotal, credit));
			}
				break;
			case DIVIDEND:
			{
				String symbol = transaction.symbol;
				
				double debit  = transaction.debit;
				double credit = transaction.credit;
				
				if (!DoubleUtil.isAlmostZero(debit)) {
					cashTotal -= debit;
					gainTotal -= debit;

					ret.add(Account.dividend(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, -debit));
				}
				if (!DoubleUtil.isAlmostZero(credit)) {
					cashTotal += credit;
					gainTotal += credit;

					ret.add(Account.dividend(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, credit));
				}
			}
				break;
			case BUY:
			{
				String symbol     = transaction.symbol;
				Transfer transfer = Transfer.getByID(transaction.id);
				double   buy      = Transaction.roundPrice(transfer.buy.buy + transfer.buy.fee);

				cashTotal  -= buy;
				stockTotal += buy;
				ret.add(Account.buy(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, buy));
			}
				break;
			case SELL:
			{
				String symbol     = transaction.symbol;
				Transfer transfer = Transfer.getByID(transaction.id);
				double sell       = Transaction.roundPrice(transfer.sell.sell - transfer.sell.fee);
				double sellCost   = transfer.sell.cost;
				double sellGain   = Transaction.roundPrice(sell - sellCost);
				
				cashTotal  += sell;
				stockTotal -= sellCost;
				gainTotal  += sellGain;

				ret.add(Account.sell(date, fundTotal, cashTotal, stockTotal, gainTotal, symbol, sell, sellCost, sellGain));
			}
				break;
			case CHANGE:
				break;
			default:
				logger.error("Unknown type = {}", transaction.type);
				throw new SecuritiesException("Unexpected");
			}
		}
		return ret;
	}


	public static void generateReport(String url) {
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			
			// key is date
			Map<String, Dividend> dividendMap = getDividendMap(transactionList);

			// key is date
			Map<String, Interest> interestMap = getInterestMap(transactionList);
			
			// key is symbol
			Map<String, BuySell> buySellMap = BuySell.getBuySellMap(transactionList);
			
			// key is date-symbol
			Map<String, TransferSummary> summaryMap = getSummaryMap(buySellMap);
			Map<String, List<TransferDetail>> detailMap = getDetailMap(buySellMap);
			
			// key is date
			List<Account> accountList = getAccountList(transactionList);

			SortedSet<String> yearSet = new TreeSet<>();

			yearSet.addAll(detailMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));
			yearSet.addAll(dividendMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));
			yearSet.addAll(interestMap.keySet().stream().map(date -> date.substring(0, 4)).collect(Collectors.toSet()));

			for(String targetYear: yearSet) {
				// Account
				{
					List<Account> myList = new ArrayList<>();
					for(Account account: accountList) {
						if (account.date.startsWith(targetYear)) myList.add(account);
					}

					if (!myList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Account.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.fillSheet(docSave, myList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}

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
