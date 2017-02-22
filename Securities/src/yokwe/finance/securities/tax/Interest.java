package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("利子明細")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Interest extends Sheet {
	@ColumnName("支払年月日")
	public final String date;
	@ColumnName("利子金額")
	public final double interest;
	@ColumnName("為替レート")
	public final double fxRate;
	@ColumnName("邦貨利子金額")
	public final int interestJPY;
	
	public Interest(String date, double interest, double fxRate) {
		this.date        = date;
		this.interest    = interest;
		this.fxRate      = fxRate;
		this.interestJPY = (int)Math.floor(this.interest * this.fxRate);
	}
}