package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;

import yokwe.finance.securities.util.Pause;

public interface UpdateProvider {
	public static final LocalDate DATE_LAST     = Market.getLastTradingDate();
	public static final LocalDate DATE_FIRST    = DATE_LAST.minusYears(1);
	
	public static final int       MAX_RETRY     = 3;  // try 3 times at least
	
	public static final int       GRACE_HOURS   = 12; // Don't update file that lastModified is within GRACE_HOURS
	public static final long      GRACE_PERIOD  = System.currentTimeMillis() - (1000 * 60 * 60 * GRACE_HOURS);
	
	public static final String    GOOGLE        = "google";
	public static final String    YAHOO         = "yahoo";
	public static final String    IEX           = "iex";

	public String  getName();
	public File    getFile(String symbol);
	public boolean updateFile(String exch, String symbol, String symbolURL, boolean newFile, LocalDate dateFirst, LocalDate dateLast);
	
	public boolean updateFile(String symbol, boolean newFile, LocalDate dateFirst, LocalDate dateLast);
	
	public default boolean updateFile(String symbol, boolean newFile) {
		return updateFile(symbol, newFile, DATE_FIRST, DATE_LAST);
	}
	
	public Pause getPause();
}
