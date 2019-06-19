package yokwe.finance.stock.iex.cloud;

import java.time.LocalDateTime;

import javax.json.JsonObject;

public class Tops extends Base implements Comparable<Tops> {
	public static final String METHOD = "/tops";
	public static final String PATH   = "/tops.csv";

	// symbol,sector,securityType,bidPrice,bidSize,askPrice,askSize,lastUpdated,lastSalePrice,lastSaleSize,lastSaleTime,volume,seq
	
	public String        symbol;
	public String        sector;
	public String        securityType;
	public double        bidPrice;
	public long          bidSize;
	public double        askPrice;
	public long          askSize;
	public LocalDateTime lastUpdated;
	public double        lastSalePrice;
	public long          lastSaleSize;
	public LocalDateTime lastSaleTime;
	public long          volume;
	public long          seq;
	
	public Tops() {
		symbol        = null;
		sector        = null;
		securityType  = null;
		bidPrice      = 0;
		bidSize       = 0;
		askPrice      = 0;
		askSize       = 0;
		lastUpdated   = null;
		lastSalePrice = 0;
		lastSaleSize  = 0;
		lastSaleTime  = null;
		volume        = 0;
		seq           = 0;
	}
	
	public Tops(JsonObject jsonObject) {
		super(jsonObject);
	}
	
	@Override
	public int compareTo(Tops that) {
		return this.symbol.compareTo(that.symbol);
	}
	
	
	public static class Last extends Base implements Comparable<Last> {
		public static final String METHOD = "/tops/last";
		public static final String PATH   = "/tops-last.csv";

		// symbol,price,size,time,seq
		public String        symbol;
		public double        price;
		public long          size;
		public LocalDateTime time;
		public long          seq;
		
		public Last() {
			symbol = null;
			price  = 0;
			size   = 0;
			time   = null;
			seq    = 0;
		}
		
		public Last(JsonObject jsonObject) {
			super(jsonObject);
		}
		
		@Override
		public int compareTo(Last that) {
			return this.symbol.compareTo(that.symbol);
		}
	}

	// symbol,price,size,time,seq
}
