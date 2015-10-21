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

public class FileUtil {
	private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

	public static String getContents(File file) {
		char[] buffer = new char[65536];
		
		StringBuilder ret = new StringBuilder();
		
		try (BufferedReader bfr = new BufferedReader(new FileReader(file), buffer.length)) {
			for(;;) {
				int len = bfr.read(buffer);
				if (len == -1) break;
				
				ret.append(buffer, 0, len);
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
		return ret.toString();
	}
	
	public static class NasdaqInfo {
		public final String etf;
		public final String exch;
		public final String symbol;
		public final String yahoo;
		public final String google;
		public final String nasdaq;
		public final String name;
		
		NasdaqInfo(String etf, String exch, String symbol, String yahoo, String google, String nasdaq, String name) {
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
	
	private static final String NASDAQ_FILE_PATH = "tmp/sqlite/nasdaq.csv";
	public static Map<String, NasdaqInfo> getNasdaqInfo() {
		Map<String, NasdaqInfo> ret = new TreeMap<>();
		
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
				
				ret.put(symbol, new NasdaqInfo(etf, exch, symbol, yahoo, google, nasdaq, name));
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
		
		return ret;
	}
	
//	public static void main(String[] args) {
//		Map<String, NasdaqInfo> map = getNasdaqInfo();
//		for(String symbol: map.keySet()) {
//			NasdaqInfo info = map.get(symbol);
//			logger.info("{}  {}", symbol, info);
//		}
//	}
}
