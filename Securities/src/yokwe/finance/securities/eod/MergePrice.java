package yokwe.finance.securities.eod;

import java.io.File;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class MergePrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MergePrice.class);

	private static final UpdateProvider priceGoogleProvider   = UpdatePrice.getProvider(UpdateProvider.GOOGLE);
	private static final UpdateProvider priceYahooProvider    = UpdatePrice.getProvider(UpdateProvider.YAHOO);
	
	private static int count       = 0;
	private static int countGoogle = 0;
	private static int countYahoo  = 0;

	public static void mergeFile(String symbol) {
		File file = Price.getFile(symbol);
		
		File priceGoogle   = priceGoogleProvider.getFile(symbol);
		File priceYahoo    = priceYahooProvider.getFile(symbol);
		
		List<Price> priceList = null;
		
		if (priceGoogle.exists() && priceYahoo.exists()) {
			// both
			List<Price> priceListGoogle = Price.load(priceGoogle);
			List<Price> priceListYahoo  = Price.load(priceYahoo);
			
			String dateGoogle = priceListGoogle.get(0).date;
			String dateYahoo  = priceListYahoo.get(0).date;
			
			if (dateGoogle.equals(UpdateProvider.DATE_LAST) && dateYahoo.equals(UpdateProvider.DATE_LAST)) {
				// both
				int priceGoogleSize = priceListGoogle.size();
				int priceYahooSize  = priceListYahoo.size();
				
				if (priceGoogleSize <= priceYahooSize) { // prefer yahoo over google
					priceList = priceListYahoo;
					countYahoo++;
				} else {
					priceList = priceListGoogle;
					countGoogle++;
				}
			} else if (dateGoogle.equals(UpdateProvider.DATE_LAST)) {
				// google
				priceList = priceListGoogle;
				countGoogle++;
			} else if (dateYahoo.equals(UpdateProvider.DATE_LAST)) {
				// yahoo
				priceList = priceListYahoo;
				countYahoo++;
			} else {
				// none -- could be happen for discontinued stock
				int priceGoogleSize = priceListGoogle.size();
				int priceYahooSize  = priceListYahoo.size();
				
				if (priceGoogleSize < priceYahooSize) {
					priceList = priceListYahoo;
					countYahoo++;
				} else {
					priceList = priceListGoogle;
					countGoogle++;
				}
			}
		} else if (priceGoogle.exists()) {
			// only google
			priceList = Price.load(priceGoogle);
			countGoogle++;
		} else if (priceYahoo.exists()) {
			// only yahoo
			priceList = Price.load(priceYahoo);
			countYahoo++;
		} else {
			// none
//			logger.warn("{}  skip   {}", String.format("%4d / %4d",  count, total), String.format("%-8s NO PRICE DATA", symbol));
		}
		
		if (priceList != null) {
			Price.save(priceList, file);
			count++;
		}
	}
	
	public static void main(String[] args) {
		logger.info("START");

		{
			File dir = Price.getFile("DUMMY").getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			// Remove all file
			File[] fileList = dir.listFiles();
			for(File file: fileList) {
				file.delete();
			}
		}

		logger.info("MERGE");
		for(String symbol: StockUtil.getSymbolList()) {
			mergeFile(symbol);
		}
		
		logger.info("symbol {}", String.format("%4d", StockUtil.getSymbolList().size()));
		logger.info("count  {}", String.format("%4d", count));
		logger.info("yahoo  {}", String.format("%4d", countYahoo));
		logger.info("google {}", String.format("%4d", countGoogle));
		
		logger.info("STOP");
	}
}
