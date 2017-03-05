package yokwe.finance.securities.eod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Field;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.util.FileUtil;

public class UpdateForex {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStats.class);
	
	public static final String ENCODING_MIZUHO  = "SHIFT_JIS";
	public static final String PATH_MIZUHO      = "tmp/fetch/mizuho/quote.csv";
	public static final String PATH_FOREX       = "tmp/eod/forex.csv";
	
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
		
		String contents = FileUtil.read(new File(PATH_MIZUHO), ENCODING_MIZUHO);

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
						throw new SecuritiesException();
					}
					currencyIndex[i] = index;
					logger.info("{}", String.format("%s  %2d", currency, index));
				}
			}
			String lastDate = "????";
			for(;;) {
				String line = in.readLine();
				if (line == null) break;
				
				String[] token = line.split("[,/]");
				int y = Integer.parseInt(token[0]);
				int m = Integer.parseInt(token[1]);
				int d = Integer.parseInt(token[2]);
				
				if (y < 2015) continue;
				
				lastDate = String.format("%4d-%02d-%02d", y, m, d);
				out.append(lastDate);
				for(int i = 0; i < currencyIndex.length; i++) {
					double value = Double.parseDouble(token[2 + currencyIndex[i]]);
					out.format(",%.2f", value);
				}
				out.println();
				count++;
			}
			
			logger.info("lastDate {}", lastDate);
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
		
		// Sanity check
		logger.info("count    {}", count);
		
		int forexCount = Forex.load().size();
		logger.info("forex    {}", forexCount);
		if (forexCount != count) {
			logger.error("count({}) != forexCount({})", count, forexCount);
			throw new SecuritiesException("Unexpected");
		}

		logger.info("STOP");
	}
}
