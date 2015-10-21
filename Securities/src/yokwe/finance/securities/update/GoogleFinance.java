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
	
	private static final String CRLF = "\r\n";

	public enum Field {
		EXCHANGE, SYMBOL, PRICE, AVG_VOL, SHARES, MKT_CAP, NAME,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<meta itemprop="exchange"
//	        content="NYSE" />
			add(Field.EXCHANGE,
				"<meta itemprop=\"exchange\"\\p{javaWhitespace}+content=\"(.+?)\" />",
				"<meta itemprop=\"exchange\"",
				Pattern.DOTALL);

//			<meta itemprop="tickerSymbol"
//			        content="A" />
			// TODO  This symbol use google format. Should I convert to commons symbol format?
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
			add(Field.AVG_VOL,
					"data-snapfield=\"vol_and_avg\">.+?<td class=\"val\">.*?([0-9,KMB\\.]+)\\p{javaWhitespace}",
					"data-snapfield=\"vol_and_avg\">" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);

//			<td class="key"
//			          data-snapfield="shares">Shares
//			</td>
//			<td class="val">979.53M
//			or
//          <td class="val">&nbsp;&nbsp;&nbsp;&nbsp;-
			add(Field.SHARES,
					"data-snapfield=\"shares\">.+?<td class=\"val\">(.+?)\\p{javaWhitespace}",
					"data-snapfield=\"shares\">" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);

//			<td class="key"
//			          data-snapfield="market_cap">Mkt cap
//			</td>
//			<td class="val">&nbsp;&nbsp;&nbsp;&nbsp;-
			add(Field.MKT_CAP,
					"data-snapfield=\"market_cap\">.+?<td class=\"val\">(.+?)\\p{javaWhitespace}",
					"data-snapfield=\"market_cap\">Mkt cap" ,
					Pattern.DOTALL, Scrape.Type.INTEGER);

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
					if (field.equals(Field.SYMBOL)) value = symbol;
					
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
