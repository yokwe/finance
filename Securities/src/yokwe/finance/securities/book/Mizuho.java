package yokwe.finance.securities.book;

import java.util.List;

import org.slf4j.LoggerFactory;

@SheetData.SheetName("mizuho-header")
public class Mizuho extends SheetData {	
	static final org.slf4j.Logger logger = LoggerFactory.getLogger(Mizuho.class);
	
	@ColumnName("DATE")
	String date;
	@ColumnName("USD")
	double usd;
	@ColumnName("GBP")
	double gbp;
	@ColumnName("EUR")
	double eur;
	@ColumnName("AUD")
	double aud;
	@ColumnName("NZD")
	double nzd;
	
	public String toString() {
		return String.format("%s %6.2f %6.2f %6.2f %6.2f %6.2f", date, usd, gbp, eur, aud, nzd);
	}

	public static void main(String[] args) {
		String url = "file:///home/hasegawa/Dropbox/Trade/投資損益計算_2016_SAVE.ods";
		
		logger.info("START");
		try (LibreOffice libreOffice = new LibreOffice(url)) {			
			List<Mizuho> mizuhoList = SheetData.getInstance(libreOffice, Mizuho.class);
			for(Mizuho mizuho: mizuhoList) {
				logger.info("{}", mizuho);
			}
		}
		logger.info("STOP");
		System.exit(0);
	}
}
