package yokwe.finance.securities.update;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.Scrape;

public class GoogleFinance {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(GoogleFinance.class);
	
	private static final String NEWLINE = "\n";

	public enum Field {
		SYMBOL, SECTOR, INDUSTRY,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<meta itemprop="tickerSymbol"
//			        content="A" />
			add(Field.SYMBOL,
					"<meta itemprop=\"tickerSymbol\"\\p{javaWhitespace}+content=\"(.+?)\" />",
					"<meta itemprop=\"tickerSymbol\"",
					Pattern.DOTALL);

// <a id=sector href="?catid=us-TRBC:52&amp;ei=Bwc7VsnQB6jBigLn55mYBg">Industrials</a>
// <a id=sector href="?catid=us-TRBC:55&amp;ei=4Ac7VqqiDJC0iALJ86TwCg">Financials</a>
			add(Field.SECTOR,
					"<a id=sector href=\"\\?catid=us-TRBC.+?>(.+?)</a>",
					"<a id=sector href=\"?catid=us-TRBC");

// <a href="?catid=us-TRBC:5210202014&amp;ei=Bwc7VsnQB6jBigLn55mYBg">Locomotive Engines &amp; Rolling Stock</a>
// <a href="?catid=us-TRBC:5550104010&amp;ei=4Ac7VqqiDJC0iALJ86TwCg">Exchange Traded Funds - NEC</a>
			add(Field.INDUSTRY,
					"<a href=\"\\?catid=us-TRBC.+?>(.+?)</a>",
					"<a href=\"?catid=us-TRBC");
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
