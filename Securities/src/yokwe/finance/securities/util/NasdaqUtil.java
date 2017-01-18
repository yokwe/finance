package yokwe.finance.securities.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.NasdaqTable;

public class NasdaqUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	private static final String NASDAQ_FILE_PATH = "tmp/database/nasdaq.csv";

	private static Map<String, NasdaqTable> map = new TreeMap<>();
	
	static {
		List<NasdaqTable> tableList = CSVUtil.load(NASDAQ_FILE_PATH, NasdaqTable.class);
		for(NasdaqTable table: tableList) {
			map.put(table.symbol, table);
		}
	}
	
	public static NasdaqTable get(String symbol) {
		if (!map.containsKey(symbol)) {
			logger.error("symbol = {}", symbol);
			throw new SecuritiesException();
		}
		return map.get(symbol);
	}
	
	public static Collection<NasdaqTable> getAll() {
		return map.values();
	}
	
	public static Map<String, NasdaqTable> getMap() {
		return map;
	}
}
