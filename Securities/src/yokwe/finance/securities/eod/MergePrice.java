package yokwe.finance.securities.eod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class MergePrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MergePrice.class);

	public static void main(String[] args) {
		logger.info("START");

		final String dateLast = UpdateProvider.DATE_LAST.toString();

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
				
				// First record is latest.
				String dateGoogle = priceListGoogle.get(0).date;
				String dateYahoo  = priceListYahoo.get(0).date;
				
				if (dateGoogle.equals(dateLast) && dateYahoo.equals(dateLast)) {
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
				} else if (dateGoogle.equals(dateLast)) {
					// google
					priceList = priceListGoogle;
					countGoogle++;
				} else if (dateYahoo.equals(dateLast)) {
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
				// If there is a hole in priceList, supply value from existing price.
				if (file.exists()) {
					String dateFirst = UpdateProvider.DATE_FIRST.toString();
					
					SortedMap<String, Price> newPriceMap = new TreeMap<>();
					priceList.stream().forEach(o -> newPriceMap.put(o.date, o));
					
					List<Price> newPriceList = new ArrayList<>();
					for(Price price: Price.load(file)) {
						String date = price.date;
						
						// Too early for now
						if (date.compareTo(dateFirst) < 0) continue;
						
						if (newPriceMap.containsKey(date)) {
							// exist in newPriceMap
							newPriceList.add(newPriceMap.get(date));
						} else {
							// not exist in priceList, but exist in price file
							newPriceList.add(price);
							logger.info("Use existing  {}  {}", symbol, date);
						}
					}
					priceList = newPriceList;
				}

				Price.save(priceList, file);
			}
		}
		
		logger.info("total  {}", String.format("%4d", total));
		logger.info("count  {}", String.format("%4d", count));
		logger.info("yahoo  {}", String.format("%4d", countYahoo));
		logger.info("google {}", String.format("%4d", countGoogle));
		
		// Copy delisted files from UpdateDelisted.PATH_DIR
		{
			File dir = new File(UpdateDelisted.PATH_DIR);
			for(File file: dir.listFiles()) {
				String name = file.getName();
				// Sanity checks
				if (!name.endsWith(".csv")) {
					logger.warn("Not CSV file {}", file.getPath());
					continue;
				}
				if (file.length() == 0) {
					logger.warn("Empty file {}", file.getPath());
					continue;
				}
				
				String symbol = name.substring(0, name.length() - 4); // minus 4 for ".csv"
				logger.info("delisted  {}", symbol);

				try {
					File priceFile = Price.getFile(symbol);
					Files.copy(file.toPath(), priceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					logger.info("IOException {}", e.getMessage());
					throw new SecuritiesException("IOException");
				}
			}
		}
		// Copy delisted files using Delisted.load()
//		for(Delisted delisted: Delisted.load()) {
//			String symbol = delisted.symbol;
//			logger.info("delisted  {}", symbol);
//			File delistedFile = Delisted.getFile(symbol);
//			if (delistedFile.exists()) {
//				try {
//					File priceFile = Price.getFile(symbol);
//					Files.copy(delistedFile.toPath(), priceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//				} catch (IOException e) {
//					logger.info("IOException {}", e.getMessage());
//					throw new SecuritiesException("IOException");
//				}
//			}
//		}

		
		logger.info("STOP");
	}
}
