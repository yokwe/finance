package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class Portfolio {
	private static final Logger logger = LoggerFactory.getLogger(Portfolio.class);

	public static final double CONFIDENCE_95_PERCENT = 1.65;
	public static final double CONFIDENCE_99_PERCENT = 2.33;
	
	private static Random random = new Random(System.currentTimeMillis());
	
	public static List<Integer> allocateRandom(List<Asset> assetList, double valueTotal) {
		final int size = assetList.size();
		double values[] = new double[size];
		{
			double total = 0.0;
			for(int i = 0; i < size; i++) {
				values[i] = random.nextDouble();
				total += values[i];
			}
			for(int i = 0; i < size; i++) {
				values[i] = (values[i] / total) * valueTotal;
			}			
		}
		
		int ret[] = new int[size];
		{
			double remaining = valueTotal;
			// First try
			for(int i = 0; i < size; i++) {
				double lastPrice = assetList.get(i).lastPrice;
				int count = (int)Math.floor(values[i] / lastPrice);
				ret[i]    = count;
				remaining -= lastPrice * count;
			}
			// Second try
			final double minPrice = assetList.stream().mapToDouble(o -> o.lastPrice).min().getAsDouble();
			for(;;) {
				if (remaining < minPrice) break;
				
				int index = random.nextInt(size);
				if (assetList.get(index).lastPrice <= remaining) {
					double lastPrice = assetList.get(index).lastPrice;
					int count = (int)Math.floor(remaining / lastPrice);
					ret[index] += count;
					remaining -= lastPrice * count;
					if (remaining < 0) {
						logger.error("remaining < 0");
						throw new SecuritiesException("remaining < 0");
					}
				}
			}
		}

		List<Integer> retList = new ArrayList<>();
		for(int e: ret) retList.add(e);
		return retList;
	}
	public static Portfolio[] random(List<Asset> assetList, double valueTotal, UniStats market) {
		List<Integer> amountList = allocateRandom(assetList, valueTotal);
		return getInstance(assetList, amountList, market);
	}
	public static Portfolio[] random(Portfolio[] portfolios, double valueTotal, UniStats market) {
		List<Asset> assetList = new ArrayList<>();
		for(Portfolio portfolio: portfolios) {
			assetList.add(portfolio.asset);
		}
		return random(assetList, valueTotal, market);
	}
	
	public static double dividend(Portfolio[] portfolios) {
		double ret = 0;
		for(Portfolio portfolio: portfolios) {
			ret += portfolio.dividend;
		}
		return ret;
	}
	public static double sum(Portfolio[] portfolios) {
		double ret = 0;
		for(Portfolio portfolio: portfolios) {
			ret += portfolio.value;
		}
		return ret;
	}
	public static double[] ratio(Portfolio[] portfolios) {
		final int    size       = portfolios.length;
		final double valueTotal = Portfolio.sum(portfolios);
		
		double ret[] = new double[size];
		for(int i = 0; i < size; i++) {
			ret[i] = portfolios[i].value / valueTotal;
		}
		return ret;
	}
	public static UniStats[] uniStats(Portfolio[] portfolios) {
		final int    size       = portfolios.length;
		
		UniStats ret[] = new UniStats[size];
		for(int i = 0; i < size; i++) {
			ret[i] = portfolios[i].asset.toUniStats();
		}
		return ret;
	}
	
	public static double hv(Portfolio portfolios[]) {
		double   ratioArray[]    = ratio(portfolios);
		UniStats statsArray[]    = uniStats(portfolios);
		BiStats  statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		return hv(ratioArray, statsArray, statsMatrix);
	}
	private static double hv(double ratioArray[], UniStats statsArray[], BiStats statsMatrix[][]) {
		int size = ratioArray.length;
		double hv = 0;
		// Calculate upper right triangle area
		for(int i = 0; i < size; i++) {
			for(int j = i + 1; j < size; j++) {
				hv += 2.0 * ratioArray[i] * ratioArray[j] * statsMatrix[i][j].correlation * statsArray[i].sd * statsArray[j].sd;
			}
		}
		// Calculate diagonal area
		for(int i = 0; i < size; i++) {
			hv += ratioArray[i] * ratioArray[i] * statsArray[i].variance;
		}
		return Math.sqrt(hv);
	}

	public static UniStats getUniStats(Connection connection, LocalDate dateFrom, LocalDate dateTo, String symbol) {
		return Asset.getInstance(connection, symbol, dateFrom, dateTo).toUniStats();
	}
	public static Portfolio[] getInstance(Connection connection, LocalDate dateFrom, LocalDate dateTo, Map<String, Integer> assetMap, UniStats market) {
		List<Integer> volumeList = new ArrayList<>();
		List<Asset>   assetList  = new ArrayList<>();
		for(Map.Entry<String, Integer> entry: assetMap.entrySet()) {
			volumeList.add(entry.getValue());
			assetList.add(Asset.getInstance(connection, entry.getKey(), dateFrom, dateTo));
		}
		return getInstance(assetList, volumeList, market);
	}
	public static Portfolio[] getInstance(List<Asset> assetList, List<Integer> volumeList, UniStats market) {
		if (volumeList.size() != assetList.size()) {
			logger.error("mountList.size = {}  assetList.size = {}", volumeList.size(), assetList.size());
			throw new SecuritiesException("volumeList.size() != assetList.size()");
		}
		final int size  = volumeList.size();
		Portfolio ret[] = new Portfolio[size];
		for(int i = 0; i < size; i++) {
			Asset    asset  = assetList.get(i);
			int      volume = volumeList.get(i);
			FinStats stats  = new FinStats(market, asset.toUniStats());
			ret[i] = new Portfolio(asset, volume, stats.getBeta());
		}
		return ret;
	}
	
	public static double[] getMarketBeta(Portfolio portfolios[], UniStats market) {
		int size = portfolios.length;
		double ret[] = new double[size];
		for(int i = 0; i < size; i++) {
			UniStats stock = portfolios[i].asset.toUniStats();
			FinStats stats = new FinStats(market, stock);
			ret[i] = stats.getBeta();
		}
		return ret;
	}
	
	// marketGrowthRatio 0.0 => no change
	// marketGrowthRatio 0.5 => market increase 50 %
	public static double getGrowth(Portfolio portfolios[], double marketGrowthRatio) {
		double ret = 0;
		for(int i = 0; i < portfolios.length; i++) {
			ret += portfolios[i].value * portfolios[i].beta * marketGrowthRatio;
		}
		return ret;
	}
	
	public static double estimateValue(Portfolio portfolios[], int marketGrowthPercent, int timeHorizonDay, double confidence) {
		double   ratioArray[]    = ratio(portfolios);
		UniStats statsArray[]    = uniStats(portfolios);
		BiStats  statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		double   sum             = sum(portfolios);
		
		final double hv     = hv(ratioArray, statsArray, statsMatrix);
		final double div    = dividend(portfolios);
		final double growth = getGrowth(portfolios, (marketGrowthPercent * 0.01));
		final double var    = hv * confidence * sum * Math.sqrt((double)timeHorizonDay);
		final double value  = sum + div + growth - var;
		
//		logger.info("");
//		for(int i = 0; i < portfolios.length; i++) {
//			Portfolio portfolio = portfolios[i];
//			logger.info("ASSET {}", String.format("%-5s %3d %7.2f  %9.2f  %8.2f  %8.2f", portfolio.asset.symbol, portfolio.volume, portfolio.asset.lastPrice, portfolio.value, portfolio.dividend, portfolio.value * portfolio.beta * marketGrowthRatio));
//		}
//		logger.info("TOTAL          {}", String.format("%8.2f =%9.2f +%8.2f +%8.2f -%8.2f", value, sum, div, growth, var));
		return value;
	}
	public static double estimateValueDebug(Portfolio portfolios[], int marketGrowthPercent, int timeHorizonDay, double confidence) {
		double   ratioArray[]    = ratio(portfolios);
		UniStats statsArray[]    = uniStats(portfolios);
		BiStats  statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		double   sum             = sum(portfolios);
		
		final double hv     = hv(ratioArray, statsArray, statsMatrix);
		final double div    = dividend(portfolios);
		final double growth = getGrowth(portfolios, (marketGrowthPercent * 0.01));
		final double var    = hv * confidence * sum * Math.sqrt((double)timeHorizonDay);
		final double value  = sum + div + growth - var;
		
		logger.info("");
		for(int i = 0; i < portfolios.length; i++) {
			Portfolio portfolio = portfolios[i];
			logger.info("ASSET {}", String.format("%-5s%4d %7.2f  %9.2f  %8.2f  %8.2f            %8.4f",
					portfolio.asset.symbol, portfolio.volume, portfolio.asset.lastPrice, portfolio.value, portfolio.dividend, portfolio.value * portfolio.beta * marketGrowthPercent * 0.01, statsArray[i].sd));
		}
		logger.info("VALUE {}", String.format("         %8.2f =%9.2f +%8.2f +%8.2f -%8.2f  %8.4f", value, sum, div, growth, var, hv));
		logger.info("PERCENT        {}", String.format("%7.3f%% =%8.2f%% +%7.2f%% +%7.2f%% -%7.2f%%", value / sum * 100, sum / sum * 100, div / sum * 100, growth / sum * 100, var / sum * 100));
		return value;
	}
	
	
	public final Asset  asset;
	public final int    volume;
	public final double value;
	public final double dividend;
	public final double beta;
	
	public Portfolio(Asset asset, int volume, double beta) {
		this.asset    = asset;
		this.volume   = volume;
		this.value    = volume * asset.lastPrice;
		this.dividend = volume * asset.dividend;
		this.beta     = beta;
	}
	
	@Override
	public String toString() {
		return String.format("{%d %8.2f %8.2f %s}", volume, value, dividend, asset.toString());
	}
	
	public static void main(String args[]) {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
		final String JDBC_CONNECTION_URL = "jdbc:sqlite:/data1/home/hasegawa/git/finance/Securities/tmp/sqlite/securities.sqlite3";

		try (Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL)) {
			LocalDate dateTo   = LocalDate.now();
			LocalDate dateFrom = dateTo.minusYears(1);
			
			final UniStats market              = getUniStats(connection, dateFrom, dateTo, "SPY");
			final int      marketGrowthPercent = 5; // 5% increase
			final int      timeHorizonDay      = 5; // 5 days => 1 week
			final double   confidence          = CONFIDENCE_95_PERCENT;
			
			Map<String, Integer> assetMap = new TreeMap<>();
//			assetMap.put("BLV",   50); // Vanguard Long Term Bond ETF
//			assetMap.put("BIV",   50); // Vanguard Intermediate Term Bond ETF
//			assetMap.put("BSV",   50); // Vanguard Short Term Bond ETF
//			assetMap.put("VIG",   50); // Vanguard Dividend Appreciation ETF
			assetMap.put("IVV",   50); // iShares Core S&P 500 ETF
			assetMap.put("IJH",   50); // iShares Core S&P Mid-Cap ETF
//			assetMap.put("VBK",   50); // Vanguard Small Cap Growth ETF
//			assetMap.put("VWO",   50); // Vanguard Emerging Markets ETF
//			assetMap.put("FXI",   50); // iShares FTSE/Xinhua China 25 Index Fund
//			assetMap.put("DBC",   10); // PowerShares DB Commodity Index Tracking Fund
			assetMap.put("VCLT",  50); // Vanguard Long-Term Corporate Bond ETF
			assetMap.put("PGX",   50); // PowerShares Preferred Portfolio
			assetMap.put("VYM",   50); // Vanguard High Dividend Yield ETF
			Portfolio[]  portfolios   = Portfolio.getInstance(connection, dateFrom, dateTo, assetMap, market);
			double valueTotal = sum(portfolios);
		
			double value = estimateValue(portfolios, marketGrowthPercent, timeHorizonDay, confidence);
			estimateValueDebug(portfolios, marketGrowthPercent, timeHorizonDay, confidence);
			
			for(int i = 0; i < 100000; i++) {
				portfolios = random(portfolios, valueTotal, market);
				double valueTemp = estimateValue(portfolios, marketGrowthPercent, timeHorizonDay, confidence);
				
				if (value < valueTemp) {
					value = valueTemp;
					estimateValueDebug(portfolios, marketGrowthPercent, timeHorizonDay, confidence);
				}
			}
			
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
}
