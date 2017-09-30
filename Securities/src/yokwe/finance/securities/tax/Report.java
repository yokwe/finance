package yokwe.finance.securities.tax;

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
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.eod.ForexUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TEMPLATE_TAX_REPORT.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/Report/TAX_REPORT_%s.ods", TIMESTAMP);

	static class BuySell {
		private static final double ALMOST_ZERO = 0.000001;

		String symbol;
		String name;

		int    buyCount;
		String dateBuyFirst;
		String dateBuyLast;

		double totalQuantity;
		double totalCost;
		int    totalCostJPY;
		double totalDividend;
		
		List<Transfer>       current;
		List<List<Transfer>> past;
		
		public BuySell(String symbol, String name) {
			this.symbol = symbol;
			this.name   = name;

			current     = new ArrayList<>();
			past        = new ArrayList<>();

			reset();
		}
		
		void reset() {
			buyCount      = 0;
			dateBuyFirst  = "";
			dateBuyLast   = "";
			
			totalQuantity = 0;
			totalCost     = 0;
			totalCostJPY  = 0;
			totalDividend = 0;
			
			// TODO Is this correct/
//			if (0 < current.size()) {
//				past.add(current);
//				current = new ArrayList<>();
//			}
		}

		void buy(Activity activity, double fxRate) {
			buyCount++;
			if (buyCount == 1) {
				dateBuyFirst = activity.tradeDate;
			} else {
				dateBuyLast  = activity.tradeDate;
			}
			
			// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
			//double cost = DoubleUtil.round((activity.quantity * activity.price) + activity.commission, 2);
			double costPrice = DoubleUtil.round(activity.quantity * activity.price, 2);
			double costFee   = activity.commission;
			int costPriceJPY = (int)Math.floor(costPrice * fxRate);
			int costFeeJPY   = (int)Math.floor(costFee * fxRate);
			
			double cost    = costPrice + costFee;
			int    costJPY = costPriceJPY + costFeeJPY;
			
			totalQuantity += activity.quantity;
			totalCost     += cost;
			totalCostJPY  += costJPY;

			// special case for negative buy -- for TAL name change
			// TODO is this correct?
			if (isAlmostZero()) {
				reset();
				return;
			}
			
			Transfer.Buy buy = new Transfer.Buy(
				activity.tradeDate, activity.symbol, activity.name,
				activity.quantity, activity.price, activity.commission, fxRate,
				totalQuantity, totalCost, totalCostJPY
				);
			current.add(new Transfer(buy));
		}
		void sell(Activity activity, double fxRate) {
			double sell    = DoubleUtil.round(activity.price * activity.quantity, 2);
			int    sellJPY = (int)Math.floor(sell * fxRate);
			int    feeJPY  = (int)Math.floor(activity.commission * fxRate);

			double sellRatio = activity.quantity / totalQuantity;
			double cost      = DoubleUtil.round(totalCost * sellRatio, 2);
			int    costJPY;
			
			if (buyCount == 1) {
				costJPY = (int)Math.floor(totalCostJPY * sellRatio);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity -= activity.quantity;
				totalCost     -= cost;
				totalCostJPY  -= costJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, sellJPY, costJPY, feeJPY, dateBuyFirst, dateBuyLast));
			} else {
				double unitCostJPY = Math.ceil(totalCostJPY / totalQuantity); // need to be round up. See https://www.nta.go.jp/taxanswer/shotoku/1466.htm
				costJPY = (int)Math.floor(unitCostJPY * activity.quantity);
				// need to adjust totalAcquisitionCostJPY
				totalCostJPY = (int)Math.floor(unitCostJPY * totalQuantity);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity -= activity.quantity;
				totalCost     -= cost;
				totalCostJPY  -= costJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, sellJPY, totalCostJPY, feeJPY, dateBuyFirst, dateBuyLast));
			}

			// TODO How about buy 1 time and sell more than 1 time?
			if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
				// Special case buy one time and sell whole
				Transfer.Buy transferBuy = current.remove(0).buy;
				Transfer.Sell transferSell = new Transfer.Sell(
					activity.tradeDate, activity.symbol, activity.name,
					activity.quantity, activity.price, activity.commission, fxRate,
					cost, costJPY,
					dateBuyFirst, dateBuyLast, totalDividend,
					totalQuantity, totalCost, totalCostJPY
					);
				current.add(new Transfer(transferBuy, transferSell));
				past.add(current);
				current = new ArrayList<>();
			} else {
				Transfer.Sell transferSell = new Transfer.Sell(
						activity.tradeDate, activity.symbol, activity.name,
						activity.quantity, activity.price, activity.commission, fxRate,
						cost, costJPY,
						dateBuyFirst, dateBuyLast, totalDividend,
						totalQuantity, totalCost, totalCostJPY
						);
				current.add(new Transfer(transferSell));
				past.add(current);
				current = new ArrayList<>();
			}
			//
			if (isAlmostZero()) {
				reset();
			}
		}
		void dividend(Activity activity) {
			this.totalDividend += activity.credit - activity.debit;
		}
		boolean isAlmostZero() {
			return Math.abs(totalQuantity) < ALMOST_ZERO;
		}
	}
	

	private static void readActivity(String url, Map<String, BuySell> buySellMap,
			Map<String, Dividend> dividendMap, Map<String, Interest> interestMap, Map<String, Account> accountMap) {
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			List<String> sheetNameList = docActivity.getSheetNameList();
			sheetNameList.sort((a, b) -> a.compareTo(b));
			for(String sheetName: sheetNameList) {
				if (!sheetName.matches("^20[0-9][0-9]$")) {
					logger.info("Skip sheet {}", sheetName);
					continue;
				}
				for(Activity activity: Sheet.extractSheet(docActivity, Activity.class, sheetName)) {
					logger.info("activity {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol);
					double fxRate = ForexUtil.getUSD(activity.date);
					
					String month = toMonth(activity.date);
					Account account;
					if (accountMap.containsKey(month)) {
						account = accountMap.get(month);
					} else {
						account = new Account();
						account.date = month;
						accountMap.put(month, account);
					}
						
					switch(activity.transaction) {
					// Transfer
					case "BOUGHT":
					case "NAME CHG": {
						// Use tradeDate
						if (activity.tradeDate.length() == 0) {
							logger.error("tradeDate is empty  {} {} {}", activity.date, activity.symbol, activity.transaction);
							throw new SecuritiesException("tradeDate is empty");
						}
						// FIXME date of fxRate should be activity.tradeDate for buy and sell
						// fxRate = ForexUtil.getUSD(activity.tradeDate);
						String key = activity.symbol;
						BuySell buySell;
						if (buySellMap.containsKey(key)) {
							buySell = buySellMap.get(key);
						} else {
							buySell = new BuySell(activity.symbol, activity.name);
							buySellMap.put(key, buySell);
						}
						buySell.buy(activity, fxRate);
						// Special case for TAL/TRTN(negative quantity for BUY)
						if (buySell.isAlmostZero()) {
							buySellMap.remove(key);
						}
						
						account.buy = DoubleUtil.round(account.buy + activity.debit - activity.credit, 2);
						break;
					}
					case "SOLD":
					case "REDEEMED": {
						// Use tradeDate
						if (activity.tradeDate.length() == 0) {
							logger.error("tradeDate is empty  {} {} {}", activity.date, activity.symbol, activity.transaction);
							throw new SecuritiesException("tradeDate is empty");
						}
						// FIXME date of fxRate should be activity.tradeDate for buy and sell
						// fxRate = ForexUtil.getUSD(activity.tradeDate);
						String key = activity.symbol;
						BuySell buySell;
						if (buySellMap.containsKey(key)) {
							buySell = buySellMap.get(key);
						} else {
							buySell = new BuySell(activity.symbol, activity.name);
							buySellMap.put(key, buySell);
						}
						
						buySell.sell(activity, fxRate);
						break;
					}
					// Dividend
					case "DIVIDEND":
					case "ADR":
					case "MLP":
					case "NRA":
					case "CAP GAIN": {
						// Add record to dividendMap that belong target year
						// Create Dividend Record
						{
							String key = String.format("%s-%s", activity.date, activity.symbol);
							if (dividendMap.containsKey(key)) {
								Dividend dividend = dividendMap.get(key);
								dividend.update(activity.credit, activity.debit);
							} else {
								Dividend dividend = Dividend.getInstance(activity.date, activity.symbol, activity.name, activity.quantity,
										activity.credit, activity.debit, fxRate);
								dividendMap.put(key, dividend);
							}
						}
						
						// Add dividend for the symbol
						{
							String key = activity.symbol;
							BuySell buySell;
							if (buySellMap.containsKey(key)) {
								buySell = buySellMap.get(key);
							} else {
								buySell = new BuySell(activity.symbol, activity.name);
								buySellMap.put(key, buySell);
							}
							buySell.dividend(activity);
						}

						account.dividend = DoubleUtil.round(account.dividend + activity.credit - activity.debit, 2);
						break;
					}
					case "INTEREST": {
						// Add record to dividendMap that belong target year
						String key = activity.date;
						if (interestMap.containsKey(key)) {
							logger.error("duplicated date {}", key);
							throw new SecuritiesException("duplicated date");
						} else {
							Interest interest = new Interest(activity.date, activity.credit, fxRate);
							interestMap.put(key, interest);
						}
						
						account.interest = DoubleUtil.round(account.interest + activity.credit - activity.debit, 2);
						break;
					}
					case "ACH":
					case "WIRE": {
						switch (activity.transaction) {
						case "ACH":
							if (0 < activity.credit) {
								account.achIn = DoubleUtil.round(account.achIn + activity.credit, 2);
							} else if (0 < activity.debit) {
								account.achOut = DoubleUtil.round(account.achOut + activity.debit, 2);
							} else {
								logger.error("Unexpected ach  {}  {}", activity.credit, activity.debit);
								throw new SecuritiesException("Unexpected ach");
							}
							break;
						case "WIRE":
							if (0 < activity.credit) {
								account.wireIn = DoubleUtil.round(account.wireIn + activity.credit, 2);
							} else if (0 < activity.debit) {
								account.wireOut = DoubleUtil.round(account.wireOut + activity.debit, 2);
							} else {
								logger.error("wire  {}  {}", activity.credit, activity.debit);
								throw new SecuritiesException("Unexpected wire");
							}
							break;
						default:
							logger.error("Unknonw transaction {}", activity.transaction);
							throw new SecuritiesException("Unknonw transaction");
						}
						break;
					}
					default:
						if (activity.transaction.startsWith("*")) {
							logger.info("skip transaction {}", activity.transaction);
							continue;
						}
						logger.error("Unknonw transaction {}", activity.transaction);
						throw new SecuritiesException("Unknonw transaction");
					}
				}

			}
		}
	}
	
	private static void buildTransferMapSummaryMap(
			Map<String, BuySell> buySellMap, Map<String, List<TransferDetail>> detailMap, Map<String, TransferSummary> summaryMap) {
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
				detailMap.put(key, detailList);
				summaryMap.put(key, new TransferSummary(lastTransfer.sell));
			}
		}
	}
	
	private static void addDummySellActivity(Map<String, BuySell> buySellMap, Map<String, Account> accountMap) {
		String theDate = "9999-12-31";
		double fxRate = ForexUtil.getUSD(theDate);
		
		for(BuySell buySell: buySellMap.values()) {
			if (buySell.isAlmostZero()) continue;
			
			// Get latest price of symbol
//			logger.info("symbol {}", buySell.symbol);
			Price price = Price.getLastPrice(buySell.symbol);
			logger.info("price {}", price);

			// Add dummy sell record
			Activity activity    = new Activity();
			activity.yyyyMM      = "9999-99";
			activity.page        = "99";
			activity.transaction = "SOLD";
			activity.date        = theDate;
			activity.tradeDate   = theDate;
			activity.symbol      = buySell.symbol;
			activity.name        = buySell.name;
			activity.quantity    = buySell.totalQuantity;
			activity.price       = price.close;
			activity.commission  = 5;
			activity.debit       = 0;
			activity.credit      = activity.quantity * activity.price;
						
			buySell.sell(activity, fxRate);
		}
		
		// add dummy placeholder
		String month = toMonth(theDate);
		Account account = new Account();
		account.date = month;
		accountMap.put(month, account);
		
		// Save cache for later use
		Price.saveCache();
	}

	private static final String toYear(String date) {
		return date.substring(0, 4);
	}
	private static final String toMonth(String date) {
		return date.substring(0, 7);
	}
	public static void generateReport(String url) {
		logger.info("url        {}", url);

		// key is symbol
		Map<String, BuySell>  buySellMap  = new TreeMap<>();
		// key is "date-symbol"
		Map<String, Dividend>             dividendMap = new TreeMap<>();
		Map<String, List<TransferDetail>> detailMap   = new TreeMap<>();
		Map<String, TransferSummary>      summaryMap  = new TreeMap<>();
		// key is date
		Map<String, Interest>             interestMap = new TreeMap<>();
		// key is date
		Map<String, Account>                 accountMap     = new TreeMap<>();
		
		readActivity(url, buySellMap, dividendMap, interestMap, accountMap);
		addDummySellActivity(buySellMap, accountMap);
		
		buildTransferMapSummaryMap(buySellMap, detailMap, summaryMap);
		
		SortedSet<String> yearSet = new TreeSet<>();

		yearSet.addAll(detailMap.keySet().stream().map(Report::toYear).collect(Collectors.toSet()));
		yearSet.addAll(dividendMap.keySet().stream().map(Report::toYear).collect(Collectors.toSet()));
		yearSet.addAll(interestMap.keySet().stream().map(Report::toYear).collect(Collectors.toSet()));
		
		logger.info("year {}", yearSet);
		
		{
			SpreadSheet docLoad = new SpreadSheet(URL_TEMPLATE, true);
			SpreadSheet docSave = new SpreadSheet();
			
			for(String targetYear: yearSet) {
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
			
			{
				for(TransferSummary e: summaryMap.values()) {
					String month = toMonth(e.dateSell);
					Account account = accountMap.get(month);
					if (account == null) {
						logger.error("account is null  month = {}", month);
						throw new SecuritiesException("account is null");
					}
					account.sell     = DoubleUtil.round(account.sell     + e.sell, 2);
					account.sellCost = DoubleUtil.round(account.sellCost + e.buy, 2);
					account.sellGain = DoubleUtil.round(account.sell     - account.sellCost, 2);
				}

				List<Account> accountList = new ArrayList<>();
				double fundTotal = 0;
				double cashTotal = 0;
				double stockTotal = 0;
				for(Account account: accountMap.values()) {
					double fund = account.wireIn - account.wireOut + account.achIn - account.achOut;
					fundTotal  = DoubleUtil.round(fundTotal  + fund, 2);
					cashTotal  = DoubleUtil.round(cashTotal  + fund + account.interest + account.dividend - account.buy + account.sell, 2);
					stockTotal = DoubleUtil.round(stockTotal + account.buy  - account.sellCost, 2);
					
					account.fundTotal  = fundTotal;
					account.cashTotal  = cashTotal;
					account.stockTotal = stockTotal;
					account.gainTotal  = DoubleUtil.round(account.cashTotal + account.stockTotal - account.fundTotal, 2);
					
					accountList.add(account);
//					logger.info("account {}", account.toString());
				}
				
				String sheetName = Sheet.getSheetName(Account.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.fillSheet(docSave, accountList);
				
				String newSheetName = String.format("%s-%s",  "9999", sheetName);
				logger.info("sheet {}", newSheetName);
				docSave.renameSheet(sheetName, newSheetName);
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
		
//		generateReport(URL_ACTIVITY_TEST);
		generateReport(URL_ACTIVITY);
		
		logger.info("STOP");
		System.exit(0);
	}
}
