package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.eod.tax.BuySell;
import yokwe.finance.securities.eod.tax.Position;
import yokwe.finance.securities.eod.tax.Report;
import yokwe.finance.securities.eod.tax.Transaction;
import yokwe.finance.securities.libreoffice.SpreadSheet;
import yokwe.finance.securities.util.DoubleUtil;

public class UpdateHoldingPrice {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateHoldingPrice.class);
	
	public static List<String> getHoldigSymbolList() {
		Set<String> set = new TreeSet<>();
		
		String url = Report.URL_ACTIVITY;
		logger.info("url        {}", url);		
		try (SpreadSheet docActivity = new SpreadSheet(url, true)) {

			// Create transaction from activity
			List<Transaction> transactionList = Transaction.getTransactionList(docActivity);
			
			// key is symbol
			BuySell.getBuySellMap(transactionList);
			
			{
				LocalDate last = UpdateProvider.DATE_LAST;
				for(LocalDate date = UpdateProvider.DATE_FIRST; date.isBefore(last) || date.isEqual(last); date = date.plusDays(1)) {
					if (Market.isClosed(date)) continue;
					List<Position> positionList = Position.getPositionList(date.toString());
					for(Position position: positionList) {
						if (DoubleUtil.isAlmostZero(position.quantity)) continue;
						if (set.contains(position.symbol)) continue;
						set.add(position.symbol);
					}
				}
			}
		}
		List<String> ret = new ArrayList<>();
		for(String symbol: set) {
			symbol = symbol.replace(".PR.", "-");
			ret.add(symbol);
		}
		return ret;
	}

	public static void main(String[] args) {
		logger.info("START");
		
		logger.info("DATE_FIRST {}", UpdateProvider.DATE_FIRST);
		logger.info("DATE_LAST  {}", UpdateProvider.DATE_LAST);
		
		List<String> holdingSymbolList = getHoldigSymbolList();

		UpdateProvider providerIEX    = UpdatePrice.getProvider(UpdateProvider.IEX);
		UpdateProvider providerYahoo  = UpdatePrice.getProvider(UpdateProvider.YAHOO);
		
		{
			File dir = providerIEX.getFile("DUMMY").getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			// Remove unknown file
			File[] fileList = dir.listFiles();
			for(File file: fileList) {
				String name = file.getName();
				if (name.endsWith(".csv")) {
					String symbol = name.replace(".csv", "");
					if (StockUtil.contains(symbol)) continue;
				}
				
				logger.info("delete unknown file {}", name);
				file.delete();
			}
		}
		
		{
			File dir = providerYahoo.getFile("DUMMY").getParentFile();
			if (!dir.exists()) {
				dir.mkdirs();
			} else {
				if (!dir.isDirectory()) {
					logger.info("Not directory {}", dir.getAbsolutePath());
					throw new SecuritiesException("Not directory");
				}
			}
			
			// Remove unknown file
			File[] fileList = dir.listFiles();
			for(File file: fileList) {
				String name = file.getName();
				if (name.endsWith(".csv")) {
					String symbol = name.replace(".csv", "");
					if (StockUtil.contains(symbol)) continue;
				}
				
				logger.info("delete unknown file {}", name);
				file.delete();
			}
		}

		{
			Map<String, Stock> stockMap = new TreeMap<>();
			for(String symbol: holdingSymbolList) {
				if (StockUtil.contains(symbol)) {
					logger.info("{}", symbol);
					Stock stock = StockUtil.get(symbol);
					stockMap.put(stock.symbol, stock);
				}
			}
			
			logger.info("UPDATE PRICE IEX");
			UpdatePrice.updateFile(providerIEX, stockMap);
			logger.info("UPDATE PRICE YAHOO");
			UpdatePrice.updateFile(providerYahoo,  stockMap);
		}
		
		logger.info("STOP");
	}
}
