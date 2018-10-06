package yokwe.finance.stock.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.data.Price;
import yokwe.finance.stock.data.YahooPrice;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.DoubleUtil;

public class UpdateYahooPrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateYahooPrice.class);

	public static final String DIR_HOME        = System.getProperty("user.home");
	public static final String DIR_YAHOO_PRICE = "Dropbox/Trade/YahooPrice";
	public static final String DIR_IN          = String.format("%s/%s", DIR_HOME, DIR_YAHOO_PRICE);
	public static final String DIR_OUT         = "tmp/yahooPrice";
	
	private static void updateYahooPrice(File file) {
		String symbol = file.getName().replace(".csv", "");
		String pathOut = String.format("%s/%s.csv", DIR_OUT, symbol);
		
		List<YahooPrice> yahooPriceList = CSVUtil.loadWithHeader(file.getPath(), YahooPrice.class);
		if (yahooPriceList == null) {
			logger.warn("Failed to load CSV file {}", file.getPath());
			return;
		}
		
		List<Price> priceList = new ArrayList<>();
		for(YahooPrice yahooPrice: yahooPriceList) {
			// String date, String symbol, double open, double high, double low, double close, long volume
			Price price = new Price(yahooPrice.date, symbol,
					DoubleUtil.roundPrice(yahooPrice.open), DoubleUtil.roundPrice(yahooPrice.high),
					DoubleUtil.roundPrice(yahooPrice.low), DoubleUtil.roundPrice(yahooPrice.close), yahooPrice.volume);
			priceList.add(price);
		}
		
		logger.info("file {} - {}", pathOut, priceList.size());
		CSVUtil.saveWithHeader(priceList, pathOut);
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		logger.info("DIR_IN  {}", DIR_IN);
		logger.info("DIR_OUT {}", DIR_OUT);
		
		// Remove contents of DIR_OUT
		{
			File dirOut = new File(DIR_OUT);
			for(File file: dirOut.listFiles()) {
				if (file.isDirectory()) continue;
				file.delete();
			}
		}
		// Build contents of DIR_OUT from DIR_IN
		{
			File dirIn = new File(DIR_IN);
			for(File file: dirIn.listFiles()) {
				if (file.isDirectory()) continue;
				if (!file.isFile()) continue;
				if (!file.canRead()) continue;
				if (!file.getName().endsWith(".csv")) continue;
				
				updateYahooPrice(file);
			}
		}
		
		logger.info("STOP");
	}
}
