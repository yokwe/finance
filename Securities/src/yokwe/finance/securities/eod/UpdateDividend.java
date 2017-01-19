package yokwe.finance.securities.eod;

import org.slf4j.LoggerFactory;

public class UpdateDividend {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDividend.class);
	
	// for price data
	//   Yahoo
	//     http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&ignore=.csv
	//     Date,Open,High,Low,Close,Volume,Adj Close
	//   Google
	//     http://www.google.com/finance/historical?q=NYSE:IBM&startdate=Jan%201,%202000&enddate=Dec%2031,%202050&output=csv
	//     \uFEFFDate,Open,High,Low,Close,Volume
	
	// for dividend data
	//   Yahoo
	//     http://real-chart.finance.yahoo.com/table.csv?s=SPY&a=00&b=01&c=2015&d=12&e=30&f=2015&g=v&ignore=.csv
	//     Date,Dividends
	

}
