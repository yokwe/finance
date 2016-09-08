package yokwe.finance.securities.book;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

@SheetData.SheetName("mizuho-header")
public class Mizuho extends SheetData implements Comparable<Mizuho> {
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Mizuho.class);
	
	public static class Map {
		private List<String> dateList = new ArrayList<>();
		private java.util.Map<String, Mizuho> map = new HashMap<>();
		
		public Map(String url) {
			try (LibreOffice libreOffice = new LibreOffice(url, true)) {
				for(Mizuho mizuho: SheetData.getInstance(libreOffice, Mizuho.class)) {
					String key = mizuho.date;
					map.put(key, mizuho);
					dateList.add(key);
				}
				dateList.sort((String o1, String o2) -> o1.compareTo(o2));
			}
			logger.info("Mizuho.Map {} => {}", dateList.get(0), dateList.get(dateList.size() - 1));
		}
		
		private String getDate(String date) {
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
		
		public Mizuho get(String date) {
			return map.get(getDate(date));
		}
	}

	
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
	
	
	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		Mizuho.Map map = new Mizuho.Map(url);

		{
			String date1 = "2016-09-03";
			Mizuho mizuho = map.get(date1);
			logger.info("date {} => {}", date1, mizuho);
		}

		{
			String date1 = "2016-09-05";
			Mizuho mizuho = map.get(date1);
			logger.info("date {} => {}", date1, mizuho);
		}

		{
			String date1 = "2016-09-10";
			Mizuho mizuho = map.get(date1);
			logger.info("date {} => {}", date1, mizuho);
		}

		logger.info("STOP");
		System.exit(0);
	}
}
