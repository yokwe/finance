package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;

public interface UpdateProvider {
	public static final LocalDate DATE_FIRST    = LocalDate.of(2015, 11, 1);
	public static final LocalDate DATE_LAST     = Market.getLastTradingDate();
	
	public static final int       MAX_RETRY     = 3;  // try 3 times at least
	
	public static final String    GOOGLE        = "google";
	public static final String    YAHOO         = "yahoo";

	public String  getName();
	public File    getFile(String symbol);
	public boolean updateFile(String exch, String symbol);
}
