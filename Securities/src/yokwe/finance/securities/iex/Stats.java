package yokwe.finance.securities.iex;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.HttpUtil;

public class Stats extends IEXBase {
	public static final String TYPE = "stats";

	public String companyName;
	public double marketcap;          // is not calculated in real time.
	public double beta;
	public double week52high;
	public double week52low;
	public double week52change;
	public long   shortInterest;
	public String shortDate;
	public double dividendRate;
	public double dividendYield;
	public String exDividendDate;
	public double latestEPS;          // (Most recent quarter)
	public String latestEPSDate;
	public long   sharesOutstanding;
	@JSONName("float")
	public long   floating;
	public double returnOnEquity;     // (Trailing twelve months)
	public double consensusEPS;       // (Most recent quarter)
	public double numberOfEstimates;  // (Most recent quarter)
	public String symbol;
	@JSONName("EBITDA")
	public long   ebitda;             // (Trailing twelve months)
	public long   revenue;            // (Trailing twelve months)
	public long   grossProfit;        // (Trailing twelve months)
	public long   cash;               // reers to total cash. (Trailing twelve months)
	public long   debt;               // refers to total debt. (Trailing twelve months)
	public double ttmEPS;             // (Trailing twelve months)
	public double revenuePerShare;    // (Trailing twelve months)
	public double revenuePerEmployee; // (Trailing twelve months)
	public double peRatioHigh;
	public double peRatioLow;
	public double EPSSurpriseDollar;  // refers to the difference between actual EPS and consensus EPS in dollars.
	public double EPSSurprisePercent; // refers to the percent difference between actual EPS and consensus EPS.
	public double returnOnAssets;     // (Trailing twelve months)
	public double returnOnCapital;    // (Trailing twelve months)
	public double profitMargin;
	public double priceToSales;
	public double priceToBook;
	public double day200MovingAvg;
	public double day50MovingAvg;
	public double institutionPercent; // represents top 15 institutions
	public double insiderPercent;
	public double shortRatio;
	public double year5ChangePercent;
	public double year2ChangePercent;
	public double year1ChangePercent;
	public double ytdChangePercent;
	public double month6ChangePercent;
	public double month3ChangePercent;
	public double month1ChangePercent;
	public double day5ChangePercent;
	public double day30ChangePercent;
	
	Stats() {
		companyName         = null;
		marketcap           = 0;
		beta                = 0;
		week52high          = 0;
		week52low           = 0;
		week52change        = 0;
		shortInterest       = 0;
		shortDate           = null;
		dividendRate        = 0;
		dividendYield       = 0;
		exDividendDate      = null;
		latestEPS           = 0;
		latestEPSDate       = null;
		sharesOutstanding   = 0;
		floating            = 0;
		returnOnEquity      = 0;
		consensusEPS        = 0;
		numberOfEstimates   = 0;
		symbol              = null;
		ebitda              = 0;
		revenue             = 0;
		grossProfit         = 0;
		cash                = 0;
		debt                = 0;
		ttmEPS              = 0;
		revenuePerShare     = 0;
		revenuePerEmployee  = 0;
		peRatioHigh         = 0;
		peRatioLow          = 0;
		EPSSurpriseDollar   = 0;
		EPSSurprisePercent  = 0;
		returnOnAssets      = 0;
		returnOnCapital     = 0;
		profitMargin        = 0;
		priceToSales        = 0;
		priceToBook         = 0;
		day200MovingAvg     = 0;
		day50MovingAvg      = 0;
		institutionPercent  = 0;
		insiderPercent      = 0;
		shortRatio          = 0;
		year5ChangePercent  = 0;
		year2ChangePercent  = 0;
		year1ChangePercent  = 0;
		ytdChangePercent    = 0;
		month6ChangePercent = 0;
		month3ChangePercent = 0;
		month1ChangePercent = 0;
		day5ChangePercent   = 0;
		day30ChangePercent  = 0;
	}
	Stats(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Stats getStock(String symbol) {
		String url = String.format("%s/stock/%s/%s", END_POINT, symbol, TYPE);
		String jsonString = HttpUtil.downloadAsString(url);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			return new Stats(jsonObject);
		}
	}

	static void test(Logger logger) {
		String jsonString = "{\"companyName\":\"International Business Machines Corporation\",\"marketcap\":132521950000,\"beta\":0.906157,\"week52high\":171.13,\"week52low\":137.45,\"week52change\":4.2732,\"shortInterest\":15164536,\"shortDate\":\"2018-07-13\",\"dividendRate\":6.28,\"dividendYield\":4.3265586,\"exDividendDate\":\"2018-05-09 00:00:00.0\",\"latestEPS\":6.15,\"latestEPSDate\":\"2017-12-31\",\"sharesOutstanding\":913000000,\"float\":911022243,\"returnOnEquity\":31.16,\"consensusEPS\":3.03,\"numberOfEstimates\":8,\"EPSSurpriseDollar\":null,\"EPSSurprisePercent\":1.6502,\"symbol\":\"IBM\",\"EBITDA\":10192000000,\"revenue\":41696000000,\"grossProfit\":19662000000,\"cash\":11515000000,\"debt\":45626000000,\"ttmEPS\":14.010000000000002,\"revenuePerShare\":46,\"revenuePerEmployee\":104816,\"peRatioHigh\":15,\"peRatioLow\":8.7,\"returnOnAssets\":4.75,\"returnOnCapital\":null,\"profitMargin\":7.13,\"priceToSales\":1.6588593,\"priceToBook\":7.3,\"day200MovingAvg\":149.46599,\"day50MovingAvg\":143.9004,\"institutionPercent\":59.4,\"insiderPercent\":0.2,\"shortRatio\":3.5121515,\"year5ChangePercent\":-0.12947989497383333,\"year2ChangePercent\":-0.024449585248252872,\"year1ChangePercent\":0.045615187677660936,\"ytdChangePercent\":-0.039181407713423766,\"month6ChangePercent\":-0.11147322819558969,\"month3ChangePercent\":0.012426021057624193,\"month1ChangePercent\":0.0390121689334289,\"day5ChangePercent\":-0.0037748798901851956,\"day30ChangePercent\":-0.0016507325125523125}";
		logger.info("json {}", jsonString);
		
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonObject jsonObject = reader.readObject();
			Stats stats = new Stats(jsonObject);
			logger.info("stats {}", stats.toString());
		}
	}
	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(Stats.class);
		logger.info("START");
		
		test(logger);
		
		{
			Stats stats = Stats.getStock("ibm");
			logger.info("stats {}", stats.toString());
		}

		logger.info("STOP");
	}
}
