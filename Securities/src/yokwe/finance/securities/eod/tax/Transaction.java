package yokwe.finance.securities.eod.tax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.ForexUtil;
import yokwe.finance.securities.eod.Market;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class Transaction implements Comparable<Transaction> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);

	public enum Type {
		WIRE_IN, WIRE_OUT, ACH_IN, ACH_OUT,
		INTEREST, DIVIDEND, BUY, SELL, CHANGE,
	}

	public  static final String         FILLER = "*NA*";
	
	private static       int    nextId = 1;
	
	public final int            id;
	public final Type           type;
	public final String         date;
	public final String         symbol;
	public final String         name;
	public final double         quantity;
	public final double         price;
	public final double         fee;
	public final double         debit;
	public final double         credit;
	
	// for name change
	public final String         newSymbol;
	public final String         newName;
	public final double         newQuantity;
	
	// USDJPY
	public final double         fxRate;
	
	
	private Transaction(Type type, String date, String symbol, String name, double quantity, double price, double fee, double debit, double credit,
			String newSymbol, String newName, double newQuantity) {
		double fxRate = ForexUtil.getUSD(date);
		
		this.id           = nextId++;
		this.type         = type;
		this.date         = date;
		this.symbol       = symbol;
		this.name         = name;
		this.quantity     = roundQuantity(quantity);
		this.price        = roundQuantity(price);
		this.fee          = roundPrice(fee);
		this.debit        = roundPrice(debit);
		this.credit       = roundPrice(credit);
		
		this.newSymbol    = newSymbol;
		this.newName      = newName;
		this.newQuantity  = newQuantity;
		
		this.fxRate       = roundPrice(fxRate);
		
		// Sanity check
		if (!DoubleUtil.isAlmostEqual(fxRate, this.fxRate)) {
			logger.error("fxRate  {}  {}", fxRate, this.fxRate);
			throw new SecuritiesException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(quantity, this.quantity)) {
			logger.error("quantity  {}  {}", quantity, this.quantity);
			throw new SecuritiesException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(price, this.price)) {
			logger.error("price  {}  {}", price, this.price);
			throw new SecuritiesException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(fee, this.fee)) {
			logger.error("fee  {}  {}", fee, this.fee);
			throw new SecuritiesException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(debit, this.debit)) {
			logger.error("debit  {}  {}", debit, this.debit);
			throw new SecuritiesException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(credit, this.credit)) {
			logger.error("fxRate  {}  {}", credit, this.credit);
			throw new SecuritiesException("Unexpected");
		}
	}
	private Transaction(Type type, String date, String symbol, String name, double quantity, double price, double fee, double debit, double credit) {
		this(type, date, symbol, name, quantity, price, fee, debit, credit, "", "", 0);
	}
	
	@Override
	public String toString() {
		return String.format("%-9s %10s %-10s %10.5f %10.5f %5.2f %8.2f %8.2f %-10s %10.5f  %6.2f",
				type, date, symbol, quantity, price, fee, debit, credit, newSymbol, newQuantity, fxRate);
	}
	
	// To calculate correct Japanese tax,
	// If buy and sell happen in same day, treat as all buy first then sell
	// Order of transaction need to be change, buy and sell per stock for one day
	@Override
	public int compareTo(Transaction that) {
		// Compare date
		int ret = this.date.compareTo(that.date);
		if (ret != 0) return ret;
		
		// Compare type
		ret = this.type.compareTo(that.type);
		if (ret != 0) return ret;
		
		// Compare symbol
		ret = this.symbol.compareTo(that.symbol);
		return ret;
	}

	
	private static Transaction buy(String date, String symbol, String name, double quantity, double price, double fee, double debit) {
		return new Transaction(Type.BUY, date, symbol, name, quantity, price, fee, debit, 0);
	}
	public static Transaction sell(String date, String symbol, String name, double quantity, double price, double fee, double credit) {
		return new Transaction(Type.SELL, date, symbol, name, quantity, price, fee, 0, credit);
	}
	private static Transaction interest(String date, double credit) {
		return new Transaction(Type.INTEREST, date, FILLER, FILLER, 0, 0, 0, 0, credit);
	}
	private static Transaction dividend(String date, String symbol, String name, double quantity, double fee, double debit, double credit) {
		return new Transaction(Type.DIVIDEND, date, symbol, name, quantity, 0, fee, debit, credit);
	}
	private static Transaction achOut(String date, double debit) {
		return new Transaction(Type.ACH_OUT, date, FILLER, FILLER, 0, 0, 0, debit, 0);
	}
	private static Transaction achIn(String date, double credit) {
		return new Transaction(Type.ACH_IN, date, FILLER, FILLER, 0, 0, 0, 0, credit);
	}
	private static Transaction wireOut(String date, double debit) {
		return new Transaction(Type.WIRE_OUT, date, FILLER, FILLER, 0, 0, 0, debit, 0);
	}
	private static Transaction wireIn(String date, double credit) {
		return new Transaction(Type.WIRE_IN, date, FILLER, FILLER, 0, 0, 0, 0, credit);
	}
	private static Transaction change(String date, String symbol, String name, double quantity, String newSymbol, String newName, double newQuantity) {
		return new Transaction(Type.CHANGE, date, symbol, name, quantity, 0, 0, 0, 0, newSymbol, newName, newQuantity);
	}
	
	public static double roundPrice(double value) {
		return DoubleUtil.round(String.format("%.4f", value), 2);
	}
	public static double roundQuantity(double value) {
		return DoubleUtil.round(String.format("%.7f", value), 5);
	}
	
	public static List<Transaction> getTransactionList(SpreadSheet docActivity) {
		return getTransactionList(docActivity, true);
	}
	public static List<Transaction> getTransactionList(SpreadSheet docActivity, boolean useTradeDate) {
		List<Transaction> transactionList = new ArrayList<>();
		
		List<String> sheetNameList = docActivity.getSheetNameList();
		sheetNameList.sort((a, b) -> a.compareTo(b));
		
		for(String sheetName: sheetNameList) {
			if (!sheetName.matches("^20[0-9][0-9]$")) {
				logger.warn("Sheet {} skip", sheetName);
				continue;
			}
			logger.info("Sheet {}", sheetName);
			
			List<Activity> activityList = Sheet.extractSheet(docActivity, Activity.class, sheetName);
			// Need to sort activityList to adjust item order of activityList
			Collections.sort(activityList);
			
			for(Iterator<Activity> iterator = activityList.iterator(); iterator.hasNext();) {
				Activity activity = iterator.next();
				
				// Sanity check
				if (activity.date != null && 0 < activity.date.length()) {
					String date = activity.date;
					if (Market.isClosed(date)) {
						logger.error("Market is closed - date -  {}", activity);
						throw new SecuritiesException("Market is closed");
					}
				} else {
					logger.error("Null date - {}", activity);
					throw new SecuritiesException("Null date");
				}
				if (activity.tradeDate != null && 0 < activity.tradeDate.length()) {
					String date = activity.tradeDate;
					if (Market.isClosed(date)) {
						logger.error("Market is closed - tradeDate -  {}", activity);
						throw new SecuritiesException("Market is closed");
					}
				}
				if (!DoubleUtil.isAlmostEqual(activity.quantity, roundQuantity(activity.quantity))) {
					logger.error("quantity  {}", activity.quantity);
					throw new SecuritiesException("Unexpected");
				}
				if (!DoubleUtil.isAlmostEqual(activity.price, roundQuantity(activity.price))) {
					logger.error("price  {}", activity.price);
					throw new SecuritiesException("Unexpected");
				}
				if (!DoubleUtil.isAlmostEqual(activity.commission, roundPrice(activity.commission))) {
					logger.error("commission  {}", activity.commission);
					throw new SecuritiesException("Unexpected");
				}

				switch(activity.transaction) {
				case "NAME CHG":
				case "MERGER":
				case "REV SPLIT": {
					Activity nextActivity = iterator.next();

					// Sanity check
					// activity
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null  {}", activity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity <= 0) {
						logger.error("quantity <= 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit != 0) {
						logger.error("debit != 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit != 0) {
						logger.error("credit != 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}


					// nextActivity
					if (nextActivity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (nextActivity.tradeDate != null) {
						logger.error("tradeDate != null  {}", nextActivity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (0 <= nextActivity.quantity) {
						logger.error("0 <= quantity  {}", nextActivity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (nextActivity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (nextActivity.commission != 0) {
						logger.error("commission != 0  {}", nextActivity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (nextActivity.debit != 0) {
						logger.error("debit != 0  {}", nextActivity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (nextActivity.credit != 0) {
						logger.error("credit != 0  {}", nextActivity.credit);
						throw new SecuritiesException("Unexpected");
					}
					
					if (!DoubleUtil.isAlmostEqual(nextActivity.quantity, roundQuantity(nextActivity.quantity))) {
						logger.error("quantity  {}", nextActivity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (!DoubleUtil.isAlmostEqual(nextActivity.price, roundQuantity(nextActivity.price))) {
						logger.error("price  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (!DoubleUtil.isAlmostEqual(nextActivity.commission, roundPrice(nextActivity.commission))) {
						logger.error("commission  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}

					
					if (nextActivity.date.equals(activity.date) && nextActivity.transaction.equals(activity.transaction)) {
						String date        = activity.date;
						String newSymbol   = activity.symbol;
						String newName     = activity.name;
						double newQuantity = roundQuantity(activity.quantity);
						
						String symbol      = nextActivity.symbol;
						String name        = nextActivity.name;
						double quantity    = roundQuantity(nextActivity.quantity);
						
						Transaction transaction = Transaction.change(date, symbol, name, quantity, newSymbol, newName, newQuantity);
						logger.info("transaction {}", transaction);
						transactionList.add(transaction);
					} else {
						logger.error("Unexpect transaction  {}  {}", activity.transaction, nextActivity);
						logger.error("activity  {}", activity);
						logger.error("next      {}", nextActivity);
						throw new SecuritiesException("Unexpected");
					}
					
					break;
				}
				case "BOUGHT": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate == null) {
						logger.error("tradeDate == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.date.compareTo(activity.tradeDate) <= 0) {
						logger.error("Wrong tradeDate  {}", activity);
						throw new SecuritiesException("Wrong tradeDate");
					}
					if (activity.quantity <= 0) {
						logger.error("quantity <= 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price <= 0) {
						logger.warn("price <= 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission < 0) {
						logger.error("commission < 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit <= 0) {
						logger.error("debit <= 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit != 0) {
						logger.error("credit != 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = useTradeDate ? activity.tradeDate : activity.date;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = roundQuantity(activity.quantity);
					double price    = roundQuantity(activity.price);
					double fee      = roundPrice(activity.commission);
					double debit    = roundPrice(price * quantity);
					
					// Sanity check
					{
						double roundDebit = roundPrice(activity.debit);
						if (!DoubleUtil.isAlmostEqual((debit + fee), roundDebit)) {
							logger.error("Not equal  debit {}  {}", (debit + fee), roundDebit);
							throw new SecuritiesException("Unexpected");
						}
					}
					
					Transaction transaction = Transaction.buy(date, symbol, name, quantity, price, fee, debit);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "DISTRIB": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity <= 0) {
						logger.error("quantity <= 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.warn("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit != 0) {
						logger.error("debit != 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit != 0) {
						logger.error("credit != 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.date; // use date. not tradeDate.
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = roundQuantity(activity.quantity);
					double price    = 0;
					double fee      = 0;
					double debit    = 0;
					
					// Sanity check
					{
						double roundDebit = roundPrice(activity.debit);
						if (!DoubleUtil.isAlmostEqual((debit + fee), roundDebit)) {
							logger.error("Not equal  debit {}  {}", (debit + fee), roundDebit);
							throw new SecuritiesException("Unexpected");
						}
					}
					
					Transaction transaction = Transaction.buy(date, symbol, name, quantity, price, fee, debit);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "SOLD":
				case "REDEEMED": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate == null) {
						logger.error("tradeDate == null");
						throw new SecuritiesException("Unexpected");
					}
					switch (activity.transaction) {
					case "SOLD":
						if (activity.date.compareTo(activity.tradeDate) <= 0) {
							logger.error("Wrong tradeDate  {}", activity);
							throw new SecuritiesException("Wrong tradeDate");
						}
						break;
					case "REDEEMED":
						if (activity.date.compareTo(activity.tradeDate) != 0) {
							logger.error("Wrong tradeDate  {}", activity);
							throw new SecuritiesException("Wrong tradeDate");
						}
						break;
					default:
						logger.error("Unexpected - transaction {}", activity);
						throw new SecuritiesException("Unexpected - transaction");
					}
					if (activity.quantity <= 0) {
						logger.error("quantity <= 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price == 0) {
						logger.error("price == 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission < 0) {
						logger.error("commission < 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit != 0) {
						logger.error("debit != 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit <= 0) {
						logger.error("credit <= 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = useTradeDate ? activity.tradeDate : activity.date;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = roundQuantity(activity.quantity);
					double price    = roundQuantity(activity.price);
					double fee      = roundPrice(activity.commission);
					double credit   = roundPrice(price * quantity);
					
					// Sanity check
					{
						double roundCredit = roundPrice(activity.credit);
						if (!DoubleUtil.isAlmostEqual((credit - fee), roundCredit)) {
							logger.error("Not equal  credit {}  {}", (credit - fee), roundCredit);
							throw new SecuritiesException("Unexpected");
						}
					}

					Transaction transaction = Transaction.sell(date, symbol, name, quantity, price, fee, credit);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "INTEREST": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null  {}", activity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity != 0) {
						logger.error("quantity != 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit != 0) {
						logger.error("debit != 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit <= 0) {
						logger.error("credit <= 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					String date     = activity.date;
					double credit   = roundPrice(activity.credit);
					
					// Sanity check
					if (!DoubleUtil.isAlmostEqual(activity.credit, credit)) {
						logger.error("Not equal  credit {}  {}", activity.credit, credit);
						throw new SecuritiesException("Unexpected");
					}

					Transaction transaction = Transaction.interest(date, credit);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "DIVIDEND":
				case "ADR":
				case "MLP":
				case "NRA":
				case "CAP GAIN": 
				case "JOURNAL": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null  {}", activity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity == 0) {
						logger.error("quantity == 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit < 0) {
						logger.error("debit < 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit < 0) {
						logger.error("credit < 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					String date     = activity.date;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = roundQuantity(activity.quantity);
					double debit    = roundPrice(activity.debit);
					double credit   = roundPrice(activity.credit);
					
					// Sanity check
					if (!DoubleUtil.isAlmostEqual(activity.debit, debit)) {
						logger.error("debit  {}  {}", activity.debit, debit);
						throw new SecuritiesException("Unexpected");
					}
					if (!DoubleUtil.isAlmostEqual(activity.credit, credit)) {
						logger.error("credit  {}  {}", activity.credit, credit);
						throw new SecuritiesException("Unexpected");
					}

					Transaction transaction = Transaction.dividend(date, symbol, name, quantity, 0, debit, credit);
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "ACH": {
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null  {}", activity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity != 0) {
						logger.error("quantity != 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit < 0) {
						logger.error("debit < 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit < 0) {
						logger.error("credit < 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					String date     = activity.date;
					double debit    = roundPrice(activity.debit);
					double credit   = roundPrice(activity.credit);
					
					// Sanity check
					if (!DoubleUtil.isAlmostEqual(activity.debit, debit)) {
						logger.error("debit  {}  {}", activity.debit, debit);
						throw new SecuritiesException("Unexpected");
					}
					if (!DoubleUtil.isAlmostEqual(activity.credit, credit)) {
						logger.error("credit  {}  {}", activity.credit, credit);
						throw new SecuritiesException("Unexpected");
					}

					
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
					// Sanity check
					if (activity.date == null) {
						logger.error("date == null");
						throw new SecuritiesException("Unexpected");
					}
					if (activity.tradeDate != null) {
						logger.error("tradeDate != null  {}", activity.tradeDate);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.quantity != 0) {
						logger.error("quantity != 0  {}", activity.quantity);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.price != 0) {
						logger.error("price != 0  {}", activity.price);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.commission != 0) {
						logger.error("commission != 0  {}", activity.commission);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.debit < 0) {
						logger.error("debit < 0  {}", activity.debit);
						throw new SecuritiesException("Unexpected");
					}
					if (activity.credit < 0) {
						logger.error("credit < 0  {}", activity.credit);
						throw new SecuritiesException("Unexpected");
					}

					String date     = activity.date;
					double debit    = roundPrice(activity.debit);
					double credit   = roundPrice(activity.credit);
					
					// Sanity check
					if (!DoubleUtil.isAlmostEqual(activity.debit, debit)) {
						logger.error("debit  {}  {}", activity.debit, debit);
						throw new SecuritiesException("Unexpected");
					}
					if (!DoubleUtil.isAlmostEqual(activity.credit, credit)) {
						logger.error("credit  {}  {}", activity.credit, credit);
						throw new SecuritiesException("Unexpected");
					}

					
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
		
		// Sort using compareTo method.
		Collections.sort(transactionList);
		return transactionList;
	}
}