package yokwe.finance.securities.eod.statement;

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
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public final double interest;
	
	
	public Interest(String date, double interest) {
		this.date        = date;
		this.interest    = interest;
	}
}
