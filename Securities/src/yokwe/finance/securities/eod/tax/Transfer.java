package yokwe.finance.securities.eod.tax;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.DoubleUtil;

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
		public final double fxRate;
		
		public final double buy;
		public final int    buyJPY;
		public final int    feeJPY;
		
		public final double totalQuantity;
		public final double totalCost;
		public final int    totalCostJPY;
		
		public Buy(String date, String symbol, String name,
				double quantity, double price, double fee, double fxRate,
				double totalQuantity, double totalCost, int totalCostJPY) {
			this.date          = date;
			this.symbol        = symbol;
			this.name          = name;
			this.quantity      = quantity;
			this.price         = price;
			this.fee           = fee;
			this.fxRate        = fxRate;
			this.buy           = DoubleUtil.round(this.price * this.quantity, 2);
			this.buyJPY        = (int)Math.floor(this.buy * this.fxRate);
			this.feeJPY        = (int)Math.floor(this.fee * this.fxRate);
			this.totalQuantity = totalQuantity;
			this.totalCost     = totalCost;
			this.totalCostJPY  = totalCostJPY;
		}
	}
	
	public static class Sell {
		public final String date;
		public final String symbol;
		public final String name;
		
		public final double quantity;
		public final double price;
		public final double fee;
		public final double fxRate;
		
		public final double sell;
		public final int    sellJPY;
		public final int    feeJPY;
		
		public final double cost;
		public final int    costJPY;

		public final String dateFirst;
		public final String dateLast;
		
		public final double totalQuantity;
		public final double totalCost;
		public final int    totalCostJPY;
		
		public Sell(String date, String symbol, String name,
			double quantity, double price, double fee, double fxRate,
			double cost, int costJPY,
			String dateFirst, String dateLast,
			double totalQuantity, double totalCost, int totalCostJPY) {
			this.date          = date;
			this.symbol        = symbol;
			this.name          = name;
			this.quantity      = quantity;
			this.price         = price;
			this.fee           = fee;
			this.fxRate        = fxRate;
			
			this.sell          = DoubleUtil.round(this.price * this.quantity, 2);
			this.sellJPY       = (int)Math.floor(this.sell * this.fxRate);
			this.feeJPY        = (int)Math.floor(this.fee * this.fxRate);
			
			this.cost          = cost;
			this.costJPY       = costJPY;
			
			this.dateFirst     = dateFirst;
			this.dateLast      = dateLast;
			
			this.totalQuantity = totalQuantity;
			this.totalCost     = totalCost;
			this.totalCostJPY  = totalCostJPY;
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
