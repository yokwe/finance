package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;

public interface UpdateProvider {
	public static final LocalDate DATE_LAST     = Market.getLastTradingDate();
	public static final LocalDate DATE_FIRST    = DATE_LAST.minusYears(1);
	
	public static final int       MAX_RETRY     = 3;  // try 3 times at least
	
	public static final String    GOOGLE        = "google";
	public static final String    YAHOO         = "yahoo";

	public String  getName();
	public File    getFile(String symbol);
	public boolean updateFile(String exch, String symbol, boolean newFile, LocalDate dateFirst, LocalDate dateLast);
	
	public default boolean updateFile(String exch, String symbol, boolean newFile) {
		return updateFile(exch, symbol, newFile, DATE_FIRST, DATE_LAST);
	}
}
