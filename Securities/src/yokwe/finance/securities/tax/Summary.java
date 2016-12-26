package yokwe.finance.securities.tax;

@Sheet.SheetName("譲渡計算明細書")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Summary extends Sheet {
	@ColumnName("売約定日")
	public final String dateSell;

	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String symbolName;
	@ColumnName("数量")
	public final double quantity;

	@ColumnName("譲渡金額")
	public final String amountSellJPY;
	@ColumnName("取得費")
	public final String acquisitionCostJPY;
	@ColumnName("譲渡手数料")
	public final String commissionSellJPY;
	@ColumnName("取得日最初")
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	public final String dateBuyLast;
	
	private Summary(Transfer transfer) {
		this.dateSell           = transfer.dateSell;
		
		this.symbol             = transfer.symbol;
		this.symbolName         = transfer.symbolName;
		this.quantity           = transfer.quantity;
		
		this.amountSellJPY      = transfer.amountSellJPY;
		this.acquisitionCostJPY = transfer.acquisitionCostJPY;
		this.commissionSellJPY  = transfer.commissionSellJPY;
		this.dateBuyFirst       = transfer.dateBuyFirst;
		this.dateBuyLast        = transfer.dateBuyLast;
	}
	
	public static Summary getInstance(Transfer transfer) {
		return new Summary(transfer);
	}
}
