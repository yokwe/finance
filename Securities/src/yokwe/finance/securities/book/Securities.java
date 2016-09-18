package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Securities {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Securities.class);
	
	private static final double ALMOST_ZERO = 0.000001;
	
	String dateBuyFirst;
	String dateBuyLast;
	String symbol;
	String name;
	double quantity;
	double price;
	double commission;
	double usdjpy;
	
	int          count;
	int          acquisitionCostJPY;
	List<ReportTransfer> reportList;
	
	private Securities(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy) {
		this.dateBuyFirst  = date;
		this.dateBuyLast   = "";
		this.symbol        = symbol;
		this.name          = name;
		this.quantity      = quantity;
		this.price         = price;
		this.commission    = commission;
		this.usdjpy        = usdjpy;
		
		count              = 1;
		acquisitionCostJPY = (int)Math.round((quantity * price + commission) * usdjpy);
		reportList         = new ArrayList<>();
	}
	
	private static Map<String, Securities> securitiesMap = new LinkedHashMap<>();
	
	public static void buy(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy, List<ReportTransfer> reportList) {
		int acquisitionCostJPY = (int)Math.round((quantity * price + commission) * usdjpy);

		if (securitiesMap.containsKey(symbol)) {
			Securities securities = securitiesMap.get(symbol);
			
			securities.dateBuyLast         = date;
			securities.quantity           += quantity;
			securities.price               = price;
			securities.commission          = commission;
			securities.usdjpy              = usdjpy;
			securities.count++;
			securities.acquisitionCostJPY += acquisitionCostJPY;
			
			// Special case for TAL/TRTN(negative quantity for BUY)
			if (Math.abs(securities.quantity) < ALMOST_ZERO) {
				securitiesMap.remove(symbol);
			}
			
			ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, acquisitionCostJPY, securities.quantity, securities.acquisitionCostJPY);
			securities.reportList.add(report);
		} else {
			Securities securities = new Securities(date, symbol, name, quantity, price, commission, usdjpy);
			ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, acquisitionCostJPY, securities.quantity, securities.acquisitionCostJPY);
			securities.reportList.add(report);
			securitiesMap.put(symbol, securities);
		}
	}
	
	public static void sell(String date, String symbol, String name, double quantity, double price, double commission, double usdjpy, List<ReportTransfer> reportList) {
		if (securitiesMap.containsKey(symbol)) {
			double priceSell        = price * quantity;
			int    sellAmountJPY    = (int)Math.round(priceSell * usdjpy);
			int    sellCommisionJPY = (int)Math.round(commission * usdjpy);

			Securities securities = securitiesMap.get(symbol);
			
			int acquisitionCostJPY;
			if (securities.count == 1) {
				acquisitionCostJPY = (int)Math.round(securities.acquisitionCostJPY * (quantity / securities.quantity));
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL {}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisitionCostJPY, sellCommisionJPY, securities.dateBuyFirst, securities.dateBuyLast));
				
				// maintain securities
				securities.quantity           -= quantity;
				securities.acquisitionCostJPY -= acquisitionCostJPY;
			} else {
				double unitCost = Math.ceil(securities.acquisitionCostJPY / securities.quantity);
				acquisitionCostJPY = (int)Math.round(unitCost * quantity);
				
				securities.quantity           -= quantity;
				securities.acquisitionCostJPY  = (int)Math.round(unitCost * securities.quantity);
				
				// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
				logger.info("SELL*{}", String.format("%s %-8s %9.5f %7d %7d %7d %s %s",
						date, symbol, quantity, sellAmountJPY, acquisitionCostJPY, sellCommisionJPY, securities.dateBuyFirst, securities.dateBuyLast));
			}
						
			if (securities.count == 1 && securities.reportList.size() == 1 && Math.abs(securities.quantity) < ALMOST_ZERO) {
				// Special case: buy once and sell whole.
				//   Output one record for both buy and sell
				ReportTransfer buy  = securities.reportList.get(0);
				ReportTransfer sell = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, sellCommisionJPY, sellAmountJPY, acquisitionCostJPY, securities.dateBuyFirst, securities.dateBuyLast);
				
				ReportTransfer report = ReportTransfer.getInstance(
						symbol, name, quantity,
						sell.dateSell, sell.priceSell, sell.commissionSell, sell.fxRateSell, sell.commissionSellJPY, sell.amountSellJPY, sell.acquisitionCostJPY, sell.dateBuyFirst, sell.dateBuyLast,
						buy.dateBuy, buy.priceBuy, buy.commissionBuy, buy.fxRateBuy, buy.amountBuyJPY, "", "");
				securities.reportList.clear();
				securities.reportList.add(report);
			} else {
				ReportTransfer report = ReportTransfer.getInstance(symbol, name, quantity, date, price, commission, usdjpy, sellCommisionJPY, sellAmountJPY, acquisitionCostJPY, securities.dateBuyFirst, securities.dateBuyLast);
				securities.reportList.add(report);
			}
			
			// If quantity of securities become ZERO, output accumulated reportList and remove from securitiesMap
			if (Math.abs(securities.quantity) < ALMOST_ZERO) {
				for(ReportTransfer report: securities.reportList) {
					reportList.add(report);
				}
				securities.reportList.clear();
				securitiesMap.remove(symbol);
			}
		} else {
			logger.error("Unknown symbol = {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public static void addRemaining(List<ReportTransfer> reportList) {
		List<String> symbolList = new ArrayList<>();
		symbolList.addAll(securitiesMap.keySet());
		Collections.sort(symbolList);
		for(Map.Entry<String, Securities> entry: securitiesMap.entrySet()) {
			Securities securities = entry.getValue();
			reportList.addAll(securities.reportList);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016.ods";
		
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			List<TransactionBuySell> transactionList = SheetData.getInstance(libreOffice, TransactionBuySell.class);
			List<ReportTransfer> reportList = new ArrayList<>();
	
			Mizuho.Map mizuhoMap = new Mizuho.Map(url);
			SymbolName.Map symbolNameMap = new SymbolName.Map(url);

			for(TransactionBuySell transaction: transactionList) {
				double usdjpy = mizuhoMap.get(transaction.tradeDate).usd;
				String symbolName = symbolNameMap.getName(transaction.symbol);
				switch (transaction.transaction) {
				case "BOUGHT": {
					Securities.buy(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportList);
					break;
				}
				case "SOLD": {
					Securities.sell(transaction.tradeDate, transaction.symbol, symbolName, transaction.quantity, transaction.price, transaction.commission, usdjpy, reportList);
					break;
				}
				default: {
					logger.error("Unknown transaction = {}", transaction.transaction);
					throw new SecuritiesException("Unexpected");
				}
				}
			}
			
//			{
//				int n = 1;
//				for(Map.Entry<String, Securities> entry: securitiesMap.entrySet()) {
//					Securities securities = entry.getValue();
//					
//					if (securities.count == 1) {
//						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
//						logger.info("BUY  {}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
//								n++, securities.symbol, securities.quantity, 0, securities.acquisitionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
//					} else {
//						double unitCost = Math.ceil(securities.acquisitionCostJPY / securities.quantity);
//						int acquisitionCostJPY = (int)Math.round(unitCost * securities.quantity);
//
//						// date symbol name sellAmountJPY asquisionCostJPY sellCommisionJPY dateBuyFirst dateBuyLast
//						logger.info("BUY *{}", String.format("%10d %-8s %9.5f %7d %7d %7d %s %s",
//								n++, securities.symbol, securities.quantity, 0, acquisitionCostJPY, 0, securities.dateBuyFirst, securities.dateBuyLast));
//					}
//				}
//			}
			
			// Output report of remaining securities in alphabetical order
			Securities.addRemaining(reportList);
			
			{
				String urlLoad = "file:///home/hasegawa/Dropbox/Trade/REPORT_TEMPLATE.ods";
				String urlSave = "file:///home/hasegawa/Dropbox/Trade/REPORT_OUTPUT.ods";
				
				try (LibreOffice docLoad = new LibreOffice(urlLoad, true)) {
					SheetData.saveSheet(docLoad, ReportTransfer.class, reportList);
					docLoad.store(urlSave);
				}
			}
			
			logger.info("STOP");
			System.exit(0);
		}
	}
}
