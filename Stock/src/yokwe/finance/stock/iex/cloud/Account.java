package yokwe.finance.stock.iex.cloud;

import java.time.LocalDateTime;
import java.util.Map;

import javax.json.JsonObject;

import yokwe.finance.stock.iex.cloud.IEXCloud.JSONName;

public class Account {
	public static class Metadata extends Base {
		public static final String METHOD = "/account/metadata";

		// {"payAsYouGoEnabled":false,"effectiveDate":1551225868000,"subscriptionTermType":"annual","tierName":"start","messageLimit":500000,"messagesUsed":0,"circuitBreaker":null}
		public boolean       payAsYouGoEnabled;
		public LocalDateTime effectiveDate;
		public String        subscriptionTermType;
		public String        tierName;
		public long          messageLimit;
		public long          messagesUsed;
		public long          circuitBreaker;
		
		public Metadata() {
			payAsYouGoEnabled    = false;
			effectiveDate        = null;
			subscriptionTermType = null;
			tierName             = null;
			messageLimit         = 0;
			messagesUsed         = 0;
			circuitBreaker       = 0;
		}

		public Metadata(JsonObject jsonObject) {
			super(jsonObject);
		}
	}
	
	public static class Usage extends Base {
		public static final String METHOD = "/account/usage";

		public static class KeyUsage extends Base {
			// "ACCOUNT_USAGE":"0","IEX_STATS":"0","REF_DATA_IEX_SYMBOLS":"0","IEX_TOPS":"0","IEX_DEEP":"0","REF_DATA":"100"
			@JSONName("ACCOUNT_USAGE")
			public long accountUsage;
			@JSONName("IEX_STATS")
			public long iexStats;
			@JSONName("REF_DATA_IEX_SYMBOLS")
			public long refDataIEXSymbols;
			@JSONName("IEX_TOPS")
			public long iexTops;
			@JSONName("IEX_DEEP")
			public long iexDeep;
			@JSONName("REF_DATA")
			public long refData;
			
			public KeyUsage() {
				accountUsage      = 0;
				iexStats          = 0;
				refDataIEXSymbols = 0;
				iexTops           = 0;
				iexDeep           = 0;
				refData           = 0;
			}

			public KeyUsage(JsonObject jsonObject) {
				super(jsonObject);
			}
		}
		
		public static class Messages extends Base {
			public Map<String, Long> dailyUsage;
			public long              monthlyUsage;
			public long              monthlyPayAsYouGo;
			// tokenUsage
			public KeyUsage          keyUsage;
			
			public Messages() {
				dailyUsage        = null;
				monthlyUsage      = 0;
				monthlyPayAsYouGo = 0;
				keyUsage          = null;
			}
			
			public Messages(JsonObject jsonObject) {
				super(jsonObject);
			}
		}
		
		// {"messages":{"dailyUsage":{"20190616":"0","20190617":"0"},"monthlyUsage":0,"monthlyPayAsYouGo":0,"tokenUsage":{},"keyUsage":{"ACCOUNT_USAGE":"0","IEX_STATS":"0","REF_DATA_IEX_SYMBOLS":"0","IEX_TOPS":"0","IEX_DEEP":"0"}},"rules":[]}
		public Messages messages;
//		public Rules    rules;
		
		public Usage() {
			messages = null;
		}
		public Usage(JsonObject jsonObject) {
			super(jsonObject);
		}
	}
}
