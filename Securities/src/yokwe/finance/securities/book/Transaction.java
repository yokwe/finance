package yokwe.finance.securities.book;

@SheetData.SheetName("売買履歴")
public class Transaction extends SheetData {	
	@ColumnName("取引明細書")
	String statement;
	@ColumnName("頁")
	String page;
	@ColumnName("受渡日")
	String settlementDate;
	@ColumnName("約定日")
	String tradeDate;
	@ColumnName("銘柄コード")
	String symbol;
	@ColumnName("銘柄")
	String name;
	@ColumnName("株数")
	int    quantity;
	@ColumnName("買値")
	double priceBuy;
	@ColumnName("売値")
	double priceSell;
	@ColumnName("手数料")
	double commission;
	@ColumnName("取得費")
	double acquisitionCost;
	@ColumnName("譲渡金額")
	double transferAmount;
	@ColumnName("譲渡手数料")
	double transferCommission;
	@ColumnName("為替レート")
	double usdjpy;
	@ColumnName("邦貨取得費")
	double acquisitionCostJPY;
	@ColumnName("邦貨譲渡金額")
	double transferAmountJPY;
	@ColumnName("邦貨譲渡手数料")
	double transferCommissionJPY;
	
	public String toString() {
		return String.format("%s %-8s %8.4f %8.4f %4d %8.2f %8.2f %8.2f %8.2f %8.2f", tradeDate, symbol, priceBuy, priceSell, quantity, commission, acquisitionCost, transferAmount, transferCommission, usdjpy);
	}
}
