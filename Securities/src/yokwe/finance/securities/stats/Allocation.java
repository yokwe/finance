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

public final class Allocation {
	private static final Logger logger = LoggerFactory.getLogger(Allocation.class);

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
	public static Allocation[] random(List<Asset> assetList, double valueTotal) {
		List<Integer> amountList = allocateRandom(assetList, valueTotal);
		return getInstance(assetList, amountList);
	}
	public static Allocation[] random(Allocation[] allocations, double valueTotal) {
		List<Asset> assetList = new ArrayList<>();
		for(Allocation allocation: allocations) {
			assetList.add(allocation.asset);
		}
		return random(assetList, valueTotal);
	}
	
	public static double dividend(Allocation[] allocations) {
		double ret = 0;
		for(Allocation allocation: allocations) {
			ret += allocation.dividend;
		}
		return ret;
	}
	public static double value(Allocation[] allocations) {
		double ret = 0;
		for(Allocation allocation: allocations) {
			ret += allocation.value;
		}
		return ret;
	}
	public static double[] ratio(Allocation[] allocations) {
		final int    size       = allocations.length;
		final double valueTotal = Allocation.value(allocations);
		
		double ret[] = new double[size];
		for(int i = 0; i < size; i++) {
			ret[i] = allocations[i].value / valueTotal;
		}
		return ret;
	}
	public static UniStats[] uniStats(Allocation[] allocations) {
		final int    size       = allocations.length;
		
		UniStats ret[] = new UniStats[size];
		for(int i = 0; i < size; i++) {
			ret[i] = new UniStats(DoubleArray.logReturn(allocations[i].asset.price));
		}
		return ret;
	}
	
