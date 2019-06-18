package yokwe.finance.stock.iex.cloud;

import javax.json.JsonObject;

public class RefData {
	public static class IEX {
		public static class Symbols extends Base implements Comparable<Symbols> {
			public static final String METHOD = "/ref-data/iex/symbols";

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
}
