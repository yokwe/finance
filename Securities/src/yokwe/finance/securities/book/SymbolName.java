package yokwe.finance.securities.book;

import java.util.LinkedHashMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

@SheetData.SheetName("symbol-name")
@SheetData.HeaderRow(0)
@SheetData.DataRow(1)
public class SymbolName extends SheetData {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(SymbolName.class);

	public static class Map {
		private java.util.Map<String, String> map = new LinkedHashMap<>();
		
		public Map(String url) {
			try (LibreOffice libreOffice = new LibreOffice(url, true)) {
				for(SymbolName symbolName: SheetData.getInstance(libreOffice, SymbolName.class)) {
					map.put(symbolName.symbol, symbolName.name);
				}
			}
		}
		
		public String getName(String symbol) {
			if (!map.containsKey(symbol)) {
				logger.error("Unknown symbol = {}", symbol);
				throw new SecuritiesException("Unexpected");
			}
			return map.get(symbol);
		}
	}
	
	@ColumnName("Symbol")
	public String symbol;
	@ColumnName("Name")
	public String name;

	@Override
	public String toString() {
		return String.format("%-8s %s", symbol, name);
	}

	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016.ods";
		
		logger.info("START");
		SymbolName.Map map = new SymbolName.Map(url);
		
		for(java.util.Map.Entry<String, String> entry: map.map.entrySet()) {
			logger.info("{}", String.format("%-8s %s", entry.getKey(), entry.getValue()));
		}
		logger.info("STOP");
	}
}
