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

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	static class BuySell {
		private static final double ALMOST_ZERO = 0.000001;

		String symbol;
		String name;

		int    buyCount;
		String dateBuyFirst;
		String dateBuyLast;

		double quantity;
		int    acquisitionCostJPY;
		
		List<Transfer>       current;
		List<List<Transfer>> past;
		
		public BuySell(String symbol, String name) {
			this.symbol        = symbol;
			this.name          = name;
			
			buyCount           = 0;
			dateBuyFirst       = "";
			dateBuyLast        = "";
			
			quantity           = 0;
			acquisitionCostJPY = 0;
			current            = new ArrayList<>();
			past               = new ArrayList<>();
		}
		
		void reset() {
			buyCount           = 0;
			dateBuyFirst       = "";
			dateBuyLast        = "";
			
			quantity           = 0;
			acquisitionCostJPY = 0;
			
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
			
			int acjpy = (int)Math.round((activity.quantity * activity.price + activity.commission) * fxRate); // acjpy = acquisitionCostJPY
			quantity           += activity.quantity;
			acquisitionCostJPY += acjpy;

			// special case for negative buy -- for TAL name change
			// TODO is this correct?
			if (isAlmostZero()) {
				reset();
				return;
			}
			
			Transfer transfer = Transfer.getInstanceForBuy (
					activity.symbol, activity.name, activity.quantity, activity.tradeDate, activity.price, activity.commission, fxRate,
					acjpy, quantity, acquisitionCostJPY);
			current.add(transfer);
		}
		void sell(Activity activity, double fxRate) {
			double priceSell        = activity.price * activity.quantity;
			int    amountSellJPY    = (int)Math.round(priceSell * fxRate);
			int    commisionSellJPY = (int)Math.round(activity.commission * fxRate);

			int acJPY;
			if (buyCount == 1) {
				acJPY = (int)Math.round(acquisitionCostJPY * (activity.quantity / quantity));
				
				// maintain quantity and acquisitionCostJPY
				quantity           -= activity.quantity;
				acquisitionCostJPY -= acJPY;
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, quantity, amountSellJPY, acJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			} else {
				double unitCost = Math.ceil(acquisitionCostJPY / quantity);
				acJPY = (int)Math.round(unitCost * activity.quantity);
				
				// maintain quantity and acquisitionCostJPY
				quantity           -= activity.quantity;
				acquisitionCostJPY -= acJPY;
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						activity.tradeDate, symbol, quantity, amountSellJPY, acquisitionCostJPY, commisionSellJPY, dateBuyFirst, dateBuyLast));
			}

			// TODO How about buy 1 time and sell more than 1 time?
			if (buyCount == 1 && current.size() == 1 && isAlmostZero()) {
				// Special case buy one time and sell whole
				Transfer buy  = current.remove(0);
				Transfer transfer = Transfer.getInstanceForSellSpecial(
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acJPY, dateBuyFirst, dateBuyLast,
						buy.dateBuy, buy.priceBuy, buy.commissionBuy, buy.fxRateBuy, buy.amountBuyJPY, quantity, buy.amountBuyJPY
						);
				current.add(transfer);
				past.add(current);
				current = new ArrayList<>();
			} else {
				Transfer transfer = Transfer.getInstanceForSell (
						activity.symbol, activity.name, activity.quantity,
						activity.tradeDate, activity.price, activity.commission, fxRate, commisionSellJPY,
						amountSellJPY, acJPY, dateBuyFirst, dateBuyLast);
				current.add(transfer);
				// TODO This is not correct. acquisitionCostJPY is not correct(acquisitionCostJPY has whole value). 
				past.add(current);
				current = new ArrayList<>();
			}
			//
			if (isAlmostZero()) {
				reset();
			}
		}
		boolean isAlmostZero() {
			return Math.abs(quantity) < ALMOST_ZERO;
		}
	}
	

	
	public static void main(String[] args) {
		logger.info("START");
		
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益.ods";
		String targetYear = "2016";
		
		// key is "date-symbol"
		Map<String, Dividend> dividendMap = new TreeMap<>();
		// key is symbol
		Map<String, BuySell> buySellMap = new TreeMap<>();
		//TODO BuySell contains List<List<Transfer>>.  Is this correct?
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			for(Activity activity: Sheet.getInstance(libreOffice, Activity.class)) {
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
					if (activity.date.startsWith(targetYear)) {
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
					}
					break;
				}
				case "INTEREST": {
					// Add record to dividendMap that belong target year
					if (activity.date.startsWith(targetYear)) {
						String key = String.format("%s-%s", activity.date, "____");
						if (dividendMap.containsKey(key)) {
							Dividend dividend = dividendMap.get(key);
							dividend.update(activity.credit, activity.debit);
						} else {
							Dividend dividend = Dividend.getInstance(activity.date, activity.credit, activity.debit, fxRate);
							dividendMap.put(key, dividend);
						}
					}
					break;
				}
				default:
					logger.error("Unknonw transaction {}", activity.transaction);
					throw new SecuritiesException("Unknonw transaction");
				}
			}
		}
		
		{
			String timeStamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());
			logger.info("timeStamp {}", timeStamp);
			
			String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
			String urlSave = String.format("file:///home/hasegawa/Dropbox/Trade/REPORT_%s.ods", timeStamp);
			
			try (LibreOffice docLoad = new LibreOffice(urlLoad, true)) {
				{
					List<Transfer> transferList = new ArrayList<>();
					List<Summary> summaryList   = new ArrayList<>();
					{
						List<String> keyList = new ArrayList<>();
						keyList.addAll(buySellMap.keySet());
						Collections.sort(keyList);
						for(String key: keyList) {
							BuySell buySell = buySellMap.get(key);
							for(List<Transfer> pastTransferList: buySell.past) {
								// I have interested in transfer record in transferList that sold date in targetYear
								Transfer lastTransfer = pastTransferList.get(pastTransferList.size() - 1);
								if (lastTransfer.dateSell.startsWith(targetYear)) {
									transferList.addAll(pastTransferList);
									summaryList.add(Summary.getInstance(lastTransfer));
								}
							}
						}
					}
					Sheet.saveSheet(docLoad, Transfer.class, transferList);
					Sheet.saveSheet(docLoad, Summary.class, summaryList);
				}
				{
					List<Dividend> dividendList = new ArrayList<>();
					{
						List<String> keyList = new ArrayList<>();
						keyList.addAll(dividendMap.keySet());
						Collections.sort(keyList);
						for(String key: keyList) {
							Dividend dividend = dividendMap.get(key);
							dividendList.add(dividend);
						}
					}
					Sheet.saveSheet(docLoad, Dividend.class, dividendList);
				}
				docLoad.store(urlSave);
			}
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}