package yokwe.finance.securities.update;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.NasdaqUtil;
import yokwe.finance.securities.util.Scrape;

public class YahooFinance {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(YahooFinance.class);
	
	private static final String NEWLINE = "\n";

	public enum Field {
		EXCHANGE, SYMBOL, PRICE, AVG_VOL, SHARES, MKT_CAP, NAME,
	}
	
	public static class ScrapeGoogleFinance extends Scrape<Field> {
		public void init() {
//			<span class="rtq_exch"><span class="rtq_dash">-</span>NYSE  </span><span class="wl_sign">
			add(Field.EXCHANGE,
				"<span class=\"rtq_exch\">.+?</span>(.+?)</span>",
				"<span class=\"rtq_exch\">");

//			<title>VCLT: Summary for Vanguard Long-Term Corporate Bo- Yahoo! Finance</title>
			add(Field.SYMBOL,
					"<title>(.+?):",
					"<title>");

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

//			<meta property="og:title" content="Vanguard Long-Term Corporate Bond ETF">
//			<meta property="og:title" content='Etablissements Delhaize Frères et Cie "Le Lion" (Groupe Delhaize) SA'>
			add(Field.NAME,
					"<meta property=\"og:title\" content=[\"\'](.+?)[\"\']>",
					"<meta property=\"og:title\" content=",
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
					// Use symbol from file name
					if (field.equals(Field.SYMBOL)) {
						value = symbol;
					}
					// Use exchange of nasdaq
					if (field.equals(Field.EXCHANGE)) {
						value = NasdaqUtil.get(symbol).exchange;
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
