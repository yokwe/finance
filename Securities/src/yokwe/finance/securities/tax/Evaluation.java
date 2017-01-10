package yokwe.finance.securities.tax;

import yokwe.finance.securities.libreoffice.Sheet;

@Sheet.SheetName("Evaluation")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
// TODO Is really need to extends from Sheet?
public class Evaluation extends Sheet {
	@ColumnName("symbol")
	public String symbol;
	@ColumnName("name")
	public String name;
	@ColumnName("quantity")
	public double quantity;
	@ColumnName("buy")
	public double buy;
	@ColumnName("sell")
	public double sell;
	@ColumnName("profit")
	public double profit;
	@ColumnName("dividend")
	public double dividend;
	@ColumnName("total profit")
	public double totalProfit;
	
	public Evaluation() {
		// Default constructor
	}
	
	public Evaluation(String symbol, String name, double quantity, double buy, double sell, double dividend) {
		this.symbol      = symbol;
		this.name        = name;
		this.quantity    = quantity;
		this.buy         = buy;
		this.sell        = sell;
		this.profit      = sell - buy;
		this.dividend    = dividend;
		this.totalProfit = profit + dividend;
	}
}
