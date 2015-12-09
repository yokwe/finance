package yokwe.finance.securities.stats;

import java.util.ArrayList;
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
	
	public final String      symbol;
	public final List<Daily> list;
	private      int         size;
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
	public void logReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		List<Daily> save = new ArrayList<>(list);
		list.clear();
		
		boolean firstTime = true;
		Daily yesterday = null;
		for(Daily today: save) {
			if (firstTime) {
				yesterday = today;
				firstTime = false;
			} else {
				list.add(today.logReturn(yesterday));
				yesterday = today;
			}
		}
		size = list.size();
	}
	public void relativeReturn() {
		if (size <= 1) {
			logger.error("size = {}	, size");
			throw new SecuritiesException("size <= 1");
		}

		List<Daily> save = new ArrayList<>(list);
		list.clear();
		
		boolean firstTime = true;
		Daily yesterday = null;
		for(Daily today: save) {
			if (firstTime) {
				yesterday = today;
				firstTime = false;
			} else {
				list.add(today.relativeReturn(yesterday));
				yesterday = today;
			}
		}
		size = list.size();
	}
}
