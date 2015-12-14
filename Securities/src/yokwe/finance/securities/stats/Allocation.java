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

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public final class Allocation {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Allocation.class);

	public static final int ALLOCAIONT_UNIT = 10;
	
	private static Random random = new Random(System.currentTimeMillis());
	
	public static List<Integer> allocateRandom(List<Asset> assetList, double valueTotal, int allocationUnit) {
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
		List<Integer> amountList = allocateRandom(assetList, valueTotal, ALLOCAIONT_UNIT);
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
			ret += allocation.amount * allocation.asset.dividend;
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
	

	public static Allocation[] getInstance(Connection connection, LocalDate dateFrom, LocalDate dateTo, Map<String, Integer> assetMap) {
		List<Integer> amountList = new ArrayList<>();
		List<Asset>   assetList  = new ArrayList<>();
		for(Map.Entry<String, Integer> entry: assetMap.entrySet()) {
			amountList.add(entry.getValue());
			assetList.add(new Asset(connection, entry.getKey(), dateFrom, dateTo));
		}
		return getInstance(assetList, amountList);
	}
	public static Allocation[] getInstance(List<Asset> assetList, List<Integer> amountList) {
		if (amountList.size() != assetList.size()) {
			logger.error("mountList.size = {}  assetList.size = {}", amountList.size(), assetList.size());
			throw new SecuritiesException("amountList.size() != assetList.size()");
		}
		final int    size  = amountList.size();
		final double total;
		{
			double t = 0;
			for(int i = 0; i < size; i++) {
				Asset asset  = assetList.get(i);
				int   amount = amountList.get(i);
				t += asset.lastPrice * amount;
			}
			total = t;
		}
		Allocation ret[] = new Allocation[size];
		for(int i = 0; i < size; i++) {
			Asset asset  = assetList.get(i);
			int   amount = amountList.get(i);
			ret[i] = new Allocation(asset, amount, (asset.lastPrice * amount) / total);
		}
		return ret;
	}
	public final Asset  asset;
	public final int    amount;
	public final double ratio;
	public final double value; // dollar value = amount * asset.lastPrice
	
	public Allocation(Asset asset, int amount, double ratio) {
		this.asset  = asset;
		this.amount = amount;
		this.ratio  = ratio;
		this.value  = amount * asset.lastPrice;
	}
	
	@Override
	public String toString() {
		return String.format("{%d %5.2f %8.2f %s}", amount, ratio, value, asset.toString());
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
			assetMap.put("VCLT", 100);
			assetMap.put("PGX",  300);
			assetMap.put("VYM",  100);
//			assetMap.put("ARR",   50);
			double valueTotal = 0;
			Allocation[] allocations = Allocation.getInstance(connection, dateFrom, dateTo, assetMap);
			for(Allocation allocation: allocations) {
				logger.info("ALLOC     {}", allocation.toString());
				valueTotal += allocation.value;
			}
			double hv  = HV.calculate(allocations);
			double div = Allocation.dividend(allocations);
			{
				double sd     = hv;
				double var1d  = sd * HV.CONFIDENCE_95_PERCENT * valueTotal;
				double var1m  = sd * HV.CONFIDENCE_95_PERCENT * valueTotal * Math.sqrt(21);
				logger.info("hv    {}", String.format("    %8.2f  %8.2f  %8.2f%8.2f", div, valueTotal, var1d, var1m));
			}
			
			for(int i = 0; i < 100; i++) {
				allocations = random(allocations, valueTotal);
				double hvTemp  = HV.calculateTerse(allocations);
				double divTemp = Allocation.dividend(allocations);
				if (hvTemp < hv && div < divTemp) {
					hv  = hvTemp;
					div = divTemp;
					logger.info("");
					{
						double sd     = hv;
						double var1d  = sd * HV.CONFIDENCE_95_PERCENT * valueTotal;
						double var1m  = sd * HV.CONFIDENCE_95_PERCENT * valueTotal * Math.sqrt(21);
						logger.info("hv    {}", String.format("    %8.2f  %8.2f  %8.2f%8.2f", div, valueTotal, var1d, var1m));
					}
					
					{
						double total = 0;
						for(Allocation allocation: allocations) {
							total += allocation.value;
							logger.info("RATIO {}", String.format("%-6s %5d  %8.4f  %8.2f", allocation.asset.symbol, allocation.amount, allocation.ratio, allocation.value));
						}
						logger.info("TOTAL                         {}", String.format("%8.2f %7.2f", total, valueTotal - total));
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
