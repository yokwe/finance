package yokwe.finance.securities.eod;

import java.util.Arrays;
import java.util.List;

import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.stats.HV;
import yokwe.finance.securities.stats.MA;
import yokwe.finance.securities.stats.RSI;
import yokwe.finance.securities.util.DoubleStreamUtil;

public class Stats {
	public String exchange;
	public String symbol;
	public String name;
	
	// sampled values
	public String sample;
	public double price;
	public long   vol;
	
	// price
	public int    priceCount;
	
	public double sd;
	public double hv;
	public double rsi;

	public double min;
	public double max;
	public double minPercent;
	public double maxPercent;

	public double sma20;
	public double sma50;
	public double sma200;
	
	// dividend
	public double div;
	public int    divCount;
	public double divYield;
	
	// volume
	public long   vol5;
	public long   vol30;
	
	public Stats(NasdaqTable nasdaq, List<Price> priceList, List<Dividend> dividendList) {
		this.exchange = nasdaq.exchange;
		this.symbol   = nasdaq.symbol;
		this.name     = nasdaq.name;
		
		{
			Price lastPrice = priceList.get(priceList.size() - 1);
			this.sample   = lastPrice.date;
			this.price    = lastPrice.close;
			this.vol      = lastPrice.volume;
		}
		
		{
			double[] priceArray = priceList.stream().mapToDouble(o -> o.close).toArray();
			this.priceCount = priceArray.length;
			
			DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats();
			Arrays.stream(priceArray).forEach(stats);
			this.sd = stats.getStandardDeviation();
			
			HV hv = new HV();
			Arrays.stream(priceArray).forEach(hv);
			this.hv = hv.getValue();
			
			RSI rsi = new RSI();
			Arrays.stream(priceArray).forEach(rsi);
			this.rsi = rsi.getValue();
			
			this.min = stats.getMin();
			this.max = stats.getMax();
			this.minPercent = (this.price - this.min) / this.price;
			this.maxPercent = (this.max - this.price) / this.price;
			
			MA price20 = MA.sma(20);
			Arrays.stream(priceArray).forEach(price20);
			this.sma20 = price20.getValue();
			
			MA price50 = MA.sma(50);
			Arrays.stream(priceArray).forEach(price50);
			this.sma50 = price50.getValue();
			
			MA price200 = MA.sma(200);
			Arrays.stream(priceArray).forEach(price200);
			this.sma200 = price200.getValue();
		}
		
		// dividend
		//public double div;
		//public int    divCount;
		//public double divYield;
		
		// volume
		//public long   vol5;
		//public long   vol30;

		
	}
}
