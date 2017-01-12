package yokwe.finance.securities.tax;

public class Transfer {
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
			this.buy           = this.price * this.quantity;
			this.buyJPY        = (int)Math.round(this.buy * this.fxRate);
			this.feeJPY        = (int)Math.round(this.fee * this.fxRate);
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
		
		public final double dividend;

		public final double totalQuantity;
		public final double totalCost;
		public final int    totalCostJPY;
		
		public Sell(String date, String symbol, String name,
			double quantity, double price, double fee, double fxRate,
			double cost, int costJPY,
			String dateFirst, String dateLast,
			double dividend,
			double totalQuantity, double totalCost, int totalCostJPY) {
			this.date          = date;
			this.symbol        = symbol;
			this.name          = name;
			this.quantity      = quantity;
			this.price         = price;
			this.fee           = fee;
			this.fxRate        = fxRate;
			
			this.sell          = this.price * this.quantity;
			this.sellJPY       = (int)Math.round(this.sell * this.fxRate);
			this.feeJPY        = (int)Math.round(this.fee * this.fxRate);
			
			this.cost          = cost;
			this.costJPY       = costJPY;
			
			this.dateFirst     = dateFirst;
			this.dateLast      = dateLast;
			
			this.dividend      = dividend;
			
			this.totalQuantity = totalQuantity;
			this.totalCost     = totalCost;
			this.totalCostJPY  = totalCostJPY;			
		}
	}
	
	public Transfer(Buy buy) {
		this.buy  = buy;
		this.sell = null;
	}
	
	public Transfer(Sell sell) {
		this.buy  = null;
		this.sell = sell;
	}
	
	public Transfer(Buy buy, Sell sell) {
		this.buy  = buy;
		this.sell = sell;
	}
	
	public final Buy  buy;
	public final Sell sell;
}
