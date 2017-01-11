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
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.Mizuho;


// TODO report profit loss of each year like evaluation sheet

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
		double totalCost;
		int    totalCostJPY;
		double totalDividend;
		
		List<TransferDetail>       current;
		List<List<TransferDetail>> past;
		
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
			double acquisitionCost = (activity.quantity * activity.price) + activity.commission;
			int acquisitionCostJPY = (int)Math.round(acquisitionCost * fxRate);
			totalQuantity           += activity.quantity;
			totalCost    += acquisitionCost;
			totalCostJPY += acquisitionCostJPY;

			// special case for negative buy -- for TAL name change
			// TODO is this correct?
			if (isAlmostZero()) {
				reset();
				return;
			}
			
			TransferDetail transfer = TransferDetail.getInstanceForBuy (
					activity.symbol, activity.name, activity.quantity, activity.tradeDate, activity.price, activity.commission, fxRate,
					acquisitionCostJPY, totalQuantity, totalCostJPY);
			current.add(transfer);
		}
		void sell(Activity activity, double fxRate) {
			double priceSell        = activity.price * activity.quantity;
			int    amountSellJPY    = (int)Math.round(priceSell * fxRate);
			int    commisionSellJPY = (int)Math.round(activity.commission * fxRate);

			double acquisitionCost;
			int    acquisitionCostJPY;
			
			double sellRatio = activity.quantity / totalQuantity;
			acquisitionCost    = totalCost * sellRatio;

			if (buyCount == 1) {
				acquisitionCostJPY = (int)Math.round(totalCostJPY * sellRatio);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity           -= activity.quantity;
				totalCost    -= acquisitionCost;
				totalCostJPY -= acquisitionCostJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, amountSellJPY, acquisitionCostJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			} else {
				double unitCostJPY = Math.ceil(totalCostJPY / totalQuantity); // need to be round up
				acquisitionCostJPY = (int)Math.round(unitCostJPY * activity.quantity);
				// need to adjust totalAcquisitionCostJPY
				totalCostJPY = (int)Math.round(unitCostJPY * totalQuantity);
				
				// maintain totalQuantity, totalAcquisitionCost and totalAcquisitionCostJPY
				totalQuantity           -= activity.quantity;
				totalCost    -= acquisitionCost;
				totalCostJPY -= acquisitionCostJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, totalQuantity, amountSellJPY, totalCostJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			}

			// TODO How about buy 1 time and sell more than 1 time?
			if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
				// Special case buy one time and sell whole
				TransferDetail buy  = current.remove(0);
				TransferDetail transfer = TransferDetail.getInstanceForSellSpecial(
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast,
						buy.dateBuy, buy.priceBuy, buy.commissionBuy, buy.fxRateBuy, buy.amountBuyJPY
						);
				current.add(transfer);
				past.add(current);
				current = new ArrayList<>();
			} else {
				TransferDetail transfer = TransferDetail.getInstanceForSell (
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast, totalQuantity, totalCostJPY);
				current.add(transfer);
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
	

	private static void readActivity(String url, Map<String, BuySell> buySellMap, Map<String, Dividend> dividendMap, Map<String, Evaluation> evaluationMap) {
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
					
					
					// Add record to evaluationList
					{
						double sellRatio = activity.quantity / buySell.totalQuantity;
						double cost      = buySell.totalCost * sellRatio;
						
						Evaluation evaluation = new Evaluation(buySell.symbol, buySell.name, activity.quantity, 
								cost, activity.credit, buySell.totalDividend);
						
						String k = String.format("%s-%s", activity.date, buySell.symbol);
						evaluationMap.put(k, evaluation);
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
	
	private static void buildTransferMapSummaryMap(Map<String, BuySell> buySellMap, Map<String, List<TransferDetail>> transferMap, Map<String, Summary> summaryMap) {
		List<String> keyList = new ArrayList<>();
		keyList.addAll(buySellMap.keySet());
		Collections.sort(keyList);
		for(BuySell buySell: buySellMap.values()) {
			String symbol = buySell.symbol;
			String firstDateSell = null;
			for(List<TransferDetail> pastTransferList: buySell.past) {
				TransferDetail lastTransfer = pastTransferList.get(pastTransferList.size() - 1);
				if (firstDateSell == null) firstDateSell = lastTransfer.dateSell;
				String key = String.format("%s-%s", lastTransfer.dateSell, symbol);
				summaryMap.put(key, Summary.getInstance(lastTransfer));
				transferMap.put(key, pastTransferList);
			}
		}
	}
	
	private static void addDummySellActivity(Map<String, BuySell> buySellMap, Map<String, Evaluation> evalutationList) {
		String theDate = "9999-99-99";
		double fxRate = Mizuho.getUSD(theDate);
		
		for(BuySell buySell: buySellMap.values()) {
			if (buySell.isAlmostZero()) continue;
			
			// Get latest price of symbol
//			logger.info("symbol {}", buySell.symbol);
			Price price = Price.getLastPrice(buySell.symbol);
			logger.info("price {}", price);

			// Add record to evaluationList
			{
				Evaluation evaluation = new Evaluation(buySell.symbol, buySell.name, buySell.totalQuantity, 
						buySell.totalCost, price.close * buySell.totalQuantity + 7, buySell.totalDividend);
				
				String key = String.format("%s-%s", theDate, buySell.symbol);
				evalutationList.put(key, evaluation);
			}

			// Add dummy sell record
			Activity activity = new Activity();
			activity.yyyyMM      = "9999-99";
			activity.page        = "99";
			activity.transaction = "SOLD";
			activity.date        = theDate;
			activity.tradeDate   = theDate;
			activity.symbol      = buySell.symbol;
			activity.name        = buySell.name;
			activity.quantity    = buySell.totalQuantity;
			activity.price       = price.close;
			activity.commission  = 7;
			activity.debit       = 0;
			activity.credit      = activity.quantity * activity.price;
						
			buySell.sell(activity, fxRate);
		}
		
		// Save cache for later use
		Price.saveCache();
	}

	public static void generateReport(String url) {
		logger.info("url        {}", url);

		String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
		logger.info("timeStamp {}", timeStamp);
		
		// key is symbol
		Map<String, BuySell>  buySellMap  = new TreeMap<>();
		// key is "date-symbol"
		Map<String, Dividend>       dividendMap   = new TreeMap<>();
		Map<String, List<TransferDetail>> transferMap   = new TreeMap<>();
		Map<String, Summary>        summaryMap    = new TreeMap<>();
		Map<String, Evaluation>	    evaluationMap = new TreeMap<>();
		
		readActivity(url, buySellMap, dividendMap, evaluationMap);
		addDummySellActivity(buySellMap, evaluationMap);
		
		buildTransferMapSummaryMap(buySellMap, transferMap, summaryMap);
		
		List<String> yearList = new ArrayList<>();
		for(String key: dividendMap.keySet()) {
			String year = key.substring(0,  4);
			if (yearList.contains(year)) continue;
			yearList.add(year);
		}
		for(String key: summaryMap.keySet()) {
			String year = key.substring(0,  4);
			if (yearList.contains(year)) continue;
			yearList.add(year);
		}
		yearList.sort((a, b) -> a.compareTo(b));
		logger.info("year {}", yearList);
		
		{
			String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
			String urlSave = String.format("file:///home/hasegawa/Dropbox/Trade/REPORT_%s.ods", timeStamp);
			
			SpreadSheet docLoad = new SpreadSheet(urlLoad, true);
			SpreadSheet docSave = new SpreadSheet();
			
			for(String targetYear: yearList) {
				{
					List<TransferDetail> transferList = new ArrayList<>();
					for(String key: transferMap.keySet()) {
						if (key.startsWith(targetYear)) transferList.addAll(transferMap.get(key));
					}
					
					if (!transferList.isEmpty()) {
						String sheetName = Sheet.getSheetName(TransferDetail.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.saveSheet(docSave, TransferDetail.class, transferList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
				
				{
					List<Summary> summaryList = new ArrayList<>();
					for(String key: summaryMap.keySet()) {
						if (key.startsWith(targetYear)) summaryList.add(summaryMap.get(key));
					}

					if (!summaryList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Summary.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.saveSheet(docSave, Summary.class, summaryList);
						
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
						Sheet.saveSheet(docSave, Dividend.class, dividendList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
				
				{
					List<Evaluation> evaluationList = new ArrayList<>();
					for(String key: evaluationMap.keySet()) {
						if (key.startsWith(targetYear)) evaluationList.add(evaluationMap.get(key));
					}

					if (!evaluationList.isEmpty()) {
						String sheetName = Sheet.getSheetName(Evaluation.class);
						docSave.importSheet(docLoad, sheetName, docSave.getSheetCount());
						Sheet.saveSheet(docSave, Evaluation.class, evaluationList);
						
						String newSheetName = String.format("%s-%s",  targetYear, sheetName);
						logger.info("sheet {}", newSheetName);
						docSave.renameSheet(sheetName, newSheetName);
					}
				}
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
		
		Mizuho.enableTestMode();
		generateReport(url);
	}
	
	public static void generateReport() {
		String url        = "file:///home/hasegawa/Dropbox/Trade/投資損益.ods";
		
		generateReport(url);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		//generateTestReport();
		generateReport();
		
		logger.info("STOP");
		System.exit(0);
	}
}
