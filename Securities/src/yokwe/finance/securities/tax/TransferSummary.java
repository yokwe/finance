package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("譲渡概要")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferSummary extends Sheet {
	@ColumnName("売約定日")
	public final String dateSell;

	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String name;
	@ColumnName("数量")
	public final double quantity;

	@ColumnName("譲渡金額")
	public final double sellJPY;
	@ColumnName("取得費")
	public final double costJPY;
	@ColumnName("譲渡手数料")
	public final double feeJPY;
	@ColumnName("利益")
	public final double profitJPY;
	@ColumnName("取得日最初")
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	public final String dateBuyLast;

	@ColumnName("Buy")
	public final double buy;
	@ColumnName("Sell")
	public final double sell;
	@ColumnName("Profit")
	public final double profit;
	@ColumnName("Dividend")
	public final double dividend;
	@ColumnName("Total Profit")
	public final double totalProfit;
	
	public TransferSummary(Transfer.Sell sell) {
		this.dateSell     = sell.date;
		
		this.symbol       = sell.symbol;
		this.name         = sell.name;
		this.quantity     = sell.quantity;
		
		this.sellJPY      = sell.sellJPY;
		this.costJPY      = sell.costJPY;
		this.feeJPY       = sell.feeJPY;
		this.profitJPY    = sell.sellJPY - sell.costJPY - sell.feeJPY;
		this.dateBuyFirst = sell.dateFirst;
		this.dateBuyLast  = sell.dateLast;
		
		this.buy          = sell.cost;
		this.sell         = sell.sell - sell.fee;
		this.profit       = sell.sell - sell.fee - sell.cost;
		this.dividend     = sell.dividend;
		this.totalProfit  = sell.sell - sell.fee - sell.cost + sell.dividend;
	}
}
