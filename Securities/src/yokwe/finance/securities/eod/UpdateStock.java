package yokwe.finance.securities.eod;

import org.slf4j.LoggerFactory;

public class UpdateStock {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(UpdateStock.class);
	
	static class NasdaqTraded {
		public String traded;
		public String actSymbol;
		public String name;
		public String exch;
		public String category;
		public String etf;
		public String lotSize;
		public String test;
		public String status;
		public String cqsSymbol;
		public String symbol;
	}
	static class CompaniesByRegion {
		public String nasdaq ;
		public String name;
		public String lastSale;
		public String marketCap;
		public String adrTSO;
		public String country;
		public String ipoYear;
		public String sector;
		public String industry;
		public String url;
	}

	public static void main(String[] args) {
		logger.info("START");
		logger.info("STOP");
	}
}
