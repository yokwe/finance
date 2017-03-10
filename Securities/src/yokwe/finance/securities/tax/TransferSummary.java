package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("譲渡概要")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferSummary extends Sheet {
	@ColumnName("売約定日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateSell;

	@ColumnName("銘柄コード")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String symbol;
	@ColumnName("銘柄")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public final String name;
	@ColumnName("数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double quantity;

	@ColumnName("譲渡金額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double sellJPY;
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double costJPY;
	@ColumnName("譲渡手数料")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double feeJPY;
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	@ColumnName("利益")
	public final double profitJPY;
	@ColumnName("取得日最初")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
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
