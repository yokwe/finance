package yokwe.finance.securities.iex;

import java.util.Map;

import javax.json.JsonObject;

public class Company extends IEXBase implements Comparable<Company> {
	public static final String TYPE = "company";
	
	public static class CSV implements Comparable<CSV> {
		private static final String CHAR_SPLIT_JOIN = ",";
		
		public String symbol;
		public String companyName;
		public String exchange;
		public String industry;
		public String website;
		public String description;
		public String CEO;
		public String issueType;
		public String sector;
		public String tags;

		CSV() {
			this.symbol      = null;
			this.companyName = null;
			this.exchange    = null;
			this.industry    = null;
			this.website     = null;
			this.description = null;
			this.CEO         = null;
			this.issueType   = null;
			this.sector      = null;
			this.tags        = null;
		}
		CSV(Company company) {
			this.symbol      = company.symbol;
			this.companyName = company.companyName;
			this.exchange    = company.exchange;
			this.industry    = company.industry;
			this.website     = company.website;
			this.description = company.description;
			this.CEO         = company.CEO;
			this.issueType   = company.issueType;
			this.sector      = company.sector;
			this.tags        = String.join(CHAR_SPLIT_JOIN, company.tags);
		}
		
		Company toCompany() {
			Company company = new Company();
			
			company.symbol      = this.symbol;
			company.companyName = this.companyName;
			company.exchange    = this.exchange;
			company.industry    = this.industry;
			company.website     = this.website;
			company.description = this.description;
			company.CEO         = this.CEO;
			company.tags        = this.tags.split(CHAR_SPLIT_JOIN);

			return company;
		}
		
		@Override
		public int compareTo(CSV that) {
			return this.symbol.compareTo(that.symbol);
		}
	}

	public String symbol;
	public String companyName;
	public String exchange;
	public String industry;
	public String website;
	public String description;
	public String CEO;
	public String issueType;
	public String sector;
	public String[] tags;
	
	/*	issueTYpe refers to the common issue type of the stock.
	ad – American Depository Receipt (ADR’s)
	re – Real Estate Investment Trust (REIT’s)
	ce – Closed end fund (Stock and Bond Fund)
	si – Secondary Issue
	lp – Limited Partnerships
	cs – Common Stock
	et – Exchange Traded Fund (ETF)
	(blank) = Not Available, i.e., Warrant, Note, or (non-filing) Closed Ended Funds
	 */	

	public Company() {
		symbol      = null;
		companyName = null;
		exchange    = null;
		industry    = null;
		website     = null;
		description = null;
		CEO         = null;
		issueType   = null;
		sector      = null;
	}
	public Company(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	@Override
	public int compareTo(Company that) {
		return this.symbol.compareTo(that.symbol);
	}

	public static Map<String, Company> getStock(String... symbols) {
		return IEXBase.getStockObject(Company.class, symbols);
	}
	
//	public static Company getStock(String symbol) {
//		String url = String.format("%s/stock/%s/%s", END_POINT, symbol, TYPE);
//		String jsonString = HttpUtil.downloadAsString(url);
//
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonObject jsonObject = reader.readObject();
//			return new Company(jsonObject);
//		}
//	}
//
//	static void test(Logger logger) {
//		String jsonString = "{\"symbol\":\"IBM\",\"companyName\":\"International Business Machines Corporation\",\"exchange\":\"New York Stock Exchange\",\"industry\":\"Application Software\",\"website\":\"http://www.ibm.com\",\"description\":\"International Business Machines Corp offers a variety of IT services along with software, and hardware. It has operations in over 170 countries and provides planning, build, manage, and maintain IT infrastructure, platforms, applications, and services.\",\"CEO\":\"Virginia M. Rometty\",\"issueType\":\"cs\",\"sector\":\"Technology\",\"tags\":[\"Technology\",\"Information Technology Services\",\"Application Software\"]}";
//		logger.info("json {}", jsonString);
//		
//		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
//			JsonObject jsonObject = reader.readObject();
//			Company Company = new Company(jsonObject);
//			logger.info("Company {}", Company.toString());
//		}
//	}
//	public static void main(String[] args) {
//		Logger logger = LoggerFactory.getLogger(Company.class);
//		logger.info("START");
//		
//		test(logger);
//		
//		{
//			Company company = Company.getStock("ibm");
//			logger.info("Company {}", company.toString());
//		}
//
//		logger.info("STOP");
//	}
}
