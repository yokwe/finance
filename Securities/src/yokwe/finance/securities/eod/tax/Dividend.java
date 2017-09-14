package yokwe.finance.securities.eod.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("配当明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Dividend extends Sheet {
	@ColumnName("配当年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public String date;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	@ColumnName("銘柄コード")
	public String symbol;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	@ColumnName("銘柄")
	public String symbolName;
	
	@ColumnName("数量")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public double quantity;

	@ColumnName("配当金額")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double dividend;
	
	@ColumnName("外国源泉額")
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	public double taxWithholding;
	
	@NumberFormat(SpreadSheet.FORMAT_NUMBER2)
	@ColumnName("為替レート")
	public double fxRate;
	
	@ColumnName("邦貨配当金額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public int dividendJPY;
	
	@ColumnName("邦貨外国源泉額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public int taxWithholdingJPY;
	
	@ColumnName("邦貨収入金額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public int incomeJPY;

	
	private Dividend(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding, double fxRate, int dividendJPY, int taxWithholdingJPY) {
		this.date              = date;
		this.symbol            = symbol;
		this.symbolName        = symbol + " - " + symbolName;
		this.quantity          = quantity;
		
		this.dividend          = dividend;
		this.taxWithholding    = taxWithholding;
		this.fxRate            = fxRate;
		this.dividendJPY       = dividendJPY;
		this.taxWithholdingJPY = taxWithholdingJPY;
		
		this.incomeJPY         = dividendJPY - taxWithholdingJPY;
	}
	
	public void update(double dividend, double taxWithholding) {
		this.dividend       += dividend;
		this.taxWithholding += taxWithholding;
		dividendJPY          = (int)Math.floor(fxRate * this.dividend);
		taxWithholdingJPY    = (int)Math.floor(fxRate * this.taxWithholding);
		
		this.incomeJPY       = dividendJPY - taxWithholdingJPY;
	}

	public static Dividend getInstance(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding, double fxRate) {
		return new Dividend(date, symbol, symbolName, quantity, dividend, taxWithholding, fxRate, (int)Math.floor(fxRate * dividend), (int)Math.floor(fxRate * taxWithholding));
	}
}
