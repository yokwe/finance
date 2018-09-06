package yokwe.finance.stock.firstrade.tax;

import yokwe.finance.stock.libreoffice.Sheet;
import yokwe.finance.stock.libreoffice.SpreadSheet;

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
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final double sellJPY;
	
	@ColumnName("取得費")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final double costJPY;
	
	@ColumnName("譲渡手数料")
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	public final double feeJPY;
	
	@NumberFormat(SpreadSheet.FORMAT_JPY)
	@ColumnName("利益")
	public final double profitJPY;
	
	@ColumnName("取得日最初")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuyFirst;
	
	@ColumnName("取得日最後")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String dateBuyLast;

	@ColumnName("Buy")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double buy;
	
	@ColumnName("Sell")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double sell;
	
	@ColumnName("Profit")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double profit;
	
	@ColumnName("Dividend")
	@NumberFormat(SpreadSheet.FORMAT_USD_BLANK)
	public final double dividend;
	
	@ColumnName("Total Profit")
	@NumberFormat(SpreadSheet.FORMAT_USD)
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
		this.dividend     = 0  /* + sell.dividend */;
		this.totalProfit  = sell.sell - sell.fee - sell.cost /* + sell.dividend */;
	}
}
