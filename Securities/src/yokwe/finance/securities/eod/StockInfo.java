package yokwe.finance.securities.eod;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

public class StockInfo {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(StockInfo.class);
	
	private static Map<String, StockInfo> stockInfoMap;
	static {
		{
			Map<String, List<StockHistory>> stockHistoryMap = UpdateStockHistory.getStockHistoryMap();

			Map<String, StockInfo> map = new TreeMap<>();
			for(List<StockHistory> list: stockHistoryMap.values()) {
				StockInfo activeStockHistory = new StockInfo(list);
				map.put(activeStockHistory.symbol, activeStockHistory);
			}
			stockInfoMap = Collections.unmodifiableMap(map);
		}
	}
	
	public static Map<String, StockInfo> getStockInfoMap() {
		return stockInfoMap;
	}
	
	public final String             symbol;
	public final boolean            active;
	public final List<StockHistory> stockHistoryList;
	public final StockHistory       lastStockHistory;
	public final List<StockHistory> lastStockHistoryList;
	
	private StockInfo(List<StockHistory> stockHistory) {
		StockHistory lastStockHistory = stockHistory.get(stockHistory.size() - 1);

		this.symbol               = lastStockHistory.group;
		this.stockHistoryList     = Collections.unmodifiableList(stockHistory);
		this.lastStockHistory     = lastStockHistory;
		this.lastStockHistoryList = Collections.unmodifiableList(stockHistoryList.stream().filter(o -> o.session == lastStockHistory.session).collect(Collectors.toList()));
		this.active               = lastStockHistory.totalQuantity != 0;
	}
	
	public static void main(String[] args) {
		logger.info("START");
		
		{
			int count_active = 0;
			Map<String, StockInfo> stockInfoMap = getStockInfoMap();
			for(Map.Entry<String, StockInfo> entry: stockInfoMap.entrySet()) {
				StockInfo stockInfo = entry.getValue();
				
				logger.info("{} {}", String.format("%-8s", stockInfo.symbol), stockInfo.active ? "ACTIVE" : "");
				if (stockInfo.active) {
					count_active++;
					for(StockHistory stockHistory: stockInfo.lastStockHistoryList) {
						logger.info("  {}", stockHistory);
					}
				}
			}
			logger.info("stockHistoryInfoMap {} / {}", count_active, stockInfoMap.size());
		}
		
		logger.info("STOP");
	}
}
