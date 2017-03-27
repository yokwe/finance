package yokwe.finance.securities.eod;

import java.io.File;
import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class Delisted {
	public String exchange;
	public String symbol;
	
	@Override
	public String toString() {
		return String.format("[%s %s]", exchange, symbol);
	}
	
	public static File getFile(String symbol) {
		String path = String.format("%s/%s.csv", DelistedUtil.PATH_DIR, symbol);
		return new File(path);
	}

	public static List<Delisted> load() {
		return CSVUtil.loadWithHeader(DelistedUtil.PATH_CSV, Delisted.class);
	}
}
