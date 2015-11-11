package yokwe.finance.securities.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Correlation {
	private static final Logger logger = LoggerFactory.getLogger(Correlation.class);

	public  final int                   length;
	public  final int                   size;
	private final Map<String, Integer>  indexMap; // map from name to index
	private final String[]              names;    // String[size] all name in dataMap
	private final double[][]            ccArray;  // double[size][size]
	private final double[][]            allData;  // double[size][length]
	
	public Correlation(Map<String, double[]> doubleMap) {
		size     = doubleMap.size();

		indexMap = new HashMap<>();
		names    = new String[size];
		ccArray  = new double[size][size];
		allData  = new double[size][];

		{
			int index = 0;
			for(String name: doubleMap.keySet()) {
				names[index++] = name;
			}
			// Sort names before use.  dataMap can be HashMap
			Arrays.sort(names);
		}
		
		length = doubleMap.get(names[0]).length;

		double[][] devArray = new double[size][length];
		double[]   sdArray  = new double[size];
		
		for(int index = 0; index < names.length; index++) {
			final String   name = names[index];
			final double[] data = doubleMap.get(name);
			final double[] dev  = devArray[index];
							
			// Sanity check
			if (data.length != length) {
				logger.error("name = {}  length = {}  data.length = {}", name, length, data.length);
				throw new SecuritiesException("length");
			}
			
			// build indexMap
			indexMap.put(name, index);

			// build allData
			allData[index] = data;
							
			// build devArray
			double mean = 0;
			for(double e: data) mean += e;
			mean /= length;
			double var = 0;
			for(int i = 0; i < length; i++) {
				double t = data[i] - mean;
				dev[i] = t;
				var += t * t;
			}
			
			// build sdArray
			sdArray[index] = 1.0 / Math.sqrt(var);
		}
		// Compute ccArray from devArray and sdArray
		for(int x = 0; x < size; x++) {
			for(int y = 0; y < x; y++) {
				double[] dataX = devArray[x];
				double[] dataY = devArray[y];
				double   sdX   = sdArray[x];
				double   sdY   = sdArray[y];
				
				double cc = 0;
				for(int i = 0; i < dataX.length; i++) cc += dataX[i] * dataY[i];
				cc *= (sdX * sdY);
				ccArray[x][y] = cc;
				ccArray[y][x] = cc;
			}
			ccArray[x][x] = 1.0;
		}
	}
	private int getIndex(String name) {
		if (indexMap.containsKey(name)) {
			return indexMap.get(name);
		} else {
			logger.error("Unknown name = {}", name);
			throw new SecuritiesException("unknown name");
		}
	}
	public String[] getNames() {
		return names;
	}
	public double[] getData(String name) {
		final int index = getIndex(name);
		if (allData.length <= index) {
			logger.error("Cant happen {}  {}", name, index);
			throw new SecurityException("getData");
		}
		return allData[index];
	}
	public double getCorrelation(String nameX, String nameY) {
		final int indexX = getIndex(nameX);
		final int indexY = getIndex(nameY);
		
		return ccArray[indexX][indexY];
	}
	
	public double getCorrelationX(String nameX, String nameY) {
		double[] x = getData(nameX);
		double[] y = getData(nameY);
		return new PearsonsCorrelation().correlation(x, y);
	}
	public Map<String, Double> getCorrelation(String name) {
		final int index = getIndex(name);
		Map<String, Double> ret = new TreeMap<>();
		for(int i = 0; i < names.length; i++) {
			ret.put(names[i], ccArray[index][i]);
		}
		return ret;
	}
}
