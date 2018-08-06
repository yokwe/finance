package yokwe.finance.securities.eod.stockHistory;

import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("Dividend")
@Sheet.HeaderRow(2)
@Sheet.DataRow(2)
public class DividendSheet extends Sheet {
	@ColumnName("symbol")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String symbol;
	
	@ColumnName("quantity")
	@NumberFormat(SpreadSheet.FORMAT_INTEGER)
	public Double quantity;
	
	@ColumnName("cost")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double cost;
	
	@ColumnName("div")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div;
	
	@ColumnName("interest")
	@NumberFormat(SpreadSheet.FORMAT_PERCENT)
	public Double interest;
	
	
	@ColumnName("pay1")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay1;
	@ColumnName("div1")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div1;
	
	@ColumnName("pay2")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay2;
	@ColumnName("div2")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div2;
	
	@ColumnName("pay3")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay3;
	@ColumnName("div3")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div3;
	
	@ColumnName("pay4")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay4;
	@ColumnName("div4")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div4;
	
	@ColumnName("pay5")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay5;
	@ColumnName("div5")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div5;
	
	@ColumnName("pay6")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay6;
	@ColumnName("div6")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div6;
	
	@ColumnName("pay7")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay7;
	@ColumnName("div7")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div7;
	
	@ColumnName("pay8")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay8;
	@ColumnName("div8")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div8;
	
	@ColumnName("pay9")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay9;
	@ColumnName("div9")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div9;
	
	@ColumnName("pay10")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay10;
	@ColumnName("div10")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div10;
	
	@ColumnName("pay11")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay11;
	@ColumnName("div11")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div11;
	
	@ColumnName("pay12")
	@NumberFormat(SpreadSheet.FORMAT_STRING)
	public String pay12;
	@ColumnName("div12")
	@NumberFormat(SpreadSheet.FORMAT_USD)
	public Double div12;
	
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
