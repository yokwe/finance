package yokwe.finance.stock.rakuten;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.CSVUtil;
import yokwe.finance.stock.util.CSVUtil.ColumnName;
import yokwe.finance.stock.util.HttpUtil;

public class UpdateStockUS {
	private static final Logger logger = LoggerFactory.getLogger(UpdateStockUS.class);

	public static final String URL_ETF   = "https://www.rakuten-sec.co.jp/web/market/search/etf_search/ETFD.csv";
	public static final String URL_STOCK = "https://www.trkd-asia.com/rakutensec/exportcsvus?all=on&vall=on&r1=on&forwarding=na&target=0&theme=na&returns=na&head_office=na&name=&sector=na&pageNo=&c=us&p=result";
	
	public static final String PATH_RAKUTEN_US = "tmp/rakuten/rakuten-stock-us.csv";
	
	// 現地コード,銘柄名(English),銘柄名,市場,業種,取扱
	public static class StockData {
		public static final String TRADEABLE_YES = "○";
		
		@ColumnName("現地コード")
		public String ticker;
		@ColumnName("銘柄名(English)")
		public String name;
		@ColumnName("銘柄名")
		public String nameJP;
		@ColumnName("市場")
		public String exchange;
		@ColumnName("業種")
		public String industry;
		@ColumnName("取扱")
		public String tradeable;
	}
	
	public static class ETFData {
//		"QQQ.O"
		public String tickerInternal;
//		"QQQ"
		public String ticker;
//		"Invesco QQQ Trust Series 1"
		public String name;
//		"ﾅｽﾀﾞｯｸ"
		public String exchangeJP;
//		"NASDAQ 100 TR"
		public String indexName;
//		"0.20"
		public String expenseRatio;
//		"株式"
		public String assetClass;
//		"アメリカ"
		public String assetRegion;
//		"252.07"
		public String pctChangeStart;
//		"2.85"
		public String pctChangeYTD;
//		"-8.01"
		public String pctChane1M;
//		"-11.55"
		public String pctChane3M;
//		"-5.09"
		public String pctChane6M;
//		"-2.82"
		public String pctChane9M;
//		"3.17"
		public String pctChaneY1;
//		"36.69"
		public String pctChaneY2;
//		"43.23"
		public String pctChaneY3;
//		"101.22"
		public String pctChaneY5;
//		"米ドル"
		public String currency;
//		"0.83"
		public String pctChange1D;
//		"159.22"
		public String referencePrice;
//		"2018/11/20"
		public String referencePriceDate;
//		"パワーシェアーズ QQQ 信託シリーズ1"
		public String nameJP;
//		"パワーシェアーズ QQQ 信託シリーズ1(PowerShares QQQ Trust Series 1)はパワーシェアーズ・キューキューキュー指数連動株式と呼ばれる証券を発行するユニット型投資信託。同信託はナスダック100指数(Nasdaq-100 Index)(同指数)の構成証券の全てを保有する。同信託の投資目的は同指数の価格・利回り実績に連動する投資成果を提供すること。同信託のスポンサーはInvesco PowerShares Capital Management, LLCで、受託銀行はThe Bank of New York Mellonである。"
		public String description;
//		"67716.70"
		public String aum;
//		"百万米ドル"
		public String aumUnit;
//		"2018/10/31"
		public String aumDate;
	}

	public static void save(List<StockUS> usSecurityList) {
		CSVUtil.saveWithHeader(usSecurityList, PATH_RAKUTEN_US);
	}
	public static List<StockUS> load() {
		return CSVUtil.loadWithHeader(PATH_RAKUTEN_US, StockUS.class);
	}
	
	private static <E> List<E> loadData(String url, Class<E> clazz) {
		String content = HttpUtil.downloadAsString(url);
		Reader reader = new StringReader(content);
		
		CSVFormat csvFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
		List<E> ret = CSVUtil.load(reader, clazz, csvFormat);
		
		logger.info("URL {}", url);
		logger.info("  {}", ret.size());
		return ret;
	}
	private static <E> List<E> loadDataWithHeader(String url, Class<E> clazz) {
		String content = HttpUtil.downloadAsString(url);
		Reader reader = new StringReader(content);
		
		List<E> ret = CSVUtil.loadWithHeader(reader, clazz);
		
		logger.info("URL {}", url);
		logger.info("  {}", ret.size());
		return ret;
	}

	public static void main(String[] args) {
		logger.info("START");
		
		Map<String, StockUS> stockMap = new TreeMap<>();
		int countStock = 0;
		int countETF = 0;
		
		List<ETFData> etfDataList = loadData(URL_ETF, ETFData.class);
		List<StockData> stockDataList = loadDataWithHeader(URL_STOCK, StockData.class);

		for(ETFData data: etfDataList) {
			switch(data.exchangeJP) {
			case "香港":
			case "名証ETF":
			case "東証ETF":
			case "ｼﾝｶﾞﾎﾟｰﾙ":
				continue;
			case "ﾅｽﾀﾞｯｸ":
				data.exchangeJP = "NASDAQ";
				break;
			case "NYSE ARCA":
				data.exchangeJP = "NYSE Arca";
				break;
			default:
				logger.error("Unexpected {}", data.exchangeJP);
				throw new UnexpectedException("Unexpected");
			}
			
			StockUS stock = new StockUS(data.ticker, data.name, data.nameJP, data.exchangeJP, "ETF");
			stock.name = stock.name.replaceAll("&amp;", "&");
			stock.nameJP = stock.nameJP.replaceAll("&amp;", "&");
			
			if (stockMap.containsKey(stock.ticker)) {
				logger.error("Unexpected {}", stock.ticker);
				throw new UnexpectedException("Unexpected");
			}
			stockMap.put(stock.ticker, stock);
			countETF++;
		}
		
		for(StockData data: stockDataList) {			
			StockUS stock = new StockUS(data.ticker, data.name, data.nameJP, data.exchange, "普通株式");
			if (stockMap.containsKey(stock.ticker)) {
				StockUS oldStock = stockMap.get(stock.ticker);
				if (oldStock.category.compareTo("ETF") == 0) continue;
				logger.error("Unexpected {}", stock.ticker);
				throw new UnexpectedException("Unexpected");
			}
			
			stock.name = stock.name.replaceAll("&amp;", "&");
			stock.nameJP = stock.nameJP.replaceAll("&amp;", "&");

			stockMap.put(stock.ticker, stock);
			
			countStock++;
		}
		
		List<StockUS> stockUSList = new ArrayList<>(stockMap.values());
		Collections.sort(stockUSList);

		logger.info("OUTPUT = {}", PATH_RAKUTEN_US);
		save(stockUSList);

		logger.info("ETF    = {}", String.format("%5d", countETF));
		logger.info("STOCK  = {}", String.format("%5d", countStock));
		logger.info("TOTAL  = {}", String.format("%5d", countETF + countStock));
		
		logger.info("STOP");		
	}
}
