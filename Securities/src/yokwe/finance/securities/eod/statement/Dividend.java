package yokwe.finance.securities.eod.statement;

import yokwe.finance.securities.eod.tax.Transaction;
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
	
	@ColumnName("配当金額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double dividend;
	
	@ColumnName("源泉額")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double taxWithholding;
	
	@ColumnName("損益")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public double profitLoss;
		
	private Dividend(
			String date, String symbol, String symbolName,
			double dividend, double taxWithholding) {
		this.date           = date;
		this.symbol         = symbol;
		this.symbolName     = symbol + " - " + symbolName;
		
		this.dividend       = Transaction.roundPrice(dividend);
		this.taxWithholding = Transaction.roundPrice(taxWithholding);
		
		this.profitLoss     = Transaction.roundPrice(dividend - taxWithholding);
	}
	
	public void update(double dividend, double taxWithholding) {
		this.dividend       = Transaction.roundPrice(this.dividend       + dividend);
		this.taxWithholding = Transaction.roundPrice(this.taxWithholding + taxWithholding);
		
		this.profitLoss     = Transaction.roundPrice(this.dividend - this.taxWithholding);
	}

	public static Dividend getInstance(
			String date, String symbol, String symbolName, double quantity,
			double dividend, double taxWithholding) {
		return new Dividend(date, symbol, symbolName, dividend, taxWithholding);
	}
}
