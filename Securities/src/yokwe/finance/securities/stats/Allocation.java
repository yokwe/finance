package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
	
	public static List<Integer> allocateRandom(int allocationTotal, int numberOfAllocation, int allocationUnit) {
		double ratio[]    = new double[numberOfAllocation];
		double ratioTotal = 0.0;
		for(int i = 0; i < ratio.length; i++) {
			ratio[i] = random.nextDouble();
			ratioTotal += ratio[i];
		}
		
		List<Integer> ret = new ArrayList<>();
		{
			int remaining = allocationTotal;
			for(int i = ratio.length; 0 < i;) {
				i--;
				if (i == 0) {
					ret.add(remaining);
				} else {
					int amount = (int)Math.round(allocationTotal * (ratio[i] / ratioTotal) / allocationUnit) * allocationUnit;
					ret.add(amount);
					remaining -= amount;
				}
			}
		}

		return ret;
	}
	public static Allocation[] random(List<Asset> assetList, int allocationTotal) {
		List<Integer> amountList = allocateRandom(allocationTotal, assetList.size(), ALLOCAIONT_UNIT);
		return getInstance(assetList, amountList);
	}
	public static Allocation[] random(Allocation[] allocations) {
		int allocationTotal = 0;
		List<Asset> assetList = new ArrayList<>();
		for(Allocation allocation: allocations) {
			assetList.add(allocation.asset);
			allocationTotal += allocation.amount;
		}
		return random(assetList, allocationTotal);
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
		final double total = amountList.stream().mapToDouble(o -> (double)o).sum();
		Allocation ret[] = new Allocation[size];
		for(int i = 0; i < size; i++) {
			Asset asset  = assetList.get(i);
			int   amount = amountList.get(i);
			ret[i] = new Allocation(asset, amount, (amount / total));
		}
		return ret;
	}
	public final Asset  asset;
	public final int    amount;
	public final double ratio;
	
	public Allocation(Asset asset, int amount, double ratio) {
		this.asset  = asset;
		this.amount = amount;
		this.ratio  = ratio;
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
			assetMap.put("VCLT", 8600);
			assetMap.put("PGX",  4400);
			assetMap.put("VYM",  3400);
//			assetMap.put("ARR",  2100);
			Allocation[] allocations = Allocation.getInstance(connection, dateFrom, dateTo, assetMap);
			
			double hv = HV.calculate(allocations);
			logger.info("HV        {}", String.format("%8.4f", hv));
			
			for(int i = 0; i < 100000; i++) {
				allocations = random(allocations);
				double hvTemp = HV.calculateTerse(allocations);
				if (hvTemp < hv) {
					hv = hvTemp;
					logger.info("");
					logger.info("HV        {}", String.format("%8.4f", hv));
					
					{
						double amountTotal = Arrays.stream(allocations).mapToInt(o -> o.amount).sum();
//						logger.info("SUM          {}", String.format("%5d", (int)amountTotal));
						for(Allocation allocation: allocations) {
							double ratio = allocation.amount / amountTotal;;
							logger.info("RATIO {}", String.format("%-6s %5d  %8.4f", allocation.asset.symbol, allocation.amount, ratio));
						}
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
