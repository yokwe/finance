package yokwe.finance.securities.book;

@SheetData.SheetName("譲渡取引明細")
public class Report  extends SheetData {
	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String symbolName;
	@ColumnName("数量")
	public final double quantity;

	@ColumnName("売約定日")
	public final String dateSell;
	@ColumnName("売値")
	public final String priceSell;
	@ColumnName("売手数料")
	public final String commissionSell;
	@ColumnName("売レート")
	public final String fxRateSell;
	@ColumnName("譲渡手数料")
	public final String commissionSellJPY;
	@ColumnName("譲渡価格")
	public final String amountSellJPY;
	@ColumnName("取得費")
	public final String acquisitionCostJPY;
	@ColumnName("取得日最初")
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	public final String dateBuyLast;
		
	@ColumnName("買約定日")
	public final String dateBuy;
	@ColumnName("買値")
	public final String priceBuy;
	@ColumnName("買手数料")
	public final String commissionBuy;
	@ColumnName("買レート")
	public final String fxRateBuy;
	@ColumnName("取得価格")
	public final String amountBuyJPY;
	@ColumnName("総数量")
	public final String totalQuantity;
	@ColumnName("総取得価格")
	public final String totalAmountBuyJPY;
	
	private Report(
			String symbol, String symbolName, double quantity,
			String dateSell, String priceSell, String commissionSell, String fxRateSell, String commissionSellJPY, String amountSellJPY, String acquisitionCostJPY, String dateBuyFirst, String dateBuyLast,
			String dateBuy, String priceBuy, String commissionBuy, String fxRateBuy, String amountBuyJPY, String totalQuantity, String totalAmountBuyJPY
			) {
		this.symbol = symbol;
		this.symbolName = symbolName;
		this.quantity = quantity;
		
		this.dateSell = dateSell;
		this.priceSell = priceSell;
		this.commissionSell = commissionSell;
		this.fxRateSell = fxRateSell;
		this.commissionSellJPY = commissionSellJPY;
		this.amountSellJPY = amountSellJPY;
		this.acquisitionCostJPY = acquisitionCostJPY;
		this.dateBuyFirst = dateBuyFirst;
		this.dateBuyLast = dateBuyLast;

		this.dateBuy = dateBuy;
		this.priceBuy = priceBuy;
		this.commissionBuy = commissionBuy;
		this.fxRateBuy = fxRateBuy;
		this.amountBuyJPY = amountBuyJPY;
		this.totalQuantity = totalQuantity;
		this.totalAmountBuyJPY = totalAmountBuyJPY;
	}
	
	public static Report getInstance(
			String symbol, String symbolName, double quantity,
			String dateSell, String priceSell, String commissionSell, String fxRateSell, String commissionSellJPY, String amountSellJPY, String acquisitionCostJPY, String dateBuyFirst, String dateBuyLast,
			String dateBuy, String priceBuy, String commissionBuy, String fxRateBuy, String amountBuyJPY, String totalQuantity, String totalAmountBuyJPY
			) {
		return new Report(
				symbol, symbolName, quantity,
				dateSell, priceSell, commissionSell, fxRateSell, commissionSellJPY, amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast,
				dateBuy, priceBuy, commissionBuy, fxRateBuy, amountBuyJPY, totalQuantity, totalAmountBuyJPY
				);
	}
	
	public static Report getInstance(String symbol, String symbolName, double quantity,
			String dateSell, double priceSell, double commissionSell, double fxRateSell, int commissionSellJPY,
			int amountSellJPY, int acquisitionCostJPY, String dateBuyFirst, String dateBuyLast) {

		String dateBuy = "";
		String priceBuy = "";
		String commissionBuy = "";
		String fxRateBuy = "";
		String amountBuyJPY = "";
		String totalQuantity = "";
		String totalAmountBuyJPY = "";

		return new Report(
			symbol, symbolName, quantity,
			dateSell, String.format("%.5f", priceSell), String.format("%.2f", commissionSell), String.format("%.2f", fxRateSell), String.format("%d", commissionSellJPY),
			String.format("%d", amountSellJPY), String.format("%d", acquisitionCostJPY), dateBuyFirst, dateBuyLast,
			dateBuy, priceBuy, commissionBuy, fxRateBuy, amountBuyJPY, totalQuantity, totalAmountBuyJPY
			);
	}

	public static Report getInstance(String symbol, String symbolName, double quantity,
			String dateBuy, double priceBuy, double commissionBuy, double fxRateBuy,
			int amountBuyJPY, double totalQuantity, int totalAmountBuyJPY) {
		String dateSell = "";
		String priceSell = "";
		String commissionSell = "";
		String fxRateSell = "";
		String commissionSellJPY = "";
		String amountSellJPY = "";
		String acquisitionCostJPY = "";
		String dateBuyFirst = "";
		String dateBuyLast = "";
		
		return new Report(
			symbol, symbolName, quantity,
			dateSell, priceSell, commissionSell, fxRateSell, commissionSellJPY, amountSellJPY, acquisitionCostJPY, dateBuyFirst, dateBuyLast,
			dateBuy, String.format("%.5f",  priceBuy), String.format("%.2f", commissionBuy), String.format("%.2f", fxRateBuy),
			String.format("%d", amountBuyJPY), String.format("%.5f", totalQuantity), String.format("%d", totalAmountBuyJPY)
			);
	}
}
