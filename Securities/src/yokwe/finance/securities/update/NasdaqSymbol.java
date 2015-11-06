package yokwe.finance.securities.update;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.Scrape;

public class NasdaqSymbol {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(NasdaqSymbol.class);
	
	private static final String NEWLINE = "\n";

	public enum Field {
//		SYMBOL, EXCHANGE, SECTOR,
		SYMBOL, SECTOR,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			var quoteBoxSelectedSymbol="WFC^N";
			add(Field.SYMBOL,
					"var quoteBoxSelectedSymbol=\"(.+?)\";",
					"var quoteBoxSelectedSymbol=");

// <span id="qbar_exchangeLabel"><b>Exchange: </b>NYSE</span>
//			add(Field.EXCHANGE,
//					"<span id=\"qbar_exchangeLabel\"><b>Exchange: </b>(.+?)</span>",
//					"<span id=\"qbar_exchangeLabel\"><b>Exchange: </b>");

// <span id="qbar_sectorLabel"><b>Industry: </b><a href="http://www.nasdaq.com/screening/companies-by-industry.aspx?industry=Energy">Energy</a></span>
			add(Field.SECTOR,
					"<span id=\"qbar_sectorLabel\"><b>Industry: </b><a .+?>(.+?)</a>",
					"<span id=\"qbar_sectorLabel\"><b>Industry:");
		}
	}
	
	private static ScrapeGoogleFinance scrape = new ScrapeGoogleFinance();
	
	public static void save(String dirPath, String csvPath) {
		Map<String, Map<Field, String>> values = scrape.readDirectory(dirPath);
		//
		Field[] keys = Field.values();
		
		try (BufferedWriter br = new BufferedWriter(new FileWriter(csvPath))) {
			for(String symbol: values.keySet()) {
				Map<Field, String> map = values.get(symbol);
				int count = 0;
				for(Field field: keys) {
					if (count != 0) br.append(",");
					
					String value = map.get(field);
					// Use symbol from file name
					if (field.equals(Field.SYMBOL)) {
						value = symbol;
					}

					if (value == null) {
						logger.error("symbol = {}  field = {}  map = {}", symbol, field, map);
					}
					if (value.contains(",")) {
						br.append('"').append(value).append('"');
					} else {
						br.append(value);
					}
					count++;
				}
				br.append(NEWLINE);
			}
		} catch (IOException e) {
			logger.error("IOException {}", e);
			throw new SecuritiesException("IOException");
		}
	}

	public static void main(String[] args) throws IOException {
		logger.info("START");
		String dirPath = args[0];	
		String csvPath = args[1];
		
		logger.info("dirPath = {}", dirPath);
		logger.info("csvPath = {}", csvPath);

		//
		save(dirPath, csvPath);
		//
		logger.info("STOP");
	}
}
