package yokwe.finance.securities.iex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateQuote {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateQuote.class);
	
	public static final String PATH_STATS = "tmp/iex/quote.csv";
	
	public static void main (String[] args) {
		logger.info("START");
		
		List<String> symbolList = UpdateSymbols.getSymbolList();
		int symbolListSize = symbolList.size();
		logger.info("symbolList {}", symbolList.size());
		
		List<Quote> quoteList = new ArrayList<>();
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			List<String> getList = symbolList.subList(fromIndex, toIndex);
			logger.info("  {} {}", fromIndex, getList.toString());
			
			Map<String, Quote> quoteMap = Quote.getStock(getList.toArray(new String[0]));
			quoteMap.values().stream().forEach(o -> quoteList.add(o));
		}
		logger.info("statsList {}", quoteList.size());
		CSVUtil.saveWithHeader(quoteList, PATH_STATS);

		logger.info("STOP");
	}

}
