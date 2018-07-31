package yokwe.finance.securities.iex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.CSVUtil;

public class UpdateStats {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStats.class);
	
	public static final String PATH_STATS = "tmp/iex/stats.csv";
	
	public static void main (String[] args) {
		logger.info("START");
		
		List<Symbols> symbolsList = CSVUtil.loadWithHeader(UpdateSymbols.PATH_SYMBOLS, Symbols.class);
		logger.info("symbolsList {}", symbolsList.size());
		
		List<String> symbolList = symbolsList.stream().filter(o -> o.isEnabled).map(o -> o.symbol).collect(Collectors.toList());
		Collections.sort(symbolList);
		logger.info("symbolList {}", symbolList.size());
		
		int symbolListSize = symbolList.size();
		
		List<Stats> statsList = new ArrayList<>();
		for(int i = 0; i < symbolListSize; i += IEXBase.MAX_PARAM) {
			int fromIndex = i;
			int toIndex = Math.min(fromIndex + IEXBase.MAX_PARAM, symbolListSize);
			logger.info("  {}  {}", fromIndex, toIndex);
			
			List<String> getList = symbolList.subList(fromIndex, toIndex);
			logger.info("getList {}", getList.toString());
			
			Map<String, Stats> statsMap = Stats.getStock(getList.toArray(new String[0]));
			statsMap.values().stream().forEach(o -> statsList.add(o));
		}
		logger.info("statsList {}", statsList.size());
		CSVUtil.saveWithHeader(statsList, PATH_STATS);

		logger.info("STOP");
	}

}
