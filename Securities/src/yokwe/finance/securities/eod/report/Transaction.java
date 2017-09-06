package yokwe.finance.securities.eod.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.Market;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.tax.Activity;
import yokwe.finance.securities.util.DoubleUtil;

public class Transaction {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);

	public enum Type {
		WIRE_IN, WIRE_OUT, ACH_IN, ACH_OUT,
		INTEREST, DIVIDEND, BUY, SELL, CHANGE,
	}

	public  static final String         FILLER = "*NA*";
	private static final List<Position> EMPTY_POSITION_LIST = new ArrayList<>();
	
	public final Type           type;
	public final String         date;
	public final String         symbol;
	public final String         name;
	public final double         quantity;
	public final double         debit;
	public final double         credit;
	public final double         sellCost;
	public final List<Position> positionList;
	
	// for name change
	public final String         newSymbol;
	public final String         newName;
	public final double         newQuantity;
	
	
	private Transaction(Type type, String date, String symbol, String name, double quantity, double debit, double credit, double sellCost, List<Position> positionList,
			String newSymbol, String newName, double newQuantity) {
		this.type         = type;
		this.date         = date;
		this.symbol       = symbol;
		this.name         = name;
		this.quantity     = quantity;
		this.debit        = debit;
		this.credit       = credit;
		this.sellCost     = sellCost;
		this.positionList = positionList;
		
		this.newSymbol    = newSymbol;
		this.newName      = newName;
		this.newQuantity  = newQuantity;
	}
	private Transaction(Type type, String date, String symbol, String name, double quantity, double debit, double credit, double sellCost, List<Position> positionList) {
		this(type, date, symbol, name, quantity, debit, credit, sellCost, positionList, "", "", 0);
	}
	private Transaction(Type type, String date, String symbol, String name, double quantity, double debit, double credit) {
		this(type, date, symbol, name, quantity, debit, credit, 0, EMPTY_POSITION_LIST, "", "", 0);
	}
	
	@Override
	public String toString() {
		return String.format("%-9s %10s %-10s %10.5f %8.2f %8.2f %8.2f %2d %-10s %8.2f",
				type, date, symbol, quantity, debit, credit, sellCost, positionList.size(), newSymbol, newQuantity);
	}
	
	public static Transaction buy(String date, String symbol, String name, double quantity, double debit, List<Position> positionList) {
		return new Transaction(Type.BUY, date, symbol, name, quantity, debit, 0, 0, positionList);
	}
	public static Transaction sell(String date, String symbol, String name, double quantity, double credit, double sellCost, List<Position> positionList) {
		return new Transaction(Type.SELL, date, symbol, name, quantity, 0, credit, sellCost, positionList);
	}
	public static Transaction interest(String date, double credit) {
		return new Transaction(Type.INTEREST, date, FILLER, FILLER, 0, 0, credit);
	}
	public static Transaction dividend(String date, String symbol, String name, double quantity, double debit, double credit) {
		return new Transaction(Type.DIVIDEND, date, symbol, name, quantity, debit, credit);
	}
	public static Transaction achOut(String date, double debit) {
		return new Transaction(Type.ACH_OUT, date, FILLER, FILLER, 0, debit, 0);
	}
	public static Transaction achIn(String date, double credit) {
		return new Transaction(Type.ACH_IN, date, FILLER, FILLER, 0, 0, credit);
	}
	public static Transaction wireOut(String date, double debit) {
		return new Transaction(Type.WIRE_OUT, date, FILLER, FILLER, 0, debit, 0);
	}
	public static Transaction wireIn(String date, double credit) {
		return new Transaction(Type.WIRE_IN, date, FILLER, FILLER, 0, 0, credit);
	}
	public static Transaction change(String date, String symbol, String name, double quantity, String newSymbol, String newName, double newQuantity, List<Position> positionList) {
		return new Transaction(Type.CHANGE, date, symbol, name, quantity, 0, 0, 0, positionList, newSymbol, newName, newQuantity);
	}
	
	public static double roundPrice(double value) {
		return DoubleUtil.round(value, 2);
	}
	public static double roundQuantity(double value) {
		return DoubleUtil.round(value, 5);
	}
	
	public static List<Transaction> getTransactionList(SpreadSheet docActivity) {
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
				{
					if (activity.date != null && 0 < activity.date.length()) {
						String date = activity.date;
						if (Market.isClosed(date)) {
							logger.error("Market is closed - date -  {}", activity);
							throw new SecuritiesException("Market is closed");
						}
					}
					if (activity.tradeDate != null && 0 < activity.tradeDate.length()) {
						String date = activity.tradeDate;
						if (Market.isClosed(date)) {
							logger.error("Market is closed - tradeDate -  {}", activity);
							throw new SecuritiesException("Market is closed");
						}
					}
				}
				switch(activity.transaction) {
				// TODO temporary ignore old "NAME CHG"
				case "NAME CHG": {
					break;
				}
				// TODO use "NAME CHG" and "MERGER" after everything works as expected.
				case "*NAME CHG":
				case "*MERGER": {
					Activity nextActivity = iterator.next();
					if (nextActivity.date.equals(activity.date) && nextActivity.transaction.equals(activity.transaction)) {
						String date        = activity.date;
						String newSymbol   = activity.symbol;
						String newName     = activity.name;
						double newQuantity = activity.quantity;
						
						String symbol      = nextActivity.symbol;
						String name        = nextActivity.name;
						double quantity    = nextActivity.quantity;
						
						Stock.change(date, symbol, quantity, newSymbol, newQuantity);
						
						Transaction transaction = Transaction.change(date, symbol, name, quantity, newSymbol, newName, newQuantity, Stock.getPositionList());
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
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = activity.quantity;
					double total    = roundPrice((activity.price * activity.quantity) + activity.commission);
					
					Stock.buy(date, symbol, quantity, total);
					Transaction transaction = Transaction.buy(date, symbol, name, quantity, total, Stock.getPositionList());
					logger.info("transaction {}", transaction);
					transactionList.add(transaction);
					break;
				}
				case "SOLD":
				case "REDEEMED": {
//					logger.info("activity {} {} {} {} {}", sheetName, activity.date, activity.transaction, activity.symbol, activity.quantity);
					String date     = activity.tradeDate;
					String symbol   = activity.symbol;
					String name     = activity.name;
					double quantity = activity.quantity;
					double total    = roundPrice((activity.price * activity.quantity) - activity.commission);

					double sellCost = Stock.sell(date, symbol, quantity, total);
					Transaction transaction = Transaction.sell(date, symbol, name, quantity, total, sellCost, Stock.getPositionList());
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
					String name     = activity.name;
					double quantity = activity.quantity;
					double debit    = activity.debit;
					double credit   = activity.credit;

					Transaction transaction = Transaction.dividend(date, symbol, name, quantity, debit, credit);
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
		return transactionList;
	}
}