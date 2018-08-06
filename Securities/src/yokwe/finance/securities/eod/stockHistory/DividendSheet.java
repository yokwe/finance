package yokwe.finance.securities.eod.stockHistory;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("Dividend")
@Sheet.HeaderRow(3)
@Sheet.DataRow(4)
public class DividendSheet extends Sheet {
	@ColumnName("symbol")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String symbol;
	
	@ColumnName("quantity")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	Double quantity;
	
	@ColumnName("cost")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double cost;
	
	@ColumnName("interest")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	Double interest;
	
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay1;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div1;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay2;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div2;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay3;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div3;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay4;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div4;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay5;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div5;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay6;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div6;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay7;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div7;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay8;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div8;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay9;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div9;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay10;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div10;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay11;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div11;
	
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	String pay12;
	@NumberFormat(SpreadSheet.FORMAT_USD)
	Double div12;
	
	DividendSheet(String symbol, Double quantity, Double cost, Double interest,
		String pay1, Double div1, String pay2, Double div2, String pay3, Double div3, 
		String pay4, Double div4, String pay5, Double div5, String pay6, Double div6, 
		String pay7, Double div7, String pay8, Double div8, String pay9, Double div9, 
		String pay10, Double div10, String pay11, Double div11, String pay12, Double div12
		) {
		this.symbol   = symbol;
		this.quantity = quantity;
		this.cost     = cost;
		this.interest = interest;
		
		this.div1     = div1;
		this.pay1     = pay1;
		this.div2     = div2;
		this.pay2     = pay2;
		this.div3     = div3;
		this.pay3     = pay3;
		this.div4     = div4;
		this.pay4     = pay4;
		this.div5     = div5;
		this.pay5     = pay5;
		this.div6     = div6;
		this.pay6     = pay6;
		this.div7     = div7;
		this.pay7     = pay7;
		this.div8     = div8;
		this.pay8     = pay8;
		this.div9     = div9;
		this.pay9     = pay9;
		this.div10    = div10;
		this.pay10    = pay10;
		this.div11    = div11;
		this.pay11    = pay11;
		this.div12    = div12;
		this.pay12    = pay12;
	}
	DividendSheet() {
		this(null, null, null, null,
			null, null, null, null, null, null,
			null, null, null, null, null, null,
			null, null, null, null, null, null,
			null, null, null, null, null, null);
	}
}
