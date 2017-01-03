package yokwe.finance.securities.tax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.libreoffice.Sheet;
import yokwe.finance.securities.libreoffice.SpreadSheet;

@Sheet.SheetName("mizuho-header")
@Sheet.HeaderRow(0)
@Sheet.DataRow(1)
public class Mizuho extends Sheet implements Comparable<Mizuho> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Mizuho.class);

	@ColumnName("DATE")
	public String date;
	@ColumnName("USD")
	public double usd;
	@ColumnName("GBP")
	public double gbp;
	@ColumnName("EUR")
	public double eur;
	@ColumnName("AUD")
	public double aud;
	@ColumnName("NZD")
	public double nzd;
	
	@Override
	public String toString() {
		return String.format("%s %6.2f %6.2f %6.2f %6.2f %6.2f", date, usd, gbp, eur, aud, nzd);
	}
	
	@Override
	public int compareTo(Mizuho that) {
		return this.date.compareTo(that.date);
	}
	
	private static final String URL_MIZUHO = "file:///home/hasegawa/Dropbox/Trade/mizuho-header.csv";

	private static List<String> dateList = new ArrayList<>();
	// key is date
	private static Map<String, Mizuho> mizuhoMap = new TreeMap<>();
	
	static {
		logger.info("Start load {}", URL_MIZUHO);
		try (SpreadSheet spreadSheet = new SpreadSheet(URL_MIZUHO, true)) {
			for(Mizuho mizuho: Sheet.getInstance(spreadSheet, Mizuho.class)) {
				dateList.add(mizuho.date);
				mizuhoMap.put(mizuho.date, mizuho);
			}
		}
		logger.info("mizuhoMap {}", mizuhoMap.size());
		// sort dateList
		Collections.sort(dateList);
	}
	
	private static boolean testMode = false;
	public static void enableTestMode() {
		testMode = true;
		logger.info("enableTestMode {}", testMode);
	}
	
	public static String getValidDate(String date) {
		int index = Collections.binarySearch(dateList, date);
		if (index < 0) {
			index = - (index + 1) - 1;
			if (index < 0) {
				logger.info("Unexpected date = {}", date);
				throw new SecuritiesException("Unexpected");
			}
		}
		return dateList.get(index);
	}
	
	public static Mizuho get(String date) {
		String validDate = getValidDate(date);
		Mizuho mizuho = mizuhoMap.get(validDate);
		return mizuho;
	}
	
	public static double getUSD(String date) {
		if (testMode) return 1;
		
		Mizuho mizuho = get(date);
		return mizuho.usd;
	}
}
