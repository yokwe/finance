package yokwe.finance.securities.stats;

import java.util.Arrays;

import yokwe.finance.securities.stats.DoubleArray.BiStats;
import yokwe.finance.securities.stats.DoubleArray.UniStats;

public class HV {
	public static class Assets {
		public final int    amount;
		public final Data   data;
		public Assets(int amount, Data data) {
			this.amount = amount;
			this.data   = data;
		}
	}

	// calculate composite Historical Volatility
	public static double calculate(Assets[] assets) {
		final int size = assets.length;
		
		double ratio[] = new double[size];
		{
			double sum = Arrays.stream(assets).mapToDouble(o -> o.amount).sum();
			for(int i = 0; i < size; i++) ratio[i] = sum / assets[i].amount;
		}
		double data[][] = new double[size][];
		for(int i = 0; i < size; i++) {
			data[i] = assets[i].data.toDoubleArray();
		}
		
		UniStats statsArray[] = new UniStats[size];
		for(int i = 0; i < size; i++) {
			statsArray[i] = new DoubleArray.UniStats(data[i]);
		}
		BiStats statsMatrix[][] = DoubleArray.getMatrix(statsArray);
		
		return 0;
	}
}
