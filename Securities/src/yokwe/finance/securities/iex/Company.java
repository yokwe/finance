package yokwe.finance.securities.iex;

import java.util.Map;

import javax.json.JsonObject;

public class Company extends IEXBase {
	public static final String TYPE = "company";

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
