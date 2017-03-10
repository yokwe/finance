package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("利子明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Interest extends Sheet {
	@ColumnName("支払年月日")
	@NumberFormat(SpreadSheet.FORMAT_DATE)
	public final String date;
	@ColumnName("利子金額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final double interest;
	@NumberFormat(SpreadSheet.FORMAT_PRICE2)
	@ColumnName("為替レート")
	public final double fxRate;
	@ColumnName("邦貨利子金額")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public final int interestJPY;
	
	public Interest(String date, double interest, double fxRate) {
		this.date        = date;
		this.interest    = interest;
		this.fxRate      = fxRate;
		this.interestJPY = (int)Math.floor(this.interest * this.fxRate);
	}
}
