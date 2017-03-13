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
	public static final String URL_TEMPLATE      = "file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_TEMPLATE.ods";
	public static final String URL_REPORT        = String.format("file:///home/hasegawa/Dropbox/Trade/EOD_REPORT_%s.ods", TIMESTAMP);

	// Create daily profit report
	
	private static class Transfer {
		private enum Action {
			BUY, SELL,
		}
		
		final Action action;
		final String date;
		final String symbol;
//		final double price;
		final double quantity;
//		final double commission;
		final double total;
		
		private Transfer(Action action, String date, String symbol, double price, double quantity, double commission, double total) {
			this.action     = action;
			this.date       = date;
			this.symbol     = symbol;
//			this.price      = price;
			this.quantity   = quantity;
//			this.commission = commission;
			this.total      = total;
		}
		static Transfer buy(String date, String symbol, double price, double quantity, double commission) {
			return new Transfer(Action.BUY, date, symbol, price, quantity, commission, DoubleUtil.round((price * quantity) + commission, 2));
		}
		static Transfer sell(String date, String symbol, double price, double quantity, double commission) {
			return new Transfer(Action.SELL, date, symbol, price, quantity, commission, DoubleUtil.round((price * quantity) - commission, 2));
		}
	}

	private static class Stock {
		private static Map<String, Stock> map = new TreeMap<>();
		
		static Stock get(String symbol) {
			if (map.containsKey(symbol)) {
				return map.get(symbol);
			} else {
				logger.error("Unknonw symbol {}", symbol);
				throw new SecuritiesException("Unknonw symbol");
			}
		}
		static Stock getOrCreate(String symbol) {
			if (map.containsKey(symbol)) {
				return map.get(symbol);
			} else {
				Stock stock = new Stock(symbol);
				map.put(symbol, stock);
				return stock;
			}
		}
		private static class History {
			String date;
			double quantity;
			double cost;
			
			History(String date, double quantity, double cost) {
				this.date     = date;
				this.quantity = quantity;
				this.cost     = cost;
			}
			
			@Override
			public String toString() {
				return String.format("%s %10.5f  %8.2f", date, quantity, cost);
			}
		}
		
		String        symbol;
		//
		double        totalQuantity;
		double        totalCost;
		List<History> history;
		
		public Stock(String symbol) {
			this.symbol        = symbol;
			this.totalQuantity = 0;
			this.totalCost     = 0;			
			this.history       = new ArrayList<>();
		}
		
		void reset() {
			this.totalQuantity = 0;
			this.totalCost     = 0;
			this.history.clear();
		}
		
		@Override
		public String toString() {
			return String.format("%-10s  %10.5f  %8.2f", symbol, totalQuantity, totalCost);
		}
		
		public static void buy(String date, String symbol, double quantity, double total) {
			Stock stock = Stock.getOrCreate(symbol);
			// Shortcut
			if (DoubleUtil.isAlmostZero(stock.totalQuantity + quantity)) {
				stock.reset();
				return;
			}

			stock.totalQuantity = DoubleUtil.round(stock.totalQuantity + quantity, 5);
			stock.totalCost     = DoubleUtil.round(stock.totalCost     + total,    2);
			
			stock.history.add(new History(date, quantity, total));
		}
		
		// Returns sellCost
		public static double sell(String date, String symbol, double quantity, double total) {
			Stock stock = Stock.get(symbol);
			double totalCostBefore = stock.totalCost;
			
			// Shortcut
			if (DoubleUtil.isAlmostEqual(stock.totalQuantity, quantity)) {
				stock.reset();
				return totalCostBefore;
			}
			
			// Update history
			{
				double quantitySell = quantity;
				for(History history: stock.history) {
					if (DoubleUtil.isAlmostZero(quantitySell)) break;				
					if (history.quantity == 0) continue;
					
					if (DoubleUtil.isAlmostEqual(history.quantity, quantitySell)) {
						quantitySell = 0;
						
						history.quantity = 0;
						history.cost     = 0;
					} else if (history.quantity < quantitySell) {
						quantitySell -= history.quantity;
						
						history.quantity = 0;
						history.cost     = 0;
					} else if (quantitySell < history.quantity) {
						double cost = DoubleUtil.round(history.cost * (quantitySell / history.quantity), 2);
						history.quantity = DoubleUtil.round(history.quantity - quantitySell, 5);
						history.cost     = DoubleUtil.round(history.cost     - cost,         2);
						
						quantitySell = 0;
					} else {
						logger.error("Unexpected history {}", history);
						throw new SecuritiesException("Unexpected");
					}
				}
			}
			
			// Calculate totalCost from history
			double totalCost     = 0;
			double totalQuantity = 0;
			for(History history: stock.history) {
				totalCost     += history.cost;
				totalQuantity += history.quantity;
			}
			stock.totalQuantity = DoubleUtil.round(totalQuantity, 5);
			stock.totalCost     = DoubleUtil.round(totalCost,     2);
			
			return DoubleUtil.round(totalCostBefore - stock.totalCost, 2);
		}
	}
	
	private static class Transaction {
		final String date;
		final String symbol;
		final double quantity;
		final double buy;
		final double sell;
		final double sellCost;
		
		private Transaction(String date, String symbol, double quantity, double buy, double sell, double sellCost) {
			this.date     = date;
			this.symbol   = symbol;
			this.quantity = quantity;
			this.buy      = buy;
			this.sell     = sell;
			this.sellCost = sellCost;
		}
		
		@Override
		public String toString() {
			return String.format("%s %-10s %10.5f %8.2f %8.2f %8.2f", date, symbol, quantity, buy, sell, sellCost);
		}
		
		static Transaction buy(String date, String symbol, double quantity, double buy) {
			return new Transaction(date, symbol, quantity, buy, 0, 0);
		}
		static Transaction sell(String date, String symbol, double quantity, double sell, double sellCost) {
			return new Transaction(date, symbol, quantity, 0, sell, sellCost);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			List<Transfer>    transferList    = new ArrayList<>();
			
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
						Transfer transfer = Transfer.buy(activity.date, activity.symbol, activity.price, activity.quantity, activity.commission);
						transferList.add(transfer);
						break;
					}
					case "SOLD":
					case "REDEEMED": {
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						Transfer transfer = Transfer.sell(activity.date, activity.symbol, activity.price, activity.quantity, activity.commission);
						transferList.add(transfer);
						break;
					}
					default:
						break;
					}
				}
			}
			
			logger.info("transferList {}", transferList.size());
			List<Transaction> transactionList = new ArrayList<>();
			for(Transfer transfer: transferList) {
				switch(transfer.action) {
				case BUY: {
					Stock.buy(transfer.date, transfer.symbol, transfer.quantity, transfer.total);
					Transaction transaction = Transaction.buy(transfer.date, transfer.symbol, transfer.quantity, transfer.total);
					logger.info("transaction BUY   {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case SELL: {
					double sellCost = Stock.sell(transfer.date, transfer.symbol, transfer.quantity, transfer.total);
					Transaction transaction = Transaction.sell(transfer.date, transfer.symbol, transfer.quantity, transfer.total, sellCost);
					logger.info("transaction SELL  {}", transaction);
					transactionList.add(transaction);
					break;
				}
				default:
					break;
				}
			}
			
			// Current positions
			logger.info("Stock.map {}", Stock.map.size());
			for(Stock stock: Stock.map.values()) {
				if (stock.totalQuantity == 0) continue;
				logger.info("Stock  {}", stock);
			}
		}
		
		logger.info("STOP");
		System.exit(0);
	}
}
