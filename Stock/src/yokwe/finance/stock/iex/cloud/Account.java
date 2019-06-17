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
			// "ACCOUNT_USAGE":"0","IEX_STATS":"0","REF_DATA_IEX_SYMBOLS":"0","IEX_TOPS":"0","IEX_DEEP":"0"
			@JSONName("ACCOUNT_USAGE")
			public String accountUsage;
			@JSONName("IEX_STATS")
			public String iexStats;
			@JSONName("REF_DATA_IEX_SYMBOLS")
			public String refDataIEXSymbols;
			@JSONName("IEX_TOPS")
			public String iexTops;
			@JSONName("IEX_DEEP")
			public String iexDeep;
			
			public KeyUsage() {
				accountUsage      = null;
				iexStats          = null;
				refDataIEXSymbols = null;
				iexTops           = null;
				iexDeep           = null;
			}

			public KeyUsage(JsonObject jsonObject) {
				super(jsonObject);
			}
		}
		public static class Messages extends Base {
			//public Map<String, String> dailyUsage;
			public long monthlyUsage;
			public long monthlyPayAsYouGo;
			// tokenUsage
			public KeyUsage keyUsage;
			
			public Messages() {
				monthlyUsage      = 0;
				monthlyPayAsYouGo = 0;
				keyUsage          = new KeyUsage();
			}
			
			public Messages(JsonObject jsonObject) {
				super(jsonObject);
			}
		}
		
		// {"messages":{"dailyUsage":{"20190616":"0","20190617":"0"},"monthlyUsage":0,"monthlyPayAsYouGo":0,"tokenUsage":{},"keyUsage":{"ACCOUNT_USAGE":"0","IEX_STATS":"0","REF_DATA_IEX_SYMBOLS":"0","IEX_TOPS":"0","IEX_DEEP":"0"}},"rules":[]}
		public Messages messages;
//		public Rules    rules;
		
		public Usage() {
			messages = new Messages();
		}
		public Usage(JsonObject jsonObject) {
			super(jsonObject);
		}
	}
}
