package yokwe.finance.securities.book;

@SheetData.SheetName("配当・分配金明細書")
@SheetData.HeaderRow(0)
@SheetData.DataRow(1)
public class ReportDividend extends SheetData{
	@ColumnName("配当年月日")
	public final String date;
	@ColumnName("銘柄コード")
	public final String symbol;
	@ColumnName("銘柄")
	public final String symbolName;
	@ColumnName("数量")
	public final double quantity;

	@ColumnName("配当金額")
	public final double dividend;
	@ColumnName("外国源泉額")
	public final double taxWithholding;
	@ColumnName("為替レート")
	public final double fxRate;
	@ColumnName("邦貨配当金額")
	public final double dividendJPY;
	@ColumnName("邦貨外国源泉額")
	public final double taxWithholdingJPY;
	
	private ReportDividend(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding, double fxRate, double dividendJPY, double taxWithholdingJPY) {
		this.date              = date;
		this.symbol            = symbol;
		this.symbolName        = symbolName;
		this.quantity          = quantity;
		
		this.dividend          = dividend;
		this.taxWithholding    = taxWithholding;
		this.fxRate            = fxRate;
		this.dividendJPY       = dividendJPY;
		this.taxWithholdingJPY = taxWithholdingJPY;
	}

	public static ReportDividend getInstance(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding, double fxRate, double dividendJPY, double taxWithholdingJPY) {
		return new ReportDividend(date, symbol, symbolName, quantity, dividend, taxWithholding, fxRate, dividendJPY, taxWithholdingJPY);
	}
}
