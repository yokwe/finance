package yokwe.finance.securities.tax;

@Sheet.SheetName("mizuho-header")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Mizuho extends Sheet implements Comparable<Mizuho> {
	@ColumnName("DATE")
	public String date;
	@ColumnName("USD")
	public double usd;
	@ColumnName("GBP")
	public double gbp;
	@ColumnName("EUR")
	public double eur;
	@ColumnName("AUD")
	public double aud;
	@ColumnName("NZD")
	public double nzd;
	
	@Override
	public String toString() {
		return String.format("%s %6.2f %6.2f %6.2f %6.2f %6.2f", date, usd, gbp, eur, aud, nzd);
	}
	
	@Override
	public int compareTo(Mizuho that) {
		return this.date.compareTo(that.date);
	}
}
