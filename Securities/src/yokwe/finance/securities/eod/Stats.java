package yokwe.finance.securities.eod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;
import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.HV;
import yokwe.finance.securities.stats.MA;
import yokwe.finance.securities.stats.RSI;
import yokwe.finance.securities.util.CSVUtil;
import yokwe.finance.securities.util.DoubleStreamUtil;
import yokwe.finance.securities.util.DoubleUtil;
import yokwe.finance.securities.util.NasdaqUtil;

public class Stats {
//	public String exchange;
	public String symbol;
	public String name;
	
	// last values
	public String date;
	public double price;
	
	// price
	public int    pricec;
	
	public double sd;
	public double hv;
	public double rsi;

	public double min;
	public double max;
	public double minpct;
	public double maxpct;

	// dividend
	public double div;
	public int    divc;
	public double yield;
	
	// volume
	public long   vol;
	public long   vol5;
	public long   vol30;
	
	// price change detection
	public double sma5;
	public double sma20;
	public double sma50;
	public double sma200;
	
	public double sma5pct;
	public double sma20pct;
	public double sma50pct;
	public double sma200pct;

	
	public Stats(NasdaqTable nasdaq, List<Price> priceList, List<Dividend> dividendList) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(Stats.class);

		// Order of data is important
		priceList.sort((a, b) -> a.date.compareTo(b.date));
		dividendList.sort((a, b) -> a.date.compareTo(b.date));

//		this.exchange = nasdaq.exchange;
		this.symbol   = nasdaq.symbol;
		this.name     = nasdaq.name;
		
		{
			Price lastPrice = priceList.get(priceList.size() - 1);
			this.date  = lastPrice.date;
			this.price = DoubleUtil.round(lastPrice.close, 2);
			this.vol   = lastPrice.volume;
		}
		
		{
			double[] priceArray = priceList.stream().mapToDouble(o -> o.close).toArray();
			this.pricec = priceArray.length;
			
			{
				double logReturn[] = DoubleArray.logReturn(priceArray);
				DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats(logReturn);
				
				double sd = stats.getStandardDeviation();
				if (Double.isInfinite(sd)) {
					logger.error("sd is infinite  {}", symbol);
					throw new SecuritiesException("sd is infinite");
				}
				if (Double.isNaN(sd)) {
					logger.error("sd is Nan  {}", symbol);
					throw new SecuritiesException("sd is NaN");
				}
				
				this.sd = DoubleUtil.round(stats.getStandardDeviation(), 4);
			}
			
			HV hv = new HV(priceArray);
			this.hv = DoubleUtil.round(hv.getValue(), 4);
			
			if (RSI.DEFAULT_PERIDO < priceArray.length) {
				RSI rsi = new RSI();
				Arrays.stream(priceArray).forEach(rsi);
				this.rsi = DoubleUtil.round(rsi.getValue(), 1);
			} else {
				this.rsi = -1;
			}
			
			{
				double min = priceList.get(0).low;
				double max = priceList.get(0).high;
				for(Price price: priceList) {
					if (price.low < min)  min = price.low;
					if (max < price.high) max = price.high;
				}
				this.min    = DoubleUtil.round(min, 2);
				this.max    = DoubleUtil.round(max, 2);
				this.minpct = DoubleUtil.round((this.price - this.min) / this.price, 3);
				this.maxpct = DoubleUtil.round((this.max - this.price) / this.price, 3);
			}
			
			
			// price change detection
			this.sma5   = DoubleUtil.round(MA.sma(  5, priceArray).getValue(), 2);
			this.sma20  = DoubleUtil.round(MA.sma( 20, priceArray).getValue(), 2);
			this.sma50  = DoubleUtil.round(MA.sma( 50, priceArray).getValue(), 2);
			this.sma200 = DoubleUtil.round(MA.sma(200, priceArray).getValue(), 2);
			
			this.sma5pct   = DoubleUtil.round((this.price - this.sma5)   / this.price, 3);
			this.sma20pct  = DoubleUtil.round((this.price - this.sma20)  / this.price, 3);
			this.sma50pct  = DoubleUtil.round((this.price - this.sma50)  / this.price, 3);
			this.sma200pct = DoubleUtil.round((this.price - this.sma200) / this.price, 3);
		}
		
		// dividend
		{
			double[] divArray = dividendList.stream().mapToDouble(o -> o.dividend).toArray();
			
			this.div   = DoubleUtil.round(Arrays.stream(divArray).sum(), 4);
			this.divc  = divArray.length;
			this.yield = DoubleUtil.round(this.div / this.price, 3);
		}
		
