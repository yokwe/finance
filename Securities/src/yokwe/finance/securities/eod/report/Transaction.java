package yokwe.finance.securities.eod.report;

import java.util.ArrayList;
import java.util.List;

public class Transaction implements Comparable<Transaction> {
	enum Type {
		WIRE_IN, WIRE_OUT, ACH_IN, ACH_OUT,
		INTEREST, DIVIDEND, BUY, SELL,
	}

	public  static final String         FILLER = "*NA*";
	private static final List<Position> EMPTY_POSITION_LIST = new ArrayList<>();
	
	public final Type           type;
	public final String         date;
	public final String         symbol;
	public final double         quantity;
	public final double         debit;
	public final double         credit;
	public final double         sellCost;
	public final List<Position> positionList;
	
	private Transaction(Type type, String date, String symbol, double quantity, double debit, double credit, double sellCost, List<Position> positionList) {
		this.type         = type;
		this.date         = date;
		this.symbol       = symbol;
		this.quantity     = quantity;
		this.debit        = debit;
		this.credit       = credit;
		this.sellCost     = sellCost;
		this.positionList = positionList;
	}
	private Transaction(Type type, String date, String symbol, double quantity, double debit, double credit) {
		this(type, date, symbol, quantity, debit, credit, 0, EMPTY_POSITION_LIST);
	}
	
	@Override
	public String toString() {
		return String.format("%-8s %10s %-10s %10.5f %8.2f %8.2f %8.2f %s", type, date, symbol, quantity, debit, credit, sellCost, positionList);
	}
	
	public static Transaction buy(String date, String symbol, double quantity, double debit, List<Position> positionList) {
		return new Transaction(Type.BUY, date, symbol, quantity, debit, 0, 0, positionList);
	}
	public static Transaction sell(String date, String symbol, double quantity, double credit, double sellCost, List<Position> positionList) {
		return new Transaction(Type.SELL, date, symbol, quantity, 0, credit, sellCost, positionList);
	}
	public static Transaction interest(String date, double credit) {
		return new Transaction(Type.INTEREST, date, FILLER, 0, 0, credit);
	}
	public static Transaction dividend(String date, String symbol, double debit, double credit) {
		return new Transaction(Type.DIVIDEND, date, symbol, 0, debit, credit);
	}
	public static Transaction achOut(String date, double debit) {
		return new Transaction(Type.ACH_OUT, date, FILLER, 0, debit, 0);
	}
	public static Transaction achIn(String date, double credit) {
		return new Transaction(Type.ACH_IN, date, FILLER, 0, 0, credit);
	}
	public static Transaction wireOut(String date, double debit) {
		return new Transaction(Type.WIRE_OUT, date, FILLER, 0, debit, 0);
	}
	public static Transaction wireIn(String date, double credit) {
		return new Transaction(Type.WIRE_IN, date, FILLER, 0, 0, credit);
	}

	@Override
	public int compareTo(Transaction that) {
		if (this.date.equals(that.date)) {
			return this.symbol.compareTo(that.symbol);
		} else {
			return this.date.compareTo(that.date);
		}
	}
}