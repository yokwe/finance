package yokwe.finance.securities.eod;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Activity;
import yokwe.finance.securities.util.DoubleUtil;

public class Report {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Report.class);
	
	public static final String TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now());

	public static final String URL_ACTIVITY      = "file:///home/hasegawa/Dropbox/Trade/投資活動.ods";
	public static final String URL_ACTIVITY_TEST = "file:///home/hasegawa/Dropbox/Trade/投資活動_TEST.ods";
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/TAX_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/TAX_REPORT_%s.ods", TIMESTAMP);

	// Create daily profit report
	private static class Stock {
		private static double ALMOST_ZERO = 0.000001;
		private static boolean isZero(double value) {
			return -ALMOST_ZERO < value && value < ALMOST_ZERO;
		}
		static Map<String, Stock> map = new TreeMap<>();
		
		private static class Transaction {
			String date;
			double quantity;
			double cost;
			
			Transaction(String date, double quantity, double cost) {
				this.date     = date;
				this.quantity = quantity;
				this.cost     = cost;
			}
			
			@Override
			public String toString() {
				return String.format("%s %10.5f  %8.2f", date, quantity, cost);
			}
		}
		
		String            symbol;
		//
		double            totalQuantity;
		double            totalCost;
		List<Transaction> history;
		
		public Stock(String symbol) {
			this.symbol        = symbol;
			this.totalQuantity = 0;
			this.totalCost     = 0;			
			this.history       = new ArrayList<>();
		}
		
		@Override
		public String toString() {
			return String.format("%-10s  %10.5f  %8.2f", symbol, totalQuantity, totalCost);
		}
		
		public static void buy(String date, String symbol, double quantity, double price, double commission) {
			Stock stock;
			if (map.containsKey(symbol)) {
				stock = map.get(symbol);
			} else {
				stock = new Stock(symbol);
				map.put(symbol, stock);
			}
			// Shortcut
			if (isZero(stock.totalQuantity + quantity)) {
				map.remove(symbol);
				return;
			}

			double cost = DoubleUtil.round((quantity * price) + commission, 2);
			
			stock.totalQuantity = DoubleUtil.round(stock.totalQuantity + quantity, 5);
			stock.totalCost     = DoubleUtil.round(stock.totalCost     + cost,     2);
			
			Transaction transaction = new Transaction(date, quantity, cost);
			stock.history.add(transaction);
		}
		public static void sell(String date, String symbol, double quantity, double price, double commission) {
			Stock stock;
			if (map.containsKey(symbol)) {
				stock = map.get(symbol);
			} else {
				logger.error("Unknonw symbol {}", symbol);
				throw new SecuritiesException("Unknonw symbol");
			}
			// Shortcut
			if (isZero(stock.totalQuantity - quantity)) {
				map.remove(symbol);
				return;
			}
			
			// Update history
			{
				double quantitySell = quantity;
				for(Transaction transaction: stock.history) {
					if (isZero(quantitySell)) break;				
					if (transaction.quantity == 0) continue;
					
					if (isZero(transaction.quantity - quantitySell)) {
						quantitySell = 0;
						
						transaction.quantity = 0;
						transaction.cost     = 0;
					} else if (transaction.quantity < quantitySell) {
						quantitySell -= transaction.quantity;
						
						transaction.quantity = 0;
						transaction.cost     = 0;
					} else if (quantitySell < transaction.quantity) {
						double cost = DoubleUtil.round(transaction.cost * (quantitySell / transaction.quantity), 2);
						transaction.quantity = DoubleUtil.round(transaction.quantity - quantitySell, 5);
						transaction.cost     = DoubleUtil.round(transaction.cost     - cost,         2);
						
						quantitySell = 0;
					} else {
						logger.error("Unexpected transaction {}", transaction);
						throw new SecuritiesException("Unexpected");
					}
				}
			}
			
			// Calculate totalCost from history
			double newTotalCost = 0;
			double newTotalQuantity = 0;
			for(Transaction transaction: stock.history) {
				newTotalCost     += transaction.cost;
				newTotalQuantity += transaction.quantity;
			}
			stock.totalQuantity = DoubleUtil.round(newTotalQuantity, 5);
			stock.totalCost     = DoubleUtil.round(newTotalCost,     2);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
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
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						Stock.buy(activity.tradeDate, activity.symbol, activity.quantity, activity.price, activity.commission);
						break;
					}
					case "SOLD":
					case "REDEEMED": {
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						Stock.sell(activity.tradeDate, activity.symbol, activity.quantity, activity.price, activity.commission);
						break;
					}
					default:
						break;
					}
				}
			}
		}
		
		// Current positions
		for(Stock stock: Stock.map.values()) {
			logger.info("Stock  {}", stock);
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