		// volume
		{
			double[] volArray = priceList.stream().mapToDouble(o -> o.volume).toArray();

			MA vol5 = MA.sma(5, volArray);
			this.vol5 = (long)vol5.getValue();

			MA vol30 = MA.sma(30, volArray);
			this.vol30 = (long)vol30.getValue();
		}
		
	}
	
	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(Stats.class);
		logger.info("START");
		
		final String lastTradingDate = Market.getLastTradingDate().toString();

		final String PATH_STATS          = "tmp/eod/stats.csv";
		final String PATH_PRICE_GOOGLE   = "tmp/eod/price-google";
		final String PATH_PRICE_YAHOO    = "tmp/eod/price-yahoo";
		final String PATH_DIVIDEND_YAHOO = "tmp/eod/dividend-yahoo";
		
		File dirPriceGoogle   = new File(PATH_PRICE_GOOGLE);
		File dirPriceYahoo    = new File(PATH_PRICE_YAHOO);
		File dirDividendYahoo = new File(PATH_DIVIDEND_YAHOO);
		
		List<Stats> statsList = new ArrayList<>();
		
		Collection<NasdaqTable> nasdaqCollection = NasdaqUtil.getAll();
		
//		Collection<NasdaqTable> nasdaqCollection = new ArrayList<>();
//		nasdaqCollection.add(NasdaqUtil.get("IBM"));
//		nasdaqCollection.add(NasdaqUtil.get("NYT"));
//		nasdaqCollection.add(NasdaqUtil.get("PEP"));
		
		int total = nasdaqCollection.size();
		int count = 0;
		
		int showInterval = 10000;
		boolean showOutput;
		int lastOutputCount = -1;
		for(NasdaqTable nasdaq: nasdaqCollection) {
			String symbol = nasdaq.symbol;

			int outputCount = count / showInterval;
			if (outputCount != lastOutputCount) {
				showOutput = true;
				lastOutputCount = outputCount;
			} else {
				showOutput = false;
			}

			count++;
			
			final List<Price> priceList;
			final File dividendFile;
			{
				String fileName = String.format("%s.csv", nasdaq.symbol);
				
				File priceGoogle   = new File(dirPriceGoogle, fileName);
				File priceYahoo    = new File(dirPriceYahoo, fileName);
				File dividendYahoo = new File(dirDividendYahoo, fileName);
								
				if (priceGoogle.exists() && priceYahoo.exists()) {
					// both
					List<Price> priceListGoogle = CSVUtil.loadWithHeader(priceGoogle.getPath(), Price.class);
					List<Price> priceListYahoo  = CSVUtil.loadWithHeader(priceYahoo.getPath(), Price.class);
					
					String dateGoogle = priceListGoogle.get(0).date;
					String dateYahoo  = priceListYahoo.get(0).date;
					
					if (dateGoogle.equals(lastTradingDate) && dateYahoo.equals(lastTradingDate)) {
						// both
						int priceGoogleSize = priceListGoogle.size();
						int priceYahooSize  = priceListYahoo.size();
						
						if (priceGoogleSize <= priceYahooSize) { // prefer yahoo over google
							priceList = priceListYahoo;
						} else {
							priceList = priceListGoogle;
						}
					} else if (dateGoogle.equals(lastTradingDate)) {
						// google
						priceList = priceListGoogle;
					} else if (dateYahoo.equals(lastTradingDate)) {
						// yahoo
						priceList = priceListYahoo;
					} else {
						// none -- could be happen for discontinued stock
						int priceGoogleSize = priceListGoogle.size();
						int priceYahooSize  = priceListYahoo.size();
						
						if (priceGoogleSize < priceYahooSize) {
							priceList = priceListYahoo;
						} else {
							priceList = priceListGoogle;
						}
					}
				} else if (priceGoogle.exists()) {
					// only google
					priceList = CSVUtil.loadWithHeader(priceGoogle.getPath(), Price.class);
				} else if (priceYahoo.exists()) {
					// only yahoo
					priceList = CSVUtil.loadWithHeader(priceYahoo.getPath(), Price.class);
				} else {
					// none
//					logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s NO PRICE DATA", symbol));
					continue;
				}
				
				dividendFile = dividendYahoo;
			}
			
			// date is not last trading date
			{
				String date = priceList.get(0).date;
				if (!date.equals(lastTradingDate)) {
					logger.warn("{}  old    {}", String.format("%4d / %4d",  count, total), String.format("%-8s %s", symbol, date));
				}
			}
						
			// Ignore penny stock
//			{
//				boolean penyyStock = false;
//				for(Price price: priceList) {
//					if (price.close < 1.0) {
//						penyyStock = true;
//						break;
//					}
//				}
//				if (penyyStock) {
////					logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s PENNY STOCK", symbol));
//					continue;
//				}
//			}
			
			// Ignore too small sample stock to prevent error and prevent get abnormal statistics value
			if (priceList.size() <= 5) {
				logger.warn("{}  small  {}", String.format("%4d / %4d",  count, total), String.format("%-8s %2d", symbol, priceList.size()));
				continue;
			}
			
			if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, total), symbol);
			
			List<Dividend> dividendList;
			if (dividendFile.exists()) {
				dividendList = CSVUtil.loadWithHeader(dividendFile.getPath(), Dividend.class);
			} else {
				dividendList = new ArrayList<>();
			}
			
			statsList.add(new Stats(nasdaq, priceList, dividendList));
		}
		CSVUtil.saveWithHeader(statsList, PATH_STATS);
		logger.info("stats  {}", String.format("%4d", statsList.size()));
		logger.info("STOP");
	}
}
