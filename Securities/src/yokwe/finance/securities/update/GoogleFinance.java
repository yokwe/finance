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
		SYMBOL, PRICE, VOL, AVG_VOL, MKT_CAP,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<meta itemprop="tickerSymbol"
//			        content="A" />
			add(Field.SYMBOL,
					"<meta itemprop=\"tickerSymbol\"\\p{javaWhitespace}+content=\"(.+?)\" />",
					"<meta itemprop=\"tickerSymbol\"",
					Pattern.DOTALL);

//			<td class="key"
//	          data-snapfield="market_cap">Mkt cap
//	</td>
//	<td class="val">&nbsp;&nbsp;&nbsp;&nbsp;-
			add(Field.PRICE,
					"<div id=price-panel .+?<span id=\".+?\">(.+?)</span>",
					"<div id=price-panel" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);


//			<td class="key"
//			          data-snapfield="vol_and_avg">Vol / Avg.
//			</td>
//			<td class="val">1.75M/2.25M
//          or
//			<td class="val">0.00
			// 1.75M  762,118.00 0.00 
			add(Field.VOL,
					"data-snapfield=\"vol_and_avg\">.+?<td class=\"val\">([0-9,KMB\\.]+)",
					"data-snapfield=\"vol_and_avg\">" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);
			add(Field.AVG_VOL,
					"data-snapfield=\"vol_and_avg\">.+?<td class=\"val\">.*?/([0-9,KMB\\.]+)\\p{javaWhitespace}",
					"data-snapfield=\"vol_and_avg\">Vol / Avg",
					Pattern.DOTALL, Scrape.Type.INTEGER);

//			<td class="key"
//			          data-snapfield="market_cap">Mkt cap
//			</td>
//			<td class="val">&nbsp;&nbsp;&nbsp;&nbsp;-
			add(Field.MKT_CAP,
					"data-snapfield=\"market_cap\">.+?<td class=\"val\">(.+?)\\p{javaWhitespace}",
					"data-snapfield=\"market_cap\">Mkt cap" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);
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
