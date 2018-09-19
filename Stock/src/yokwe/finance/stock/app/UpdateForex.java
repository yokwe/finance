package yokwe.finance.stock.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.data.Forex;
import yokwe.finance.stock.util.HttpUtil;

public class UpdateForex {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateForex.class);
	
	public static final String URL_MIZUHO       = "https://www.mizuhobank.co.jp/market/csv/quote.csv";
	public static final String ENCODING_MIZUHO  = "SHIFT_JIS";
	
	public static final String PATH_FOREX       = "tmp/data/forex.csv";
	
	public static List<Forex> load() {
		return CSVUtil.loadWithHeader(PATH_FOREX, Forex.class);
	}
	
	public static void main (String[] args) {
		logger.info("START");
		
		String[] currencyList;
		int[]    currencyIndex;
		{
			// Build currencyList from double field of Forex class.
			int size = 0;
			Field[] fields = Forex.class.getDeclaredFields();
			
			for(int i = 0; i < fields.length; i++) {
				if (fields[i].getType().getName().equals("double")) size++;
			}
			currencyList  = new String[size];
			currencyIndex = new int[size];
			
			int j = 0;
			for(int i = 0; i < fields.length; i++) {
				if (fields[i].getType().getName().equals("double")) {
					currencyList[j++] = fields[i].getName().toUpperCase();
				}
			}
		}
		
		String contents = HttpUtil.downloadAsString(URL_MIZUHO, ENCODING_MIZUHO);

		int count = 0;

		try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(PATH_FOREX)));
			BufferedReader in = new BufferedReader(new StringReader(contents))) {
			
			out.append("date");
			for(String currency: currencyList) {
				out.append(",").append(currency.toLowerCase());
			}
			out.println();

			in.readLine(); // Skip first line
			in.readLine(); // Skip second line
			String header = in.readLine();
			{
				// build currencyIndex
				String[] token = header.split(",");
				for(int i = 0; i < currencyList.length; i++) {
					String currency = currencyList[i];
					int index = -1;
					for(int j = 0; j < token.length; j++) {
						if (token[j].equals(currency)) {
							index = j;
							break;
						}
					}
					if (index == -1) {
						logger.error("Unknown currency = {}", currency);
						throw new UnexpectedException();
					}
					currencyIndex[i] = index;
					logger.info("{}", String.format("%s  %2d", currency, index));
				}
			}
			StringBuilder lineOut = new StringBuilder();
			for(;;) {
				String lineIn = in.readLine();
				if (lineIn == null) break;
				
				String[] token = lineIn.split("[,/]");
				int y = Integer.parseInt(token[0]);
				int m = Integer.parseInt(token[1]);
				int d = Integer.parseInt(token[2]);
				
				if (y < 2015) continue;
				
				// clear lineOut
				lineOut.setLength(0);
				lineOut.append(String.format("%4d-%02d-%02d", y, m, d));
				
				for(int i = 0; i < currencyIndex.length; i++) {
					double value = Double.parseDouble(token[2 + currencyIndex[i]]);
					lineOut.append(String.format(",%.2f", value));
				}
				
				out.println(lineOut.toString());
				logger.info("{}", lineOut);
				count++;
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new UnexpectedException();
		}
		
		// Sanity check
		logger.info("URL    = {}", URL_MIZUHO);
		logger.info("OUTPUT = {}", PATH_FOREX);
		logger.info("COUNT  = {}", count);
		
		int forexCount = load().size();
		logger.info("FOREX  = {}", forexCount);
		if (forexCount != count) {
			logger.error("count({}) != forexCount({})", count, forexCount);
			throw new UnexpectedException("Unexpected");
		}

		logger.info("STOP");
	}
}
