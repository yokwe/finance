package yokwe.finance.securities.tax;

@Sheet.SheetName("symbol-name")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class SymbolName extends Sheet {
	@ColumnName("Symbol")
	public String symbol;
	@ColumnName("Name")
	public String name;

	@Override
	public String toString() {
		return String.format("%-8s %s", symbol, name);
	}
}
