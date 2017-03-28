package yokwe.finance.securities.eod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class MergePrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MergePrice.class);

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
			
			// Copy delisted files
			for(Delisted delisted: Delisted.load()) {
				String symbol = delisted.symbol;
				logger.info("delisted  {}", symbol);
				File delistedFile = Delisted.getFile(symbol);
				if (delistedFile.exists()) {
					try {
						File priceFile = Price.getFile(symbol);
						Files.copy(delistedFile.toPath(), priceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						logger.info("IOException {}", e.getMessage());
						throw new SecuritiesException("IOException");
					}
				}
			}
		}

		UpdateProvider priceGoogleProvider   = UpdatePrice.getProvider(UpdateProvider.GOOGLE);
		UpdateProvider priceYahooProvider    = UpdatePrice.getProvider(UpdateProvider.YAHOO);
		
		// Create price file for delisted stock
		List<String> symbolList = new ArrayList<>(StockUtil.getSymbolList());		
		
		int total       = symbolList.size();
		int count       = 0;
		int countGoogle = 0;
		int countYahoo  = 0;
		
		int lastOutputCount = -1;
		int showInterval = 100;

		logger.info("MERGE");
		for(String symbol: symbolList) {
			int outputCount = count / showInterval;
			boolean showOutput;
			if (outputCount != lastOutputCount) {
				showOutput = true;
				lastOutputCount = outputCount;
			} else {
				showOutput = false;
			}
			count++;
			
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
					
					if (priceYahooSize <= priceGoogleSize) { // prefer google over google
						priceList = priceListGoogle;
						countGoogle++;
					} else {
						priceList = priceListYahoo;
						countYahoo++;
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
//				if (showOutput) logger.warn("{}  skip  {}", String.format("%4d / %4d",  count, total), symbol);
				continue;
			}
			
			if (priceList != null) {
				if (showOutput) logger.info("{}  save  {}", String.format("%4d / %4d",  count, total), symbol);
				Price.save(priceList, file);
			}
		}
		
		logger.info("total  {}", String.format("%4d", total));
		logger.info("count  {}", String.format("%4d", count));
		logger.info("yahoo  {}", String.format("%4d", countYahoo));
		logger.info("google {}", String.format("%4d", countGoogle));
		
		logger.info("STOP");
	}
}
