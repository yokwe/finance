package yokwe.finance.stock.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.libreoffice.SpreadSheet;
import yokwe.finance.stock.util.DoubleUtil;

public class Transaction implements Comparable<Transaction> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transaction.class);

	public enum Type {
		DEPOSIT,  // Increase cash
		WITHDRAW, // Decrease cash
		INTEREST, // Interest of account
		DIVIDEND, // Dividend of stock
		BUY,      // Buy stock   *NOTE* Buy must  be before SELL
		SELL,     // Sell stock  *NOTE* Sell must be after BUY
		CHANGE,   // Stock split, reverse split or symbol change
	}

	public  static final String         FILLER = "*NA*";
	
	// All number (quantity, price, fee, debit, credit and newQuantity) must be positive.
	public final Type           type;
	public final String         date;
	public final String         symbol;
	public final double         quantity;
	public final double         price;
	public final double         fee;
	public final double         debit;    // Actual amount subtract from account - contains fee
	public final double         credit;   // Actual amount add to account        - contains fee
	
	// for name change
	public final String         newSymbol;
	public final double         newQuantity;
	
	
	private Transaction(Type type, String date, String symbol, double quantity, double price, double fee, double debit, double credit,
			String newSymbol, double newQuantity) {
		this.type         = type;
		this.date         = date;
		this.symbol       = symbol;
		this.quantity     = DoubleUtil.roundQuantity(quantity);
		this.price        = DoubleUtil.roundQuantity(price);
		this.fee          = DoubleUtil.roundPrice(fee);
		this.debit        = DoubleUtil.roundPrice(debit);
		this.credit       = DoubleUtil.roundPrice(credit);
		
		this.newSymbol    = newSymbol;
		this.newQuantity  = DoubleUtil.roundQuantity(newQuantity);
		
		// Sanity check
		if (quantity < 0) {
			logger.error("quantity  {}", quantity);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(quantity, this.quantity)) {
			logger.error("quantity  {}  {}", quantity, this.quantity);
			throw new UnexpectedException("Unexpected");
		}
		if (price < 0) {
			logger.error("price  {}", price);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(price, this.price)) {
			logger.error("price  {}  {}", price, this.price);
			throw new UnexpectedException("Unexpected");
		}
		if (fee < 0) {
			logger.error("fee    {}", fee);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(fee, this.fee)) {
			logger.error("fee  {}  {}", fee, this.fee);
			throw new UnexpectedException("Unexpected");
		}
		if (debit < 0) {
			logger.error("debit  {}", debit);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(debit, this.debit)) {
			logger.error("debit  {}  {}", debit, this.debit);
			throw new UnexpectedException("Unexpected");
		}
		if (credit < 0) {
			logger.error("credit {}", credit);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(credit, this.credit)) {
			logger.error("fxRate  {}  {}", credit, this.credit);
			throw new UnexpectedException("Unexpected");
		}
		if (newQuantity < 0) {
			logger.error("newQuantity {}", newQuantity);
			throw new UnexpectedException("Unexpected");
		}
		if (!DoubleUtil.isAlmostEqual(newQuantity, this.newQuantity)) {
			logger.error("newQuantity {}  {}", newQuantity, this.newQuantity);
			throw new UnexpectedException("Unexpected");
		}
	}
	
	private Transaction(Type type, String date, String symbol, double quantity, double price, double fee, double debit, double credit) {
		this(type, date, symbol, quantity, price, fee, debit, credit, FILLER, 0);
	}

	@Override
	public String toString() {
		if (type == Type.CHANGE) {
			return String.format("%-9s %10s %-10s %10.5f %10.5f %5.2f %8.2f %8.2f %-10s %10.5f",
					type, date, symbol, quantity, price, fee, debit, credit, newSymbol, newQuantity);
		} else {
			return String.format("%-9s %10s %-10s %10.5f %10.5f %5.2f %8.2f %8.2f",
					type, date, symbol, quantity, price, fee, debit, credit);
		}
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

	public static Transaction deposit(String date, double credit) {
		return new Transaction(Type.DEPOSIT, date, FILLER, 0, 0, 0, 0, credit);
	}
	public static Transaction withdraw(String date, double debit) {
		return new Transaction(Type.WITHDRAW, date, FILLER, 0, 0, 0, debit, 0);
	}
	public static Transaction interest(String date, double credit) {
		return new Transaction(Type.INTEREST, date, FILLER, 0, 0, 0, 0, credit);
	}
	public static Transaction dividend(String date, String symbol, double quantity, double fee, double debit, double credit) {
		return new Transaction(Type.DIVIDEND, date, symbol, quantity, 0, fee, debit, credit);
	}
	public static Transaction buy(String date, String symbol, double quantity, double price, double fee, double debit) {
		return new Transaction(Type.BUY, date, symbol, quantity, price, fee, debit, 0);
	}
	public static Transaction sell(String date, String symbol, double quantity, double price, double fee, double credit) {
		return new Transaction(Type.SELL, date, symbol, quantity, price, fee, 0, credit);
	}
	public static Transaction change(String date, String symbol, double quantity, String newSymbol, double newQuantity) {
		return new Transaction(Type.CHANGE, date, symbol, quantity, 0, 0, 0, 0, newSymbol, newQuantity);
	}
	
	public static List<Transaction> getFirstrade() {
		final List<yokwe.finance.stock.firstrade.Transaction> othreTransactionList;
		
		try (SpreadSheet docActivity = new SpreadSheet(yokwe.finance.stock.firstrade.Transaction.URL_ACTIVITY, true)) {
			// Create transaction from activity using tradeDate
			othreTransactionList = yokwe.finance.stock.firstrade.Transaction.getTransactionList(docActivity, true);
		}
		
		List<Transaction> transactionList = new ArrayList<>();
		
		for(yokwe.finance.stock.firstrade.Transaction otherTransaction: othreTransactionList) {
			final Transaction transaction;
			switch(otherTransaction.type) {
			case WIRE_IN:
			case ACH_IN:
				transaction = Transaction.deposit(otherTransaction.date, otherTransaction.credit);
				break;
			case WIRE_OUT:
			case ACH_OUT:
				transaction = Transaction.withdraw(otherTransaction.date, otherTransaction.debit);
				break;
			case INTEREST:
				transaction = Transaction.interest(otherTransaction.date, otherTransaction.credit);
				break;
			case DIVIDEND:
				transaction = Transaction.dividend(otherTransaction.date, otherTransaction.symbol, otherTransaction.quantity, otherTransaction.fee,
					otherTransaction.debit, otherTransaction.credit);
				break;
			case BUY:
				transaction = Transaction.buy(otherTransaction.date, otherTransaction.symbol, otherTransaction.quantity, otherTransaction.price, otherTransaction.fee,
					DoubleUtil.roundPrice(otherTransaction.debit + otherTransaction.fee));
				break;
			case SELL:
				transaction = Transaction.sell(otherTransaction.date, otherTransaction.symbol, otherTransaction.quantity, otherTransaction.price, otherTransaction.fee,
					DoubleUtil.roundPrice(otherTransaction.credit - otherTransaction.fee));
				break;
			case CHANGE:
				transaction = Transaction.change(otherTransaction.date, otherTransaction.symbol, -otherTransaction.quantity,
					otherTransaction.newSymbol, otherTransaction.newQuantity);
				break;
			default:
				logger.error("Unknown type {}", otherTransaction.type);
				throw new UnexpectedException("Unknown type");
			}
			
			transactionList.add(transaction);
		}
		
		// Sort using compareTo method.
		Collections.sort(transactionList);
		return transactionList;
	}
	
	public static List<Transaction> getMonex() {
		final List<yokwe.finance.stock.monex.Transaction> othreTransactionList;
		
		try (SpreadSheet docActivity = new SpreadSheet(yokwe.finance.stock.monex.Transaction.URL_ACTIVITY, true)) {
			// Create transaction from activity using tradeDate
			othreTransactionList = yokwe.finance.stock.monex.Transaction.getTransactionList(docActivity);
		}

		List<Transaction> transactionList = new ArrayList<>();
		
		for(yokwe.finance.stock.monex.Transaction otherTransaction: othreTransactionList) {
			final Transaction transaction;
			switch(otherTransaction.type) {
			case USD_IN:
				transaction = Transaction.deposit(otherTransaction.date, otherTransaction.usd);
				break;
			case USD_OUT:
				transaction = Transaction.withdraw(otherTransaction.date, -otherTransaction.usd);
				break;
			case BUY:
				transaction = Transaction.buy(otherTransaction.date, otherTransaction.symbol, otherTransaction.quantity, otherTransaction.price, otherTransaction.fee, otherTransaction.total);
				break;
			case SELL:
				transaction = Transaction.sell(otherTransaction.date, otherTransaction.symbol, otherTransaction.quantity, otherTransaction.price, otherTransaction.fee, otherTransaction.total);
				break;
			case JPY_IN:
			case JPY_OUT:
				transaction = null;
				break;
			default:
				logger.error("Unknown type {}", otherTransaction.type);
				throw new UnexpectedException("Unknown type");
			}
			
			if (transaction != null) transactionList.add(transaction);
		}
		
		// Sort using compareTo method.
		Collections.sort(transactionList);
		return transactionList;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			List<Transaction> transactionList = getMonex();
			for(Transaction transaciton: transactionList) {
				logger.info("monex     {}", transaciton);
			}
		}
		
		{
			List<Transaction> transactionList = getFirstrade();
			for(Transaction transaciton: transactionList) {
				logger.info("firstrade {}", transaciton);
			}
		}
		
		logger.info("END");
		System.exit(0);
	}
}