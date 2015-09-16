package yokwe.finance.etf.util;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.etf.ETFException;


public class StreamUtil {
	static final Logger logger = LoggerFactory.getLogger(StreamUtil.class);
	
	private static class StatsAccumlator {
		int count = 0;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double sum = 0.0;
		double M = 0.0;
		double S = 0.0;
		
		void apply(double value) {
			count++;
			sum += value;
			
			if (value < min) min = value;
			if (max < value) max = value;
			
	        double tmpM = M;
	        M += (value - tmpM) / count;
	        S += (value - tmpM) * (value - M);
		}
		
		public String toString() {
			return String.format("[%d  %.3f]", count, sum);
		}
	}
	
	public static class Stats {
		public final int    count;
		public final double min;
		public final double max;
		public final double avg;
		public final double sdPopulation;
		public final double sdSample;
		
		private Stats(StatsAccumlator a) {
			this.count        = a.count;
			this.min          = a.min;
			this.max          = a.max;
			this.avg          = a.sum / a.count;
			this.sdPopulation = Math.sqrt(a.S / (a.count + 1 - 1));
			this.sdSample     = Math.sqrt(a.S / (a.count + 1 - 2));
		}
		public String toString() {
			return String.format("[%d  %.3f  %.3f  %.3f  %.3f  %.3f]", count, min, avg, max, sdPopulation, sdSample);
		}
	}
	
	public static final Collector<Double, StatsAccumlator, Stats> toStats;
	static {
		Supplier<StatsAccumlator> supplier = () -> new StatsAccumlator();
		BiConsumer<StatsAccumlator, Double> accumulator = (a, e) -> a.apply(e);
		BinaryOperator<StatsAccumlator> combiner = (a1, a2) -> {
			logger.error("combiner  {}  {}", a1.toString(), a2.toString());
			throw new ETFException("Not expected");
		};
		Function<StatsAccumlator, Stats> finisher = (a) -> new Stats(a);
		toStats = Collector.of(supplier, accumulator, combiner, finisher, Characteristics.CONCURRENT);
	}
	
	
	public static void main(String[] args) {
		logger.info("START");
		Double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
		List<Double> valueList = Arrays.asList(values);
		
		Stats stats = valueList.stream().collect(toStats);
		logger.info("Expect stats = [5  1.000  3.000  5.000  1.414  1.581]");
		logger.info("Actual stats = {}", stats);
		
		logger.info("STOP");
	}

}
