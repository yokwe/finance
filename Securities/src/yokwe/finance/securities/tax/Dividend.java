package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("配当明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Dividend extends Sheet {
	@ColumnName("配当年月日")
	public String date;
	@ColumnName("銘柄コード")
	public String symbol;
	@ColumnName("銘柄")
	public String symbolName;
	@ColumnName("数量")
	public String quantity; // can be blank for INTEREST

	@ColumnName("配当金額")
	public double dividend;
	@ColumnName("外国源泉額")
	public double taxWithholding;
	@ColumnName("為替レート")
	public double fxRate;
	@ColumnName("邦貨配当金額")
	public int dividendJPY;
	@ColumnName("邦貨外国源泉額")
	public int taxWithholdingJPY;
	
	private Dividend(
			String date, String symbol, String symbolName, String quantity,
			double dividend, double taxWithholding, double fxRate, int dividendJPY, int taxWithholdingJPY) {
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
	
	public void update(double dividend, double taxWithholding) {
		this.dividend       += dividend;
		this.taxWithholding += taxWithholding;
		dividendJPY          = (int)Math.round(fxRate * this.dividend);
		taxWithholdingJPY    = (int)Math.round(fxRate * this.taxWithholding);
	}

	public static Dividend getInstance(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding, double fxRate) {
		return new Dividend(date, symbol, symbolName, String.format("%.5f", quantity), dividend, taxWithholding, fxRate, (int)Math.round(fxRate * dividend), (int)Math.round(fxRate * taxWithholding));
	}
	public static Dividend getInstance(
			String date,
			double dividend, double taxWithholding, double fxRate) {
		return new Dividend(date, "", "口座利子", "", dividend, taxWithholding, fxRate, (int)Math.round(fxRate * dividend), (int)Math.round(fxRate * taxWithholding));
	}
}
