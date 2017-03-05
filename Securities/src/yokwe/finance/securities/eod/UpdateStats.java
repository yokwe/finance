package yokwe.finance.securities.eod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.HV;
import yokwe.finance.securities.stats.MA;
import yokwe.finance.securities.stats.RSI;
import yokwe.finance.securities.util.DoubleStreamUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateStats {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStats.class);
	
	private static final String lastTradingDate  = Market.getLastTradingDate().toString();

	public static final String PATH_STATS        = "tmp/eod/stats.csv";

	private static Stats getInstance(Stock stock, List<Price> priceList, List<Dividend> dividendList) {
		// Order of data is important
		priceList.sort((a, b) -> a.date.compareTo(b.date));
		dividendList.sort((a, b) -> a.date.compareTo(b.date));

		Stats ret = new Stats();
		
//		this.exchange = stock.exchange;
		ret.symbol   = stock.symbol;
		ret.name     = stock.name;
		
		{
			Price lastPrice = priceList.get(priceList.size() - 1);
			ret.date  = lastPrice.date;
			ret.price = DoubleUtil.round(lastPrice.close, 2);
			ret.vol   = lastPrice.volume;
		}
		
		{
			double[] priceArray = priceList.stream().mapToDouble(o -> o.close).toArray();
			ret.pricec = priceArray.length;
			
			{
				double logReturn[] = DoubleArray.logReturn(priceArray);
				DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats(logReturn);
				
				double sd = stats.getStandardDeviation();
				if (Double.isInfinite(sd)) {
					logger.error("sd is infinite  {}", ret.symbol);
					throw new SecuritiesException("sd is infinite");
				}
				if (Double.isNaN(sd)) {
					logger.error("sd is Nan  {}", ret.symbol);
					throw new SecuritiesException("sd is NaN");
				}
				
				ret.sd = DoubleUtil.round(stats.getStandardDeviation(), 4);
			}
			
			HV hv = new HV(priceArray);
			ret.hv = DoubleUtil.round(hv.getValue(), 4);
			
			if (RSI.DEFAULT_PERIDO < priceArray.length) {
				RSI rsi = new RSI();
				Arrays.stream(priceArray).forEach(rsi);
				ret.rsi = DoubleUtil.round(rsi.getValue(), 1);
			} else {
				ret.rsi = -1;
			}
			
			{
				double min = priceList.get(0).low;
				double max = priceList.get(0).high;
				for(Price price: priceList) {
					if (price.low < min)  min = price.low;
					if (max < price.high) max = price.high;
				}
				ret.min    = DoubleUtil.round(min, 2);
				ret.max    = DoubleUtil.round(max, 2);
				ret.minpct = DoubleUtil.round((ret.price - ret.min) / ret.price, 3);
				ret.maxpct = DoubleUtil.round((ret.max - ret.price) / ret.price, 3);
			}
			
			
			// price change detection
			ret.last   = priceArray[priceArray.length - 2];
			ret.sma5   = DoubleUtil.round(MA.sma(  5, priceArray).getValue(), 2);
			ret.sma20  = DoubleUtil.round(MA.sma( 20, priceArray).getValue(), 2);
			ret.sma50  = DoubleUtil.round(MA.sma( 50, priceArray).getValue(), 2);
			ret.sma200 = DoubleUtil.round(MA.sma(200, priceArray).getValue(), 2);
			
			ret.lastpct   = DoubleUtil.round((ret.price - ret.last)   / ret.price, 3);
			ret.sma5pct   = DoubleUtil.round((ret.price - ret.sma5)   / ret.price, 3);
			ret.sma20pct  = DoubleUtil.round((ret.price - ret.sma20)  / ret.price, 3);
			ret.sma50pct  = DoubleUtil.round((ret.price - ret.sma50)  / ret.price, 3);
			ret.sma200pct = DoubleUtil.round((ret.price - ret.sma200) / ret.price, 3);
		}
		
		// dividend
		{
			double[] divArray = dividendList.stream().mapToDouble(o -> o.dividend).toArray();
			
			ret.div   = DoubleUtil.round(Arrays.stream(divArray).sum(), 4);
			ret.divc  = divArray.length;
			ret.yield = DoubleUtil.round(ret.div / ret.price, 3);
		}
		
		// volume
		{
			double[] volArray = priceList.stream().mapToDouble(o -> o.volume).toArray();

			MA vol5 = MA.sma(5, volArray);
			ret.vol5 = (long)vol5.getValue();

			MA vol30 = MA.sma(30, volArray);
			ret.vol30 = (long)vol30.getValue();
		}
		
		return ret;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		UpdateProvider priceGoogleProvider   = UpdatePrice.getProvider(UpdateProvider.GOOGLE);
		UpdateProvider priceYahooProvider    = UpdatePrice.getProvider(UpdateProvider.YAHOO);
		UpdateProvider dividendYahooProvider = UpdateDividend.getProvider(UpdateProvider.YAHOO);
		
		List<Stats> statsList = new ArrayList<>();
		
		Collection<Stock> stockCollection = StockUtil.getAll();
		
//		Collection<Stock> stockCollection = new ArrayList<>();
//		stockCollection.add(StockUtil.get("IBM"));
//		stockCollection.add(StockUtil.get("NYT"));
//		stockCollection.add(StockUtil.get("PEP"));
		
		int total = stockCollection.size();
		int count = 0;
		
		int showInterval = 10000;
		boolean showOutput;
		int lastOutputCount = -1;
		for(Stock stock: stockCollection) {
			String symbol = stock.symbol;

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
				File priceGoogle   = priceGoogleProvider.getFile(stock.symbol);
				File priceYahoo    = priceYahooProvider.getFile(stock.symbol);
				File dividendYahoo = dividendYahooProvider.getFile(stock.symbol);
								
				if (priceGoogle.exists() && priceYahoo.exists()) {
					// both
					List<Price> priceListGoogle = Price.load(priceGoogle);
					List<Price> priceListYahoo  = Price.load(priceYahoo);
					
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
					priceList = Price.load(priceGoogle);
				} else if (priceYahoo.exists()) {
					// only yahoo
					priceList = Price.load(priceYahoo);
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
				dividendList = Dividend.load(dividendFile);
			} else {
				dividendList = new ArrayList<>();
			}
			
			statsList.add(getInstance(stock, priceList, dividendList));
		}
		Stats.save(statsList);
		logger.info("stats  {}", String.format("%4d", statsList.size()));
		logger.info("STOP");
	}
}