package yokwe.finance.securities.eod.statement;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.tax.Transaction;

public class Transfer {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transfer.class);

	// Provide class that contains enough information to spread sheet 譲渡明細 and 譲渡計算明細書
	public static class Buy {
		public final String date;
		public final String symbol;
		public final String name;
		
		public final double quantity;
		public final double price;
		public final double fee;
		
		public final double buy;
		
		public final double totalQuantity;
		public final double totalCost;
		
		public Buy(String date, String symbol, String name,
				double quantity, double price, double fee,
				double totalQuantity, double totalCost) {
			this.date          = date;
			this.symbol        = symbol;
			this.name          = name;
			this.quantity      = quantity;
			this.price         = price;
			this.fee           = fee;
			this.buy           = Transaction.roundPrice(this.price * this.quantity);
			this.totalQuantity = totalQuantity;
			this.totalCost     = totalCost;
		}
	}
	
	public static class Sell {
		public final String date;
		public final String symbol;
		public final String name;
		
		public final double quantity;
		public final double price;
		public final double fee;
		
		public final double sell;
		
		public final double cost;
		
		public final double totalQuantity;
		public final double totalCost;
		
		public Sell(String date, String symbol, String name,
			double quantity, double price, double fee,
			double cost,
			double totalQuantity, double totalCost) {
			this.date          = date;
			this.symbol        = symbol;
			this.name          = name;
			this.quantity      = quantity;
			this.price         = price;
			this.fee           = fee;
			
			this.sell          = Transaction.roundPrice(this.price * this.quantity);
			
			this.cost          = cost;
			
			this.totalQuantity = totalQuantity;
			this.totalCost     = totalCost;
		}
	}
	
	private static Map<Integer, Transfer> all = new TreeMap<>();
	public static Transfer getByID(int id) {
		if (all.containsKey(id)) {
			return all.get(id);
		} else {
			logger.error("Unknown id  {}", id);
			throw new SecuritiesException("Unexpected");
		}
	}
	
	public Transfer(int id, Buy buy) {
		this.id   = id;
		this.buy  = buy;
		this.sell = null;
		
		all.put(this.id, this);
	}
	
	public Transfer(int id, Sell sell) {
		this.id   = id;
		this.buy  = null;
		this.sell = sell;
		
		all.put(this.id, this);
	}
	
	public Transfer(int id, Buy buy, Sell sell) {
		this.id   = id;
		this.buy  = buy;
		this.sell = sell;
		
		all.put(this.id, this);
	}
	
	public final int  id;
	public final Buy  buy;
	public final Sell sell;
}
