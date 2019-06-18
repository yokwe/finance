package yokwe.finance.stock.iex.cloud;

import javax.json.JsonObject;

public class RefData {
	public static class IEX {
		public static class Symbols extends Base implements Comparable<Symbols> {
			public static final String METHOD = "/ref-data/iex/symbols";
			public static final String PATH   = "/iex-symbols.csv";

			// {"symbol":"A","date":"2019-06-17","isEnabled":true}
			public String  symbol;
			public String  date;
			public boolean isEnabled;
			
			public Symbols() {
				symbol    = null;
				date      = null;
				isEnabled = false;
			}
			
			public Symbols(JsonObject jsonObject) {
				super(jsonObject);
			}

			@Override
			public int compareTo(Symbols that) {
				return this.symbol.compareTo(that.symbol);
			}
		}
	}
	
	
	public static class OTC {
		public static class Symbols extends Base implements Comparable<Symbols> {
			public static final String METHOD = "/ref-data/otc/symbols";
			public static final String PATH   = "/otc-symbols.csv";

			// symbol,exchange,name,date,type,iexId,region,currency,isEnabled
			public String  symbol;
			public String  exchange;
			public String  name;
			public String  date;
			public String  type;
			public String  iexId;
			public String  region;
			public String  currency;
			public boolean isEnabled;
			
			public Symbols() {
				symbol    = null;
				exchange  = null;
				name      = null;
				date      = null;
				type      = null;
				iexId     = null;
				region    = null;
				currency  = null;
				isEnabled = false;
			}
			
			public Symbols(JsonObject jsonObject) {
				super(jsonObject);
			}

			@Override
			public int compareTo(Symbols that) {
				return this.symbol.compareTo(that.symbol);
			}
		}
	}


	public static class Market {
		public static class US {
			public static class Exchanges extends Base implements Comparable<Exchanges> {
				public static final String METHOD = "/ref-data/market/us/exchanges";
				public static final String PATH   = "/us-exchanges.csv";

				// mic,name,longName,tapeId,oatsId,type
				public String mic;
				public String name;
				public String longName;
				public String tapeId;
				public String oatsId;
				public String type;
				
				public Exchanges() {
					mic      = null;
					name     = null;
					longName = null;
					tapeId   = null;
					oatsId   = null;
					type     = null;
				}
				
				public Exchanges(JsonObject jsonObject) {
					super(jsonObject);
				}

				@Override
				public int compareTo(Exchanges that) {
					return this.mic.compareTo(that.mic);
				}
			}
		}
	}
	
	
	public static class Symbols extends Base implements Comparable<Symbols> {
		public static final String METHOD = "/ref-data/symbols";
		public static final String PATH   = "/symbols.csv";

		// symbol,exchange,name,date,type,iexId,region,currency,isEnabled
		public String  symbol;
		public String  exchange;
		public String  name;
		public String  date;
		public String  type;
		public String  iexId;
		public String  region;
		public String  currency;
		public boolean isEnabled;
		
		public Symbols() {
			symbol    = null;
			exchange  = null;
			name      = null;
			date      = null;
			type      = null;
			iexId     = null;
			region    = null;
			currency  = null;
			isEnabled = false;
		}
		
		public Symbols(JsonObject jsonObject) {
			super(jsonObject);
		}

		@Override
		public int compareTo(Symbols that) {
			return this.symbol.compareTo(that.symbol);
		}
	}

	
	public static class Exchanges extends Base implements Comparable<Exchanges> {
		public static final String METHOD = "/ref-data/exchanges";
		public static final String PATH   = "/exchanges.csv";

		// exchange,region,description,mic,exchangeSuffix
		public String exchange;
		public String region;
		public String description;
		public String mic;
		public String exchangeSuffix;
		
		public Exchanges() {
			exchange       = null;
			region         = null;
			description    = null;
			mic            = null;
			exchangeSuffix = null;
		}
		
		public Exchanges(JsonObject jsonObject) {
			super(jsonObject);
		}

		@Override
		public int compareTo(Exchanges that) {
			int ret = this.region.compareTo(that.region);
			if (ret == 0) ret = this.exchange.compareTo(that.exchange);
			return ret;
		}
	}

}
