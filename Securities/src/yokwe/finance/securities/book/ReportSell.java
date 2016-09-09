package yokwe.finance.securities.book;

@SheetData.SheetName("譲渡所得計算明細書")
public class ReportSell extends SheetData {
	@ColumnName("譲渡日")
	public final String dateSell;
	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String symbolName;
	@ColumnName("数量")
	public final double quantity;
	@ColumnName("譲渡価額")
	public final int    priceSell;
	@ColumnName("取得費")
	public final int    priceBuy;
	@ColumnName("売却手数料")
	public final int    commissionSell;
	@ColumnName("取得日最初")
	public final String dateBuyFirst;
	@ColumnName("取得日最後")
	public final String dateBuyLast;
	
	public ReportSell(String dateSell, String symbol, String symbolName, double quantity, int priceSell, int priceBuy, int commissionSell, String dateBuyFirst, String dateBuyLast) {
		this.dateSell       = dateSell;
		this.symbol         = symbol;
		this.symbolName     = symbolName;
		this.quantity       = quantity;
		this.priceSell      = priceSell;
		this.priceBuy       = priceBuy;
		this.commissionSell = commissionSell;
		this.dateBuyFirst   = dateBuyFirst;
		this.dateBuyLast    = dateBuyLast;
	}
	
	@Override
	public String toString() {
		return String.format("%s  %-8s  %5.0f  %8d  %8d  %3d  %s  %s", dateSell, symbol, quantity, priceSell, priceBuy, commissionSell, dateBuyFirst, dateBuyLast);
	}
}