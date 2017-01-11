package yokwe.finance.securities.tax;

import org.slf4j.LoggerFactory;

public class Transfer {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Transfer.class);

	// Provide class that contains information to build record of spread sheet 譲渡明細 and 譲渡計算明細書
	public static class Buy {
		public String date;
		public String symbol;
		public String name;
		
		public double quantity;
		public double price;
		public double fee;
		public double fxRate;
		
		public double buy;
		public int    buyJPY;
		
		public double totalQuantity;
		public double totalBuy;
		public int    totalBuyJPY;
	}
	
	public static class Sell {
		public String date;
		public String symbol;
		public String name;
		
		public double quantity;
		public double price;
		public double fee;
		public double fxRate;
		
		public double sell;
		public int    sellJPY;

		public String dateFirst;
		public String dateLast;
		
		public double dividend;
	}
	
	public static class BuySell {
		Buy  buy;
		Sell sell;
	}
}
