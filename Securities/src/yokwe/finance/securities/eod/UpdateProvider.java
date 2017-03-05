package yokwe.finance.securities.eod;

import java.io.File;
import java.time.LocalDate;

public interface UpdateProvider {
	public static final String GOOGLE = "google";
	public static final String YAHOO  = "yahoo";

	public String  getName();
	public File    getFile(String symbol);
	public boolean updateFile(String exch, String symbol, LocalDate dateFirst, LocalDate dateLast);
}