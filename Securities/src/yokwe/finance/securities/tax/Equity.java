package yokwe.finance.securities.tax;

import java.util.List;

@Sheet.SheetName("equityStats-header")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Equity extends Sheet {
	public static List<Equity> load(String url) {
		try (LibreOffice libreOffice = new LibreOffice(url, true)) {
			return Sheet.getInstance(libreOffice, Equity.class);
		}
	}
	
	@ColumnName("symbol")
	public String symbol;
	@ColumnName("name")
	public String name;
	@ColumnName("price")
	public double price;
	@ColumnName("sd")
	public double sd;
	@ColumnName("div")
	public double div;
	@ColumnName("freq")
	public double freq;
	@ColumnName("changepct")
	public double changepct;
	@ColumnName("count")
	public int ount;
	@ColumnName("hv")
	public double hv;
	@ColumnName("change")
	public double change;
	@ColumnName("var95")
	public double var95;
	@ColumnName("var99")
	public double var99;
	@ColumnName("rsi")
	public double rsi;
	@ColumnName("price200")
	public double price200;
	@ColumnName("divYield")
	public double divYield;
	@ColumnName("beta")
	public double beta;
	@ColumnName("r2")
	public double r2;
	@ColumnName("vol5")
	public int vol5;
	@ColumnName("min")
	public double min;
	@ColumnName("max")
	public double max;
	@ColumnName("minpct")
	public double minpct;
	@ColumnName("maxpct")
	public double maxpct;
}
