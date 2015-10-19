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

public class YahooFinance {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooFinance.class);
	
	private static final String CRLF = "\r\n";

	public enum Field {
		EXCHANGE, SYMBOL, PRICE, AVG_VOL, SHARES, MKT_CAP, NAME,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<span class="rtq_exch"><span class="rtq_dash">-</span>NYSE  </span><span class="wl_sign">
			add(Field.EXCHANGE,
				"<span class=\"rtq_exch\">.+?</span>(.+?)</span>",
				"<span class=\"rtq_exch\">");

//			<div class="title"><h2>International Business Machines Corporation (IBM)</h2>
			add(Field.SYMBOL,
					"<div class=\"title\"><h2>.+?\\((.+?)\\)</h2>",
					"<div class=\"title\"><h2>",
					Pattern.DOTALL);

//			<span class="time_rtq_ticker"><span id="yfs_l84_ibm">150.39</span>
			add(Field.PRICE,
					"<span class=\"time_rtq_ticker\"><span .+?>(.+?)</span>",
					"<span class=\"time_rtq_ticker\">");

//			Avg Vol <span class="small">(3m)</span>:</th><td class="yfnc_tabledata1">4,248,670</td>
			add(Field.AVG_VOL,
					"Avg Vol <span .+?>.+?<td .+?>(.+?)</td>",
					"Avg Vol <span ");

//          NO MATCH
			add(Field.SHARES,
					"data-snapfield=\"shares\">.+?<td class=\"val\">(.+?)\\p{javaWhitespace}",
					"data-snapfield=\"shares\">");

//			Market Cap:</th><td class="yfnc_tabledata1"><span id="yfs_j10_ibm">147.31B</span>
			add(Field.MKT_CAP,
					"Market Cap:</th>.+?<span .+?>(.+?)</span>",
					"Market Cap:</th>");

//			<div class="title"><h2>International Business Machines Corporation (IBM)</h2>
			add(Field.NAME,
					"<div class=\"title\"><h2>(.+?)\\(.+?\\)</h2>",
					"<div class=\"title\"><h2>",
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
