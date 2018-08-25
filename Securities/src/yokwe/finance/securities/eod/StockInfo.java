package yokwe.finance.securities.eod;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

public class StockInfo {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockInfo.class);
	
	public static class Entry {
		public final String             symbol;
		public final boolean            active;
		public final List<StockHistory> stockHistoryList;
		public final StockHistory       lastStockHistory;
		public final List<StockHistory> lastStockHistoryList;
		
		private Entry (List<StockHistory> stockHistory) {
			StockHistory lastStockHistory = stockHistory.get(stockHistory.size() - 1);

			this.symbol               = lastStockHistory.group;
			this.stockHistoryList     = Collections.unmodifiableList(stockHistory);
			this.lastStockHistory     = lastStockHistory;
			this.lastStockHistoryList = Collections.unmodifiableList(stockHistoryList.stream().filter(o -> o.session == lastStockHistory.session).collect(Collectors.toList()));
			this.active               = lastStockHistory.totalQuantity != 0;
		}
	}
	
	private final Map<String, Entry> entryMap;
	public StockInfo(String pathBase) {
		Map<String, Entry> map = new TreeMap<>();
		
		{
			Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap(pathBase);
			for(List<StockHistory> list: stockHistoryMap.values()) {
				Entry entry = new Entry(list);
				map.put(entry.symbol, entry);
			}
		}
		
		entryMap = Collections.unmodifiableMap(map);
	}
	
	public Map<String, Entry> getEntryMap() {
		return entryMap;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			int count_active = 0;
			StockInfo stockInfo = new StockInfo(".");
			
			Map<String, Entry> entryMap = stockInfo.entryMap;
			for(Entry entry: entryMap.values()) {				
				logger.info("{} {}", String.format("%-8s", entry.symbol), entry.active ? "ACTIVE" : "");
				if (entry.active) {
					count_active++;
					for(StockHistory stockHistory: entry.lastStockHistoryList) {
						logger.info("  {}", stockHistory);
					}
				}
			}
			logger.info("stockHistoryInfoMap {} / {}", count_active, entryMap.size());
		}
		
		logger.info("STOP");
	}
}
