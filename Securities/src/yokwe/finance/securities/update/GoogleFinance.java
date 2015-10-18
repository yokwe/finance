package yokwe.finance.securities.update;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.Scrape;

public class GoogleFinance {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(GoogleFinance.class);
	
	private static final String CRLF = "\r\n";

	public enum Field {
		SYMBOL, EXCHANGE, NAME,
		//NAME, EXCHANGE, VOLUME,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<meta itemprop="tickerSymbol"
//			        content="A" />
			add(Field.SYMBOL,
					"<meta itemprop=\"tickerSymbol\"\\p{javaWhitespace}+content=\"(.+?)\" />",
					"<meta itemprop=\"tickerSymbol\"",
					Pattern.DOTALL);
			
//			<meta itemprop="exchange"
//			        content="NYSE" />
			add(Field.EXCHANGE,
					"<meta itemprop=\"exchange\"\\p{javaWhitespace}+content=\"(.+?)\" />",
					"<meta itemprop=\"exchange\"",
					Pattern.DOTALL);

//			<meta itemprop="name"
//	           content="Vanguard Long Term Corporate Bond ETF" />
			add(Field.NAME,
				"<meta itemprop=\"name\"\\p{javaWhitespace}+content=\"(.+?)\" />",
				"<meta itemprop=\"name\"" ,
				Pattern.DOTALL);
		}
	}
	
	private static ScrapeGoogleFinance scrape = new ScrapeGoogleFinance();
	
	public static void save(String dirPath, String csvPath) {
		List<Map<Field, String>> values = scrape.readDirectory(dirPath);
		//
		Field[] keys = Field.values();
		
		try (BufferedWriter br = new BufferedWriter(new FileWriter(csvPath))) {
			for(Map<Field, String> map: values) {
				int count = 0;
				for(Field field: keys) {
					if (count != 0) br.append(",");
					String value = map.get(field);
					if (value.contains(",")) {
						br.append('"').append(map.get(field)).append('"');
					} else {
						br.append(map.get(field));
					}
					count++;
				}
				br.append(CRLF);
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
