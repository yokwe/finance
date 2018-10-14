package yokwe.finance.stock.data;

public class Stock implements Comparable<Stock> {
	public String symbol;
	public String name;

	public double marketcap;          // is not calculated in real time.
	public double beta;

	public double dividendYield;
	public String exDividendDate;

	public double priceToBook;

	public double institutionPercent; // represents top 15 institutions
	public double insiderPercent;
	public double shortRatio;
	
	public double year5ChangePercent;
	public double year2ChangePercent;
	public double year1ChangePercent;
	
	public double month6ChangePercent;
	public double month3ChangePercent;
	public double month1ChangePercent;
	
	public Stock(
		String symbol, String name,
		double marketcap, double beta,
		double dividendYield, String exDividendDate,
		double priceToBook,
		double institutionPercent, double insiderPercent, double shortRatio,
		double year5ChangePercent, double year2ChangePercent, double year1ChangePercent,
		double month6ChangePercent, double month3ChangePercent, double month1ChangePercent) {
		this.symbol = symbol;
		this.name   = name;

		this.marketcap = marketcap;
		this.beta      = beta;

		this.dividendYield  = dividendYield;
		this.exDividendDate = exDividendDate;

		this.priceToBook = priceToBook;

		this.institutionPercent = institutionPercent;
		this.insiderPercent     = insiderPercent;
		this.shortRatio         = shortRatio;
		
		this.year5ChangePercent = year5ChangePercent;
		this.year2ChangePercent = year2ChangePercent;
		this.year1ChangePercent = year1ChangePercent;
		
		this.month6ChangePercent = month6ChangePercent;
		this.month3ChangePercent = month3ChangePercent;
		this.month1ChangePercent = month1ChangePercent;
	}
	
	public Stock() {
		this(
			"", "",
			0, 0,
			0, "",
			0,
			0, 0, 0,
			0, 0, 0,
			0, 0, 0);
	}

	@Override
	public int compareTo(Stock that) {
		return this.symbol.compareTo(that.symbol);
	}
		
	@Override
	public String toString() {
		return String.format("%s %s", symbol, name);
	}
}
