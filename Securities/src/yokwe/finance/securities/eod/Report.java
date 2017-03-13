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
		enum Type {
			WIRE_IN, WIRE_OUT, ACH_IN, ACH_OUT,
			INTEREST, DIVIDEND, BUY, SELL,
		}

		static final String FILLER = "*NA*";
		
		final Type   type;
		final String date;
		final String symbol;
		final double quantity;
		final double debit;
		final double credit;
		final double sellCost;
		
		private Transaction(Type type, String date, String symbol, double quantity, double debit, double credit, double sellCost) {
			this.type     = type;
			this.date     = date;
			this.symbol   = symbol;
			this.quantity = quantity;
			this.debit    = debit;
			this.credit   = credit;
			this.sellCost = sellCost;
		}
		
		private Transaction(Type type, String date, String symbol, double quantity, double debit, double credit) {
			this(type, date, symbol, quantity, debit, credit, 0);
		}
		
		@Override
		public String toString() {
			return String.format("%-8s %10s %-10s %10.5f %8.2f %8.2f %8.2f", type, date, symbol, quantity, debit, credit, sellCost);
		}
		
		static Transaction buy(String date, String symbol, double quantity, double debit) {
			return new Transaction(Type.BUY, date, symbol, quantity, debit, 0);
		}
		static Transaction sell(String date, String symbol, double quantity, double credit, double sellCost) {
			return new Transaction(Type.SELL, date, symbol, quantity, 0, credit, sellCost);
		}
		static Transaction interest(String date, double credit) {
			return new Transaction(Type.INTEREST, date, FILLER, 0, 0, credit);
		}
		static Transaction dividend(String date, String symbol, double debit, double credit) {
			return new Transaction(Type.DIVIDEND, date, symbol, 0, debit, credit);
		}
		static Transaction achOut(String date, double debit) {
			return new Transaction(Type.ACH_OUT, date, FILLER, 0, debit, 0);
		}
		static Transaction achIn(String date, double credit) {
			return new Transaction(Type.ACH_IN, date, FILLER, 0, 0, credit);
		}
		static Transaction wireOut(String date, double debit) {
			return new Transaction(Type.WIRE_OUT, date, FILLER, 0, debit, 0);
		}
		static Transaction wireIn(String date, double credit) {
			return new Transaction(Type.WIRE_IN, date, FILLER, 0, 0, credit);
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		try (SpreadSheet docActivity = new SpreadSheet(URL_ACTIVITY, true)) {
			List<Transaction> transactionList = new ArrayList<>();
			
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
						String date     = activity.date;
						String symbol   = activity.symbol;
						double quantity = activity.quantity;
						double total    = DoubleUtil.round((activity.price * activity.quantity) + activity.commission, 2);
						
						Stock.buy(date, symbol, quantity, total);
						Transaction transaction = Transaction.buy(date, symbol, quantity, total);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "SOLD":
					case "REDEEMED": {
//						logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
						String date     = activity.date;
						String symbol   = activity.symbol;
						double quantity = activity.quantity;
						double total    = DoubleUtil.round((activity.price * activity.quantity) - activity.commission, 2);

						double sellCost = Stock.sell(date, symbol, quantity, total);
						Transaction transaction = Transaction.sell(date, symbol, quantity, total, sellCost);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "INTEREST": {
						String date     = activity.date;
						double credit   = activity.credit;

						Transaction transaction = Transaction.interest(date, credit);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "DIVIDEND":
					case "ADR":
					case "MLP":
					case "NRA":
					case "CAP GAIN": {
						String date     = activity.date;
						String symbol   = activity.symbol;
						double debit    = activity.debit;
						double credit   = activity.credit;

						Transaction transaction = Transaction.dividend(date, symbol, debit, credit);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
						break;
					}
					case "ACH": {
						String date     = activity.date;
						double debit    = activity.debit;
						double credit   = activity.credit;
						
						if (debit != 0) {
							Transaction transaction = Transaction.achOut(date, debit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						if (credit != 0) {
							Transaction transaction = Transaction.achIn(date, credit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						break;
					}
					case "WIRE": {
						String date     = activity.date;
						double debit    = activity.debit;
						double credit   = activity.credit;
						
						if (debit != 0) {
							Transaction transaction = Transaction.wireOut(date, debit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						if (credit != 0) {
							Transaction transaction = Transaction.wireIn(date, credit);
							logger.info("transaction {}", transaction);
							transactionList.add(transaction);
						}
						break;
					}
					default:
						logger.error("Unknown transaction {}", activity.transaction);
						throw new SecuritiesException("Unknown transaction");
					}
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
