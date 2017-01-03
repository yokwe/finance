package yokwe.finance.securities.tax;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.SpreadSheet;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	static class BuySell {
		private static final double ALMOST_ZERO = 0.000001;

		String symbol;
		String name;

		int    buyCount;
		String dateBuyFirst;
		String dateBuyLast;

		double totalQuantity;
		double totalAcquisitionCost;
		int    totalAcquisitionCostJPY;
		
		List<Transfer>       current;
		List<List<Transfer>> past;
		
		public BuySell(String symbol, String name) {
			this.symbol        = symbol;
			this.name          = name;
			
			buyCount           = 0;
			dateBuyFirst       = "";
			dateBuyLast        = "";
			
			totalQuantity           = 0;
			totalAcquisitionCost    = 0;
			totalAcquisitionCostJPY = 0;
			
			current            = new ArrayList<>();
			past               = new ArrayList<>();
		}
		
		void reset() {
			buyCount           = 0;
			dateBuyFirst       = "";
			dateBuyLast        = "";
			
			totalQuantity           = 0;
			totalAcquisitionCost    = 0;
			totalAcquisitionCostJPY = 0;
			
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
			double acquisitionCost = (activity.quantity * activity.price) + activity.commission;
			int acquisitionCostJPY = (int)Math.round(acquisitionCost * fxRate);
			totalQuantity           += activity.quantity;
			totalAcquisitionCost    += acquisitionCost;
			totalAcquisitionCostJPY += acquisitionCostJPY;

			// special case for negative buy -- for TAL name change
			// TODO is this correct?
			if (isAlmostZero()) {
				reset();
				return;
			}
			
			Transfer transfer = Transfer.getInstanceForBuy (
					activity.symbol, activity.name, activity.quantity, activity.tradeDate, activity.price, activity.commission, fxRate,
					acquisitionCostJPY, totalQuantity, totalAcquisitionCostJPY);
			current.add(transfer);
		}
		void sell(Activity activity, double fxRate) {
			double priceSell        = activity.price * activity.quantity;
			int    amountSellJPY    = (int)Math.round(priceSell * fxRate);
			int    commisionSellJPY = (int)Math.round(activity.commission * fxRate);

			double acquisitionCost;
			int    acquisitionCostJPY;
			
			double sellRatio = activity.quantity / totalQuantity;
			acquisitionCost    = totalAcquisitionCost * sellRatio;

			if (buyCount == 1) {
				acquisitionCostJPY = (int)Math.round(totalAcquisitionCostJPY * sellRatio);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity           -= activity.quantity;
				totalAcquisitionCost    -= acquisitionCost;
				totalAcquisitionCostJPY -= acquisitionCostJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, amountSellJPY, acquisitionCostJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			} else {
				double unitCostJPY = Math.ceil(totalAcquisitionCostJPY / totalQuantity); // need to be round up
				acquisitionCostJPY = (int)Math.round(unitCostJPY * activity.quantity);
				// need to adjust totalAcquisitionCostJPY
				totalAcquisitionCostJPY = (int)Math.round(unitCostJPY * totalQuantity);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity           -= activity.quantity;
				totalAcquisitionCost    -= acquisitionCost;
				totalAcquisitionCostJPY -= acquisitionCostJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, amountSellJPY, totalAcquisitionCostJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			}

			// TODO How about buy 1 time and sell more than 1 time?
			if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
				// Special case buy one time and sell whole
				Transfer buy  = current.remove(0);
				Transfer transfer = Transfer.getInstanceForSellSpecial(
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast,
						buy.dateBuy, buy.priceBuy, buy.commissionBuy, buy.fxRateBuy, buy.amountBuyJPY
						);
				current.add(transfer);
				past.add(current);
				current = new ArrayList<>();
			} else {
				Transfer transfer = Transfer.getInstanceForSell (
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast, totalQuantity, totalAcquisitionCostJPY);
				current.add(transfer);
				past.add(current);
				current = new ArrayList<>();
			}
			//
			if (isAlmostZero()) {
				reset();
			}
		}
		boolean isAlmostZero() {
			return Math.abs(totalQuantity) < ALMOST_ZERO;
		}
	}
	

	private static void readActivity(String url, Map<String, BuySell> buySellMap, Map<String, Dividend> dividendMap) {
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {
			for(Activity activity: Sheet.getInstance(docActivity, Activity.class)) {
				logger.info("activity {} {} {}", activity.date, activity.transaction, activity.symbol);
				double fxRate = Mizuho.getUSD(activity.date);
				
				switch(activity.transaction) {
				// Transfer
				case "BOUGHT":
				case "NAME CHG": {
					// Use tradeDate
					if (activity.tradeDate.length() == 0) {
						logger.error("tradeDate is empty  {} {} {}", activity.date, activity.symbol, activity.transaction);
						throw new SecuritiesException("tradeDate is empty");
					}
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
					break;
				}
				case "SOLD":
				case "REDEEMED": {
					// Use tradeDate
					if (activity.tradeDate.length() == 0) {
						logger.error("tradeDate is empty  {} {} {}", activity.date, activity.symbol, activity.transaction);
						throw new SecuritiesException("tradeDate is empty");
					}
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
					String key = String.format("%s-%s", activity.date, activity.symbol);
					if (dividendMap.containsKey(key)) {
						Dividend dividend = dividendMap.get(key);
						dividend.update(activity.credit, activity.debit);
					} else {
						Dividend dividend = Dividend.getInstance(activity.date, activity.symbol, activity.name, activity.quantity,
								activity.credit, activity.debit, fxRate);
						dividendMap.put(key, dividend);
					}
					break;
				}
				case "INTEREST": {
					// Add record to dividendMap that belong target year
					String key = String.format("%s-%s", activity.date, "____");
					if (dividendMap.containsKey(key)) {
						Dividend dividend = dividendMap.get(key);
						dividend.update(activity.credit, activity.debit);
					} else {
						Dividend dividend = Dividend.getInstance(activity.date, activity.credit, activity.debit, fxRate);
						dividendMap.put(key, dividend);
					}
					break;
				}
				default:
					logger.error("Unknonw transaction {}", activity.transaction);
					throw new SecuritiesException("Unknonw transaction");
				}
			}
		}
	}
	
	private static void buildTransferSummaryList(String targetYear, Map<String, BuySell> buySellMap, List<Transfer> transferList, List<Summary> summaryList) {
		Map<String, List<Transfer>> targetTransferMap = new TreeMap<>();

		List<String> keyList = new ArrayList<>();
		keyList.addAll(buySellMap.keySet());
		Collections.sort(keyList);
		for(String key: keyList) {
			BuySell buySell = buySellMap.get(key);
			List<Transfer> targetList = new ArrayList<>();
			String firstDateSell = null;
			for(List<Transfer> pastTransferList: buySell.past) {
				// I have interested in transfer record in transferList that sold date in targetYear
				Transfer lastTransfer = pastTransferList.get(pastTransferList.size() - 1);
				if (lastTransfer.dateSell.startsWith(targetYear)) {
					if (firstDateSell == null) firstDateSell = lastTransfer.dateSell;
					targetList.addAll(pastTransferList);
					summaryList.add(Summary.getInstance(lastTransfer));
				}
			}
			targetTransferMap.put(String.format("%s-%s", firstDateSell, key), targetList);
		}
		
		// sort summaryList with dateSell
		Collections.sort(summaryList, (a, b) -> a.dateSell.compareTo(b.dateSell));
		// build transferList from targetTransferMap
		for(List<Transfer> e: targetTransferMap.values()) {
			transferList.addAll(e);
		}
	}
	
	private static void buildDividendList(String targetYear, Map<String, Dividend> dividendMap, List<Dividend> dividendList) {
		List<String> keyList = new ArrayList<>();
		keyList.addAll(dividendMap.keySet());
		Collections.sort(keyList);
		for(String key: keyList) {
			Dividend dividend = dividendMap.get(key);
			if (dividend.date.startsWith(targetYear)) {
				dividendList.add(dividend);
			}
		}
	}
	
	public static void generateReport(String url, String targetYear) {
		logger.info("url        {}", url);
		logger.info("targetYear {}", targetYear);

		String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
		logger.info("timeStamp {}", timeStamp);
		
		// key is symbol
		Map<String, BuySell>  buySellMap  = new TreeMap<>();
		// key is "date-symbol"
		Map<String, Dividend> dividendMap = new TreeMap<>();
		
		List<Transfer> transferList = new ArrayList<>();
		List<Summary>  summaryList  = new ArrayList<>();
		List<Dividend> dividendList = new ArrayList<>();

		readActivity(url, buySellMap, dividendMap);
		
		buildTransferSummaryList(targetYear, buySellMap, transferList, summaryList);
		buildDividendList(targetYear, dividendMap, dividendList);
				
		{
			String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
			String urlSave = String.format("file:///home/hasegawa/Dropbox/Trade/REPORT_%s.ods", timeStamp);
			
			SpreadSheet docLoad = new SpreadSheet(urlLoad, true);
			SpreadSheet docSave = new SpreadSheet();
			
			{
				String sheetName = Sheet.getSheetName(Transfer.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.saveSheet(docSave, Transfer.class, transferList);
				
				String newSheetName = String.format("%s-%s",  targetYear, sheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}
			
			{
				String sheetName = Sheet.getSheetName(Summary.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.saveSheet(docSave, Summary.class, summaryList);
				
				String newSheetName = String.format("%s-%s",  targetYear, sheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}
			{
				String sheetName = Sheet.getSheetName(Dividend.class);
				docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
				Sheet.saveSheet(docSave, Dividend.class, dividendList);
				
				String newSheetName = String.format("%s-%s",  targetYear, sheetName);
				docSave.renameSheet(sheetName, newSheetName);
			}
			
			// remove first sheet
			docSave.removeSheet(docSave.getSheetName(0));

			docSave.store(urlSave);
			
			docLoad.close();
		}
	}
	
	public static void generateTestReport() {
		// See page 50 of URL below about test case
		//   https://www.nta.go.jp/tetsuzuki/shinkoku/shotoku/tebiki2016/kisairei/kabushiki/pdf/15.pdf
		
		String url        = "file:///home/hasegawa/Dropbox/Trade/投資損益_TEST.ods";
		String targetYear = "2000";
		
		Mizuho.enableTestMode();
		generateReport(url, targetYear);
	}
	
	public static void generateReport() {
		String url        = "file:///home/hasegawa/Dropbox/Trade/投資損益.ods";
		String targetYear = "2016";
		
		generateReport(url, targetYear);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		//generateTestReport();
		generateReport();
		
		logger.info("STOP");
		System.exit(0);
	}
}

//add remaining equity information
//TODO output to another sheet
//		{
//			String theDate = "9999-99-99";
//			double fxRate = Mizuho.getUSD(theDate);
//			
//			for(String key: keyList) {
//				BuySell buySell = buySellMap.get(key);
//				if (buySell.isAlmostZero()) continue;
//
//				// add dummy sell record
//				Equity equity = Equity.get(key);
//				Activity activity = new Activity();
//				
//				activity.yyyyMM      = "9999-99";
//				activity.page        = "99";
//				activity.transaction = "SOLD";
//				activity.date        = theDate;
//				activity.tradeDate   = theDate;
//				activity.symbol      = buySell.symbol;
//				activity.name        = buySell.name;
//				activity.quantity    = buySell.totalQuantity;
//				activity.price       = equity.price;
//				activity.commission  = 7;
//				activity.debit       = 0;
//				activity.credit      = activity.quantity * activity.price;
//				
//				buySell.sell(activity, fxRate);
//				
//				transferList.addAll(buySell.past.get(buySell.past.size() - 1));
//			}
//		}
