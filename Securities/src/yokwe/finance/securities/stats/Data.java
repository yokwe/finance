package yokwe.finance.securities.stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Data {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Data.class);

	public static class Daily {
		public final String date;
		public final double value;
		public Daily(String date, double value) {
			this.date  = date;
			this.value = value;
		}
		private Daily logReturn(Daily yesterday) {
			return new Daily(this.date, Math.log(this.value / yesterday.value));
		}
		private Daily relativeReturn(Daily yesterday) {
			return new Daily(this.date, (this.value / yesterday.value) - 1.0);
		}
	}
	
	public  final String      symbol;
	private final List<Daily> list;
	private       int         size;
	public Data(String symbol, List<Daily> that) {
		if (symbol == null) {
			logger.error("symbol == null");
			throw new SecuritiesException("symbol == null");
		}
		if (that == null) {
			logger.error("that == null");
			throw new SecuritiesException("that == null");
		}
		this.symbol = symbol;
		list   = new ArrayList<>(that);
		size   = list.size();
	}
	public Daily[] toArray() {
		return list.toArray(new Daily[0]);
	}
	public List<Daily> toList() {
		return new ArrayList<>(list);
	}
	public double[] toDoubleArray() {
		return list.stream().mapToDouble(o -> o.value).toArray();
	}
	public void setList(Daily[] newList) {
		setList(Arrays.asList(newList));
	}
	public void setList(List<Daily> newList) {
		list.clear();
		list.addAll(newList);
		size = list.size();
	}
	public void logReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		List<Daily> newList = new ArrayList<>();
		boolean firstTime = true;
		Daily yesterday = null;
		for(Daily today: list) {
			if (firstTime) {
				yesterday = today;
				firstTime = false;
			} else {
				newList.add(today.logReturn(yesterday));
				yesterday = today;
			}
		}
		setList(newList);
	}
	public void relativeReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		List<Daily> newList = new ArrayList<>();
		boolean firstTime = true;
		Daily yesterday = null;
		for(Daily today: list) {
			if (firstTime) {
				yesterday = today;
				firstTime = false;
			} else {
				newList.add(today.relativeReturn(yesterday));
				yesterday = today;
			}
		}
		setList(newList);
	}
}
