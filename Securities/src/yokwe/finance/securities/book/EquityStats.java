package yokwe.finance.securities.book;

import java.util.HashMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

@SheetData.SheetName("equityStats-header")
@SheetData.HeaderRow(0)
@SheetData.DataRow(1)
public class EquityStats extends SheetData {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(EquityStats.class);
	
	public static class Map {
		private java.util.Map<String, EquityStats> map = new HashMap<>();
		
		public Map(String url) {
			try (LibreOffice libreOffice = new LibreOffice(url, true)) {
				for(EquityStats equityStats: SheetData.getInstance(libreOffice, EquityStats.class)) {
					String symbol = equityStats.symbol;
					if (map.containsKey(symbol)) {
						logger.info("Duplicate symbol = {}", symbol);
						throw new SecuritiesException("Unexpected");
					}
					map.put(equityStats.symbol, equityStats);
				}
				logger.info("size = {}", map.size());
			}
		}
		
		public EquityStats get(String symbol) {
			if (map.containsKey(symbol)) {
				return map.get(symbol);
			}
			logger.info("Unexpected symbol = {}", symbol);
			throw new SecuritiesException("Unexpected");
		}
	}

	
	@ColumnName("symbol")
	public String symbol;
//	@ColumnName("name")
//	public String name;
	@ColumnName("price")
	public double price;
//	@ColumnName("sd")
//	public double sd;
//	@ColumnName("div")
//	public double div;
//	@ColumnName("freq")
//	public double freq;
//	@ColumnName("changepct")
//	public double changepct;
//	@ColumnName("count")
//	public int ount;
//	@ColumnName("hv")
//	public double hv;
//	@ColumnName("change")
//	public double change;
//	@ColumnName("var95")
//	public double var95;
//	@ColumnName("var99")
//	public double var99;
//	@ColumnName("rsi")
//	public double rsi;
//	@ColumnName("price200")
//	public double price200;
//	@ColumnName("divYield")
//	public double divYield;
//	@ColumnName("beta")
//	public double beta;
//	@ColumnName("r2")
//	public double r2;
//	@ColumnName("vol5")
//	public int vol5;
//	@ColumnName("min")
//	public double min;
//	@ColumnName("max")
//	public double max;
//	@ColumnName("minpct")
//	public double minpct;
//	@ColumnName("maxpct")
//	public double maxpct;

	
	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016.ods";
		
		logger.info("START");
		EquityStats.Map map = new EquityStats.Map(url);

		{
			String symbol = "A";
			EquityStats equityStats = map.get(symbol);
			logger.info("symbol {} => {}", symbol, equityStats.price);
		}

		{
			String symbol = "AA";
			EquityStats equityStats = map.get(symbol);
			logger.info("symbol {} => {}", symbol, equityStats.price);
		}

		logger.info("STOP");
		System.exit(0);
	}
}
