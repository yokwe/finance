package yokwe.finance.securities.eod.statement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.DateMap;
import yokwe.finance.securities.eod.PriceUtil;
import yokwe.finance.securities.eod.tax.Transaction;

public class Position {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Position.class);

	public static final double NO_VALUE   = -1;
	
	public static class Lot implements Cloneable {
		public final String date;
		public final double price;
		public final double quantity;
		
		public       double remain;
		
		public Lot(String date, double price, double quantity) {
			this.date     = date;
			this.price    = price;
			this.quantity = quantity;
			
			this.remain   = quantity;
		}
		public Lot(Lot that) {
			this.date     = that.date;
			this.price    = that.price;
			this.quantity = that.quantity;
			this.remain   = that.remain;
		}
		
		@Override
		public String toString() {
			return String.format("%s %8.2f %9.5f %9.5f", date, price, quantity, remain);
		}
	}

	public final String    symbol;
	public final List<Lot> lotList;
	
	public Position(String symbol, List<Lot> lotList) {
		this.symbol  = symbol;
		this.lotList = new ArrayList<>(lotList.size());
		for(Lot lot: lotList) {
			this.lotList.add(new Lot(lot));
		}
	}
	public Position(Position that) {
		this(that.symbol, that.lotList);
	}
	public Position(String symbol) {
		this(symbol, new ArrayList<Lot>(0));
	}
	
	public double getQuantity() {
		double ret = 0;
		for(Lot lot: lotList) {
			ret = Transaction.roundQuantity(ret + lot.remain);
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return String.format("[%s %.5f]", symbol, getQuantity());
	}
	
	private static Map<String, Position>   positionMap = new TreeMap<>();
	public static void buy(Transaction transaction) {
		String symbol = transaction.symbol;
		
		Position position;
		if (positionMap.containsKey(symbol)) {
			position = positionMap.get(symbol);
		} else {
			position = new Position(symbol);
			positionMap.put(symbol, position);
		}
		
		position.lotList.add(new Lot(transaction.date, transaction.price, transaction.quantity));
		
		// Update dateMap
		dateMap.put(transaction.date, getPositionList());
	}
	public static void sell(Transaction transaction) {
		String symbol = transaction.symbol;

		Position position;
		if (positionMap.containsKey(symbol)) {
			position = positionMap.get(symbol);
		} else {
			logger.error("Unknown symbol  {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
		
		double quantity = transaction.quantity;
		
		for(Lot lot: position.lotList) {
			if (quantity == 0) break;
			if (lot.remain == 0) continue;
			
			if (quantity <= lot.remain) {
				lot.remain = Transaction.roundQuantity(lot.remain - quantity);
				quantity   = 0;
			} else {
				quantity   = Transaction.roundQuantity(quantity - lot.remain);
				lot.remain = 0;
			}
			
			// Sanity check
			if (quantity < 0) {
				logger.error("quantity < 0  {}", String.format("%.5f", quantity));
				throw new SecuritiesException("Unexpected");
			}
			if (lot.remain < 0) {
				logger.error("lot.remain < 0  {}", String.format("%.5f", lot.remain));
				throw new SecuritiesException("Unexpected");
			}
		}
		if (quantity != 0) {
			logger.error("quantity != 0  {}", String.format("%.5f", quantity));
			throw new SecuritiesException("Unexpected");
		}
		
		// Update dateMap
		dateMap.put(transaction.date, getPositionList());
	}
	public static double cost(Transaction transaction) {
		String symbol = transaction.symbol;

		Position position;
		if (positionMap.containsKey(symbol)) {
			position = positionMap.get(symbol);
		} else {
			logger.error("Unknown symbol  {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
		
		double ret = 0;
		double quantity = transaction.quantity;
		
		for(Lot lot: position.lotList) {
			if (quantity == 0) break;
			if (lot.remain == 0) continue;
			
			if (quantity <= lot.remain) {
				ret      = Transaction.roundPrice(ret + quantity * lot.price);
				quantity = 0;
			} else {
				ret      = Transaction.roundPrice(ret + lot.remain * lot.price);
				quantity = Transaction.roundQuantity(quantity - lot.remain);
			}
			
			// Sanity check
			if (quantity < 0) {
				logger.error("quantity < 0  {}", String.format("%.5f", quantity));
				throw new SecuritiesException("Unexpected");
			}
			if (lot.remain < 0) {
				logger.error("lot.remain < 0  {}", String.format("%.5f", lot.remain));
				throw new SecuritiesException("Unexpected");
			}
		}
		if (quantity != 0) {
			logger.error("quantity != 0  {}", String.format("%.5f", quantity));
			throw new SecuritiesException("Unexpected");
		}
		
		return ret;
	}
	//Position.change(transaction.date, transaction.symbol, transaction.quantity, transaction.newSymbol, transaction.newQuantity);
	public static void change(Transaction transaction) {
		String symbol = transaction.symbol;
		
		Position position;
		if (positionMap.containsKey(symbol)) {
			position = positionMap.get(symbol);
		} else {
			logger.error("Unknown symbol  {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
		// Sanity check
		if (position.getQuantity() != -transaction.quantity) {
			logger.error("Unexpected quantity pos {}  {} != {}", symbol, transaction.quantity, position.getQuantity());
			throw new SecuritiesException("Unexpected");
		}
		if (transaction.newQuantity != -transaction.quantity) {
			// We can handle *ONLY* same quantity for change for now
			logger.error("Unexpected quantity new {}  {} != {}", symbol, transaction.newQuantity, transaction.quantity);
			throw new SecuritiesException("Unexpected");
		}
		// Delete old symbol
		positionMap.remove(symbol);
		// Add new symbol
		positionMap.put(transaction.newSymbol, new Position(transaction.newSymbol, position.lotList));
		
		// Update dateMap
		dateMap.put(transaction.date, getPositionList());
	}

	
	private static double getUnrealizedValue(String date, Position position) {
		double ret = 0;
		
		String symbol   = position.symbol;
		double quantity = position.getQuantity();
		
		if (PriceUtil.contains(symbol, date)) {
			double price = PriceUtil.getClose(symbol, date);
			double value = price * quantity;
			
			ret = Transaction.roundPrice(ret + value);
		} else {
			// price of symbol at the date is not available
			return NO_VALUE;
		}
		return ret;
	}

	// This method is used to get unrealized value of stocks. Value is not accurate. Because commission is fixed values.
	private static double getUnrealizedValue(String date, List<Position> positionList) {
		boolean noValue = false;
		double ret = 0;
		
		for(Position position: positionList) {
			double value = getUnrealizedValue(date, position);
			if (value == NO_VALUE) noValue = true;
			
			ret = Transaction.roundPrice(ret + value);
		}
		return noValue ? NO_VALUE : ret;
	}
	
	private static DateMap<List<Position>> dateMap     = new DateMap<>();
	private static List<Position> getPositionList() {
		List<Position> ret = new ArrayList<>();
		for(Position position: positionMap.values()) {
			if (position.getQuantity() == 0) continue;
			ret.add(new Position(position));
		}
		return ret;
	}
	public static double getUnrealizedValue(String date) {
		return getUnrealizedValue(date, dateMap.get(date));
	}

}
