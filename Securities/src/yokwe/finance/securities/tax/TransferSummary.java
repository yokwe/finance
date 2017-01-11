package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.Sheet.ColumnName;

@Sheet.SheetName("譲渡サマリー")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class TransferSummary {
	@ColumnName("売約定日")
	public String dateSell;

	@ColumnName("銘柄コード")
	public String symbol;
	@ColumnName("銘柄")
	public String symbolName;
	@ColumnName("数量")
	public double quantity;

	@ColumnName("譲渡金額")
	public double sellJPY;
	@ColumnName("取得費")
	public double buyJPY;
	@ColumnName("譲渡手数料")
	public double feeJPY;
	@ColumnName("利益")
	public double profitJPY;
	@ColumnName("取得日最初")
	public String dateBuyFirst;
	@ColumnName("取得日最後")
	public String dateBuyLast;

	@ColumnName("Buy")
	public double buy;
	@ColumnName("Sell")
	public double sell;
	@ColumnName("Profit")
	public double profit;
	@ColumnName("Dividend")
	public double dividend;
	@ColumnName("Total Profit")
	public double totalProfit;
}
