package yokwe.finance.securities.eod;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
			Price lastPrice = priceList.get(0);
			this.sample   = lastPrice.date;
			this.price    = lastPrice.close;
			this.vol      = lastPrice.volume;
		}
		
		{
			double[] priceArray = priceList.stream().mapToDouble(o -> o.close).toArray();
			this.priceCount = priceArray.length;
			
			{
				double logReturn[] = DoubleArray.logReturn(priceArray);
				DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats();
				Arrays.stream(logReturn).forEach(stats);
				this.sd = DoubleUtil.round(stats.getStandardDeviation(), 4);
			}
			
			HV hv = new HV();
			Arrays.stream(priceArray).forEach(hv);
			this.hv = DoubleUtil.round(hv.getValue(), 4);
			
			RSI rsi = new RSI();
			Arrays.stream(priceArray).forEach(rsi);
			this.rsi = DoubleUtil.round(rsi.getValue(), 1);
			
			DoubleStreamUtil.Stats stats = new DoubleStreamUtil.Stats();
			Arrays.stream(priceArray).forEach(stats);
			this.min = DoubleUtil.round(stats.getMin(), 2);
			this.max = DoubleUtil.round(stats.getMax(), 2);
			this.minPercent = DoubleUtil.round((this.price - this.min) / this.price, 3);
			this.maxPercent = DoubleUtil.round((this.max - this.price) / this.price, 3);
			
			MA price20 = MA.sma(20);
			Arrays.stream(priceArray).forEach(price20);
			this.sma20 = DoubleUtil.round(price20.getValue(), 2);
			
			MA price50 = MA.sma(50);
			Arrays.stream(priceArray).forEach(price50);
			this.sma50 = DoubleUtil.round(price50.getValue(), 2);
			
			MA price200 = MA.sma(200);
			Arrays.stream(priceArray).forEach(price200);
			this.sma200 = DoubleUtil.round(price200.getValue(), 2);
		}
		
		// dividend
		{
			double[] divArray = dividendList.stream().mapToDouble(o -> o.dividend).toArray();
			
			Arrays.stream(divArray).sum();

			this.div      = DoubleUtil.round(Arrays.stream(divArray).sum(), 4);
			this.divCount = divArray.length;
			this.divYield = DoubleUtil.round(this.div / this.price, 3);
		}
		
		// volume
		{
			double[] volArray = priceList.stream().mapToDouble(o -> o.volume).toArray();

			MA vol5 = MA.sma(5);
			Arrays.stream(volArray).forEach(vol5);
			this.vol5 = (long)vol5.getValue();

			MA vol30 = MA.sma(30);
			Arrays.stream(volArray).forEach(vol30);
			this.vol30 = (long)vol30.getValue();
		}
	}
	
	public static void main(String[] args) {
		final org.slf4j.Logger logger = LoggerFactory.getLogger(Stats.class);
		logger.info("START");
				
		final String PATH_STATS   = "tmp/eod/stats.csv";
		final String PATH_PRICE   = "tmp/eod/price-yahoo";
		final String PATH_DIVIDED = "tmp/eod/dividend-yahoo";
		
		File dirPrice    = new File(PATH_PRICE);
		File dirDividend = new File(PATH_DIVIDED);
		
		File[] priceFiles = dirPrice.listFiles(o -> o.getName().endsWith(".csv"));
		Arrays.sort(priceFiles);
		
		List<Stats> statsList = new ArrayList<>();
		int total = priceFiles.length;
		int count = 0;
		
		int showInterval = 100;
		boolean showOutput;
		int lastOutputCount = -1;
		for(File priceFile: priceFiles) {
			int outputCount = count / showInterval;
			if (outputCount != lastOutputCount) {
				showOutput = true;
				lastOutputCount = outputCount;
			} else {
				showOutput = false;
			}

			count++;
			
			String symbol = priceFile.getName().replace(".csv", "");

			if (!NasdaqUtil.contains(symbol)) {
				logger.error("Unknown symbol {}", symbol);
				throw new SecuritiesException("Unknown symbol");
			}
			NasdaqTable nasdaq = NasdaqUtil.get(symbol);
			
			List<Price> priceList = CSVUtil.loadWithHeader(priceFile.getPath(), Price.class);
			
			{
				boolean penyyStock = false;
				for(Price price: priceList) {
					if (price.close < 1.0) {
						penyyStock = true;
						break;
					}
				}
				if (penyyStock) {
//					logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s PENNY STOCK", symbol));
					continue;
				}
			}
			
			if (priceList.size() < RSI.DEFAULT_PERIDO) {
				logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s %2d", symbol, priceList.size()));
				continue;
			}
			
			if (showOutput) logger.info("{}  update {}", String.format("%4d / %4d",  count, total), symbol);
			
			List<Dividend> dividendList;
			File fileDividend = new File(dirDividend, priceFile.getName());
			if (fileDividend.exists()) {
				dividendList = CSVUtil.loadWithHeader(fileDividend.getPath(), Dividend.class);
			} else {
				dividendList = new ArrayList<>();
			}
			
			statsList.add(new Stats(nasdaq, priceList, dividendList));
		}
		CSVUtil.saveWithHeader(statsList, PATH_STATS);
		logger.info("STOP");
	}
}
