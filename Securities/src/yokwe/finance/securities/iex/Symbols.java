package yokwe.finance.securities.iex;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.util.HttpUtil;

public class Symbols extends IEXBase {
	String  symbol;    // refers to the symbol represented in Nasdaq Integrated symbology (INET).
	String  name;      // refers to the name of the company or security.
	String  date;      // refers to the date the symbol reference data was generated.
	boolean isEnabled; // will be true if the symbol is enabled for trading on IEX.
	String  type;      // refers to the common issue type (AD - ADR
	                   // RE - REIT
				       // CE - Closed end fund
				       // SI - Secondary Issue
				       // LP - Limited Partnerships
				       // CS - Common Stock
				       // ET - ETF)
	String  iexId;     // unique ID applied by IEX to track securities through symbol changes.

	Symbols() {
		symbol    = null;
		name      = null;
		date      = null;
		isEnabled = false;
		type      = null;
		iexId     = null;
	}
	Symbols(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	public static Symbols[] getSymbols() {
		String url = String.format("%s/ref-data/symbols", END_POINT);
		String jsonString = HttpUtil.downloadAsString(url);

		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonArray jsonArray = reader.readArray();
			int jsonArraySize = jsonArray.size();
			
			Symbols[] ret = new Symbols[jsonArraySize];
			for(int i = 0; i < jsonArraySize; i++) {
				JsonObject element = jsonArray.getJsonObject(i);
				ret[i] = new Symbols(element);
			}
			
			return ret;
		}
	}

	static void test(Logger logger) {
		String jsonString = "[{\"symbol\":\"VENUSDT\",\"name\":\"VeChain USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000015},{\"symbol\":\"XLMUSDT\",\"name\":\"Stellar Lumens USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000016},{\"symbol\":\"QTUMUSDT\",\"name\":\"Qtum USD\",\"date\":\"2018-07-27\",\"isEnabled\":true,\"type\":\"crypto\",\"iexId\":10000017}]";
		logger.info("json {}", jsonString);
		
		try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
			JsonArray jsonArray = reader.readArray();
			int jsonArraySize = jsonArray.size();
			logger.info("jsonArraySize = {}", jsonArraySize);
			
			List<Symbols> result = new ArrayList<>();
			for(int i = 0; i < jsonArraySize; i++) {
				JsonObject element = jsonArray.getJsonObject(i);
				Symbols chart = new Symbols(element);
				result.add(chart);
			}

			logger.info("result ({}){}", result.size(), result.toString());
		}
	}

	public static void main(String[] args) {
		Logger logger = LoggerFactory.getLogger(Chart.class);
		logger.info("START");
		
		test(logger);
		
		{
			Symbols[] symbols = Symbols.getSymbols();
			logger.info("symbols {}", symbols.length);
//			logger.info("symbols {}", Arrays.asList(symbols).toString());
		}

		logger.info("STOP");
	}

	
}
