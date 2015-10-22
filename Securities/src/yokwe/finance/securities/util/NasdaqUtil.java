package yokwe.finance.securities.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class NasdaqUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	private static final String NASDAQ_FILE_PATH = "tmp/sqlite/nasdaq.csv";

	public static class Entry {
		public final String etf;
		public final String exch;
		public final String symbol;
		public final String yahoo;
		public final String google;
		public final String nasdaq;
		public final String name;
		
		Entry(String etf, String exch, String symbol, String yahoo, String google, String nasdaq, String name) {
			this.etf    = etf;
			this.exch   = exch;
			this.symbol = symbol;
			this.yahoo  = yahoo;
			this.google = google;
			this.nasdaq = nasdaq;
			this.name   = name;
		}
		
		public String toString() {
			return String.format("%s,%s,%s,%s,%s,%s,%s", etf, exch, symbol, yahoo, google, nasdaq, name);
		}
	}
	
	private static Map<String, Entry> map = new TreeMap<>();
	
	static {
		char[] buffer = new char[65536];
		
		try (BufferedReader br = new BufferedReader(new FileReader(new File(NASDAQ_FILE_PATH)), buffer.length)) {
			for(;;) {
				String line = br.readLine();
				if (line == null) break;
				
				String[] fields = line.split(",", 7);
				String etf    = fields[0];
				String exch   = fields[1];
				String symbol = fields[2];
				String yahoo  = fields[3];
				String google = fields[4];
				String nasdaq = fields[5];
				String name   = fields[6];
				
				if (name.charAt(0) == '"') name = name.substring(1, name.length() - 1);
				if (name.contains("\"\"")) name = name.replace("\"\"", "\"");
				
				map.put(symbol, new Entry(etf, exch, symbol, yahoo, google, nasdaq, name));
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
	
	public static Entry get(String symbol) {
		if (!map.containsKey(symbol)) {
			logger.error("symbol = {}", symbol);
			throw new SecuritiesException();
		}
		
		return map.get(symbol);
	}
}
