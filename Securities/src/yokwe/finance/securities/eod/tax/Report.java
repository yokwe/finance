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

import yokwe.finance.securities.eod.ForexUtil;
import yokwe.finance.securities.eod.Market;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Dividend;
import yokwe.finance.securities.tax.Interest;

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

		for(Transaction transaction: transactionList) {
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
				// Special case for TAL/TRTN(negative quantity for BUY)
				if (buySell.isAlmostZero()) {
					ret.remove(key);
				}
			}
			if (transaction.type == Transaction.Type.SELL) {
				String key = transaction.symbol;
				BuySell buySell;
				if (ret.containsKey(key)) {
					buySell = ret.get(key);
				} else {
					buySell = new BuySell(transaction.symbol, transaction.name);
					ret.put(key, buySell);
				}
				
				buySell.sell(transaction);
			}
		}
		
		return ret;
	}
	
	static void addDummySell(Map<String, BuySell> buySellMap) {
		String theDate = "9999-12-31";
		String lastTradingDate = Market.getLastTradingDate().toString();
		for(BuySell buySell: buySellMap.values()) {
			if (buySell.isAlmostZero()) continue;
			
			String symbol = buySell.symbol;
			String name   = buySell.name;
			double quantity = buySell.totalQuantity;
			
			// Get latest price of symbol
//			logger.info("symbol {}", buySell.symbol);
			double price = PriceUtil.getClose(symbol, lastTradingDate);
			logger.info("price {}", price);

			// Add dummy sell record
			Transaction transaction = Transaction.sell(theDate, symbol, name, quantity, price, 5, quantity * price);	
			buySell.sell(transaction);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();

			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			
			// key is date
			Map<String, Dividend> dividendMap = getDividendMap(transactionList);
			logger.info("dividendMap size = {}", dividendMap.size());

			// key is date
			Map<String, Interest> interestMap = getInterestMap(transactionList);
			
			// key is symbol
			Map<String, BuySell> buySellMap = getBuySellMap(transactionList);
			addDummySell(buySellMap);

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
