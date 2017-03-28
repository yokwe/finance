package yokwe.finance.securities.eod;

import java.io.File;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class UpdateDelisted {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateDelisted.class);
	
	public static final String PATH_CSV = "data/stock/delisted.csv";
	public static final String PATH_DIR = "tmp/eod/delisted";
	

	public static void main(String[] args) {
		logger.info("START");
		
		// create necessary dir
		new File(PATH_DIR).mkdirs();
		
		UpdateProvider updateProviderGoogle   = UpdatePrice.getProvider(UpdateProvider.GOOGLE);
//		UpdateProvider updateProviderYahoo    = UpdatePrice.getProvider(UpdateProvider.YAHOO);
		
		// Create price file for delisted stock
		List<Delisted> delistdList = Delisted.load();
		
		for(Delisted delisted: delistdList) {
			String exch   = delisted.exchange;
			String symbol = delisted.symbol;
			logger.info("{}:{}", exch, symbol);
			
			File fileDelisted = Delisted.getFile(symbol);
			
			// Google
			File fileGoogle = updateProviderGoogle.getFile(symbol);
			fileGoogle.delete(); // make sure file is actually created
			
			updateProviderGoogle.updateFile(exch, symbol, symbol, true, UpdateProvider.DATE_FIRST, UpdateProvider.DATE_LAST);
			if (fileGoogle.canRead()) {
				boolean success = fileGoogle.renameTo(fileDelisted);
				if (!success) {
					logger.error("failed to rename file  {} -> {}", fileGoogle.getPath(), fileDelisted.getPath());
					throw new SecuritiesException("Unexpected symbol");
				}
				
				List<Price> priceList = Price.load(fileDelisted);
				priceList.sort((a, b) -> (a.date.compareTo(b.date)));
				if (priceList.size() == 0) {
					logger.info("  EMPTY");
				} else {
					logger.info("  {} {}", priceList.get(0).date, priceList.get(priceList.size() - 1).date);
				}
			} else {
				logger.warn("  Failed to update");
			}
		}
		
		logger.info("STOP");
	}
}
