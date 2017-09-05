package yokwe.finance.securities.eod.report;

import java.util.ArrayList;
import java.util.List;

public class Transaction {
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
		return String.format("%-9s %10s %-10s %-10s %10.5f %8.2f %8.2f %8.2f %2d %-9s %-9s %8.2f",
				type, date, symbol, name, quantity, debit, credit, sellCost, positionList.size(), newSymbol, newName, newQuantity);
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
}