	public static double hv(Allocation allocations[]) {
		double   ratioArray[]    = ratio(allocations);
		UniStats statsArray[]    = uniStats(allocations);
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

	public static Allocation[] getInstance(Connection connection, LocalDate dateFrom, LocalDate dateTo, Map<String, Integer> assetMap) {
		List<Integer> amountList = new ArrayList<>();
		List<Asset>   assetList  = new ArrayList<>();
		for(Map.Entry<String, Integer> entry: assetMap.entrySet()) {
			amountList.add(entry.getValue());
			assetList.add(Asset.getInstance(connection, entry.getKey(), dateFrom, dateTo));
		}
		return getInstance(assetList, amountList);
	}
	public static Allocation[] getInstance(List<Asset> assetList, List<Integer> amountList) {
		if (amountList.size() != assetList.size()) {
			logger.error("mountList.size = {}  assetList.size = {}", amountList.size(), assetList.size());
			throw new SecuritiesException("amountList.size() != assetList.size()");
		}
		final int size  = amountList.size();
		Allocation ret[] = new Allocation[size];
		for(int i = 0; i < size; i++) {
			Asset asset  = assetList.get(i);
			int   amount = amountList.get(i);
			ret[i] = new Allocation(asset, amount);
		}
		return ret;
	}
	public final Asset  asset;
	public final int    amount;
	public final double value;
	public final double dividend;
	
	public Allocation(Asset asset, int amount) {
		this.asset    = asset;
		this.amount   = amount;
		this.value    = amount * asset.lastPrice;
		this.dividend = amount * asset.dividend;
	}
	
	@Override
	public String toString() {
		return String.format("{%d %8.2f %8.2f %s}", amount, value, dividend, asset.toString());
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
			
			Map<String, Integer> assetMap = new TreeMap<>();
			assetMap.put("BLV",   50);
			assetMap.put("BIV",   50);
			assetMap.put("BSV",   50);
			assetMap.put("VIG",   50);
			assetMap.put("IVV",   50);
			assetMap.put("IJH",   50);
			assetMap.put("VBK",   50);
			assetMap.put("VWO",   50);
			assetMap.put("FXI",   50);
//			assetMap.put("DBC",   10);
			Allocation[] allocations = Allocation.getInstance(connection, dateFrom, dateTo, assetMap);
			final double valueTotal  = Allocation.value(allocations);
			final int    size        = allocations.length;
			final double spyBeta[]   = new double[size];
		
			// calculate spyBeta
			{
				String   marketSymbol = "SPY";
				UniStats market       = Asset.getInstance(connection, marketSymbol, dateFrom, dateTo).toUniStats();
				for(int i = 0; i < size; i++) {
					UniStats stock = allocations[i].asset.toUniStats();
					FinStats stats = new FinStats(market, stock);
					spyBeta[i] = (0.7 <= stats.r2) ? stats.beta : 0;
				}
			}

			double hv     = 0;
			double div    = 0;
			double growth = 0;
			{
				double   ratioArray[]    = ratio(allocations);
				UniStats statsArray[]    = uniStats(allocations);
				BiStats  statsMatrix[][] = DoubleArray.getMatrix(statsArray);
				
				hv     = hv(ratioArray, statsArray, statsMatrix);
				div    = Allocation.dividend(allocations);
				growth = DoubleArray.multiplyAndAdd(ratio(allocations), spyBeta);
				
				// Show relation to stock market
				{
					String   marketSymbol = "SPY";
					UniStats market       = Asset.getInstance(connection, marketSymbol, dateFrom, dateTo).toUniStats();
					logger.info("");
					for(Allocation allocation: allocations) {
						UniStats stock = allocation.asset.toUniStats();
						logger.info("{}", String.format("%-5s %-5s %s", marketSymbol, allocation.asset.symbol, new FinStats(market, stock)));
					}
				}
				
				// Show characteristic
				{
					logger.info("");
					logger.info("STATS         MEAN     SD   SD-LR    VALUE  VaR1d VaR1m");
					for(int i = 0; i < size; i++) {
						Asset asset   = allocations[i].asset;
						double value  = 1000;
						String symbol = asset.symbol;
						double data[] = asset.price;
						double mean   = DoubleArray.mean(data);
						double sd     = DoubleArray.sd(data);
						double sdlr   = statsArray[i].sd;
						double var1d  = sdlr * CONFIDENCE_95_PERCENT * value;
						double var1m  = sdlr * CONFIDENCE_95_PERCENT * value * Math.sqrt(21);
						logger.info("STATS {}", String.format("%-5s %6.2f %6.2f  %6.4f  %6.2f %6.2f %6.2f", symbol, mean, sd, sdlr, value, var1d, var1m));
					}
				}
				
				// Show correlation
				{
					logger.info("");
					StringBuilder buf = new StringBuilder();
					buf.append("CORR          ");
					for(int i = 0; i < size; i++) {
						buf.append(String.format("%-5s ", allocations[i].asset.symbol));
					}
					logger.info(buf.toString());
					
					for(int i = 0; i < size; i++) {
						buf.setLength(0);
						buf.append(String.format("CORR  %-5s ", allocations[i].asset.symbol));
						for(int j = 0; j < size; j++) {
							buf.append(String.format("%6.2f", statsMatrix[i][j].correlation));
						}
						logger.info(buf.toString());
					}
				}
			}
			
			// Show relation to stock market
			{
				String   marketSymbol = "SPY";
				UniStats market       = Asset.getInstance(connection, marketSymbol, dateFrom, dateTo).toUniStats();
				logger.info("");
				for(Allocation allocation: allocations) {
					UniStats stock = allocation.asset.toUniStats();
					logger.info("{}", String.format("%-5s %-5s %s", marketSymbol, allocation.asset.symbol, new FinStats(market, stock)));
				}
			}
			
			// Show relation to bond market
//			{
//				String   marketSymbol = "BND";
//				UniStats market       = new Asset(connection, marketSymbol, dateFrom, dateTo).toUniStats();
//				logger.info("");
//				for(Allocation allocation: allocations) {
//					UniStats stock = allocation.asset.toUniStats();
//					logger.info("{}", String.format("%-5s %-5s %s", marketSymbol, allocation.asset.symbol, new FinStats(market, stock)));
//				}
//			}
			
			// show allocation
			{
				logger.info("");
				double total = Allocation.value(allocations);;
				double var1d = hv * CONFIDENCE_95_PERCENT * total;
				double var1m = hv * CONFIDENCE_95_PERCENT * total * Math.sqrt(21);

				for(Allocation allocation: allocations) {
					logger.info("ASSET {}", String.format("%-6s %5d  %8.2f  %8.2f", allocation.asset.symbol, allocation.amount, allocation.value, allocation.dividend));
				}
				logger.info("TOTAL               {}", String.format("%8.2f  %8.2f (%7.2f)  %5.2f %%  %8.4f", total, div, total - valueTotal, (div / total) * 100, growth));
				logger.info("HV VAR    {}", String.format("%8.4f  %8.2f  %8.2f", hv, var1d, var1m));
			}
						
			for(int i = 0; i < 1000000; i++) {
				allocations = random(allocations, valueTotal);
				double hvTemp       = hv(allocations);
				double divTemp      = Allocation.dividend(allocations);
				double growthTemp   = DoubleArray.multiplyAndAdd(ratio(allocations), spyBeta);

				if (hvTemp < hv && div < divTemp && growth < growthTemp) {
//				if (hvTemp < 0.01 && growth < growthTemp) {
//				if (hvTemp < 0.01 && div < divTemp) {
					hv     = hvTemp;
					div    = divTemp;
					growth = growthTemp;
					{
						logger.info("");
						double total = Allocation.value(allocations);;
						double var1d = hv * CONFIDENCE_95_PERCENT * total;
						double var1m = hv * CONFIDENCE_95_PERCENT * total * Math.sqrt(21);

						for(Allocation allocation: allocations) {
							logger.info("ASSET {}", String.format("%-6s %5d  %8.2f  %8.2f", allocation.asset.symbol, allocation.amount, allocation.value, allocation.dividend));
						}
						logger.info("TOTAL               {}", String.format("%8.2f  %8.2f (%7.2f)  %5.2f %%  %8.4f", total, div, total - valueTotal, (div / total) * 100, growth));
						logger.info("HV VAR    {}", String.format("%8.4f  %8.2f  %8.2f", hv, var1d, var1m));
					}
				}
			}
		} catch (SQLException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException();
		}
	}
}
