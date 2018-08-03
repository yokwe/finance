package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.stats.DoubleArray;
import yokwe.finance.securities.stats.HV;
import yokwe.finance.securities.stats.MA;
import yokwe.finance.securities.stats.RSI;
import yokwe.finance.securities.util.DoubleStreamUtil;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateStats {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStats.class);
	
	private static final LocalDate DATE_LAST  = Market.getLastTradingDate();
	private static final String STRING_DATE_LAST  = DATE_LAST.toString();
	private static final String STRING_DATE_FIRST = DATE_LAST.minusYears(1).toString();

	public static final String PATH_STATS        = "tmp/eod/stats.csv";

	private static Stats getInstance(Stock stock, List<Price> priceList, List<Dividend> dividendList) {
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
			// Limit to 1 year
			double[] priceArray = priceList.stream().mapToDouble(o -> o.close).toArray();
			ret.pricec = priceArray.length;
			
			{
				double logReturn[] = DoubleArray.logReturn(priceArray);
				DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats(logReturn);
				
				double sd = stats.getStandardDeviation();
				ret.sd = Double.isNaN(sd) ? -1 : DoubleUtil.round(sd, 4);
			}
			
			HV hv = new HV(priceArray);
			ret.hv = Double.isNaN(hv.getValue()) ? -1 : DoubleUtil.round(hv.getValue(), 4);
			
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
			ret.last   = (priceArray.length < 2) ? -1 : priceArray[priceArray.length - 2];
			ret.sma5   = DoubleUtil.round(MA.sma(  5, priceArray).getValue(), 2);
			ret.sma20  = DoubleUtil.round(MA.sma( 20, priceArray).getValue(), 2);
			ret.sma50  = DoubleUtil.round(MA.sma( 50, priceArray).getValue(), 2);
			ret.sma200 = DoubleUtil.round(MA.sma(200, priceArray).getValue(), 2);
			
			ret.lastpct   = DoubleUtil.round((ret.price - ret.last)   / ret.last,   3);
			ret.sma5pct   = DoubleUtil.round((ret.price - ret.sma5)   / ret.sma5,   3);
			ret.sma20pct  = DoubleUtil.round((ret.price - ret.sma20)  / ret.sma20,  3);
			ret.sma50pct  = DoubleUtil.round((ret.price - ret.sma50)  / ret.sma50,  3);
			ret.sma200pct = DoubleUtil.round((ret.price - ret.sma200) / ret.sma200, 3);
		}
		
		// dividend
		{
			// Limit to 1 year
			double[] divArray = dividendList.stream().mapToDouble(o -> o.dividend).toArray();
			
			ret.div   = DoubleUtil.round(Arrays.stream(divArray).sum(), 4);
			ret.divc  = divArray.length;
			ret.yield = DoubleUtil.round(ret.div / ret.price, 3);
			
			double divLast  = (divArray.length == 0) ? 0 : divArray[divArray.length - 1];
			double divAdj;
			switch(ret.divc) {
			case 13:
				divAdj = divLast * 12; // remove irregular dividend
				break;
			case 12:
				divAdj = divLast * 12;
				break;
			case 4:
				divAdj = divLast * 4;
				break;
			case 2:
				divAdj = divLast * 2;
				break;
			case 1:
				divAdj = divLast;
				break;
			default:
				divAdj = 0;
				break;
			}
			ret.yieldadj = DoubleUtil.round(divAdj / ret.price, 3);
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
		
		List<Stats> statsList = new ArrayList<>();
		
		Collection<Stock> stockCollection = UpdateStock.getStockList();
		
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
			
			File priceFile    = new File(UpdatePrice.getCSVPath(symbol));
			File dividendFile = new File(UpdateDividend.getCSVPath(symbol));
			
			if (!priceFile.exists()) continue;
			
			// Skip zero data
			List<Price> priceList = UpdatePrice.load(symbol).stream().filter(o -> (0 < o.open && 0 < o.high && 0 < o.low && 0 < o.close)).collect(Collectors.toList());
			if (priceList.size() == 0) continue;
			
			// Order of data is very important to calculate statistics number
			priceList.sort((a, b) -> a.date.compareTo(b.date));

			// date is not last trading date
			{
				String date = priceList.get(priceList.size() - 1).date;
				if (!date.equals(STRING_DATE_LAST)) {
					logger.warn("{}  old    {}", String.format("%4d / %4d",  count, total), String.format("%-8s %s", symbol, date));
				}
			}
			
			// Ignore very small price stock
			{
				double minimumPrice = 0.01;
				boolean tooSmall = false;
				for(Price price: priceList) {
					if (price.open <= minimumPrice) {
						tooSmall = true;
						break;
					}
					if (price.high <= minimumPrice) {
						tooSmall = true;
						break;
					}
					if (price.low <= minimumPrice) {
						tooSmall = true;
						break;
					}
					if (price.close <= minimumPrice) {
						tooSmall = true;
						break;
					}
				}
				if (tooSmall) {
					logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s TOO SMALL PRICE", symbol));
					continue;
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
//			if (priceList.size() <= 5) {
//				logger.warn("{}  small  {}", String.format("%4d / %4d",  count, total), String.format("%-8s %2d", symbol, priceList.size()));
//				continue;
//			}
			
			if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, total), symbol);
			
			List<Dividend> dividendList;
			if (dividendFile.exists()) {
				// Filter data for last one year
				dividendList = UpdateDividend.load(symbol).stream().filter(o -> (0 < o.date.compareTo(STRING_DATE_FIRST))).collect(Collectors.toList());;
				// Order of data is very important to calculate statistics number
				dividendList.sort((a, b) -> a.date.compareTo(b.date));
			} else {
				dividendList = new ArrayList<>();
			}
			
			Stats stats = getInstance(stock, priceList, dividendList);
			if (stats != null) statsList.add(getInstance(stock, priceList, dividendList));
		}
		Stats.save(statsList);
		logger.info("stats  {}", String.format("%4d", statsList.size()));
		logger.info("STOP");
	}
}
