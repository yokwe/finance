package yokwe.finance.securities.eod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class DateMap<E> {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DateMap.class);
	
	private List<String>   dateList = new ArrayList<>();
	private Map<String, E> map      = new TreeMap<>();
	
	public void put(String date, E data) {
		if (map.containsKey(date)) {
			map.replace(date, data);
			return;
		}
		map.put(date, data);
		
		dateList.add(date);
		Collections.sort(dateList);
	}
	
	public String getValidDate(String date) {
		int index = Collections.binarySearch(dateList, date);
		if (index < 0) {
			index = - (index + 1) - 1;
			if (index < 0) {
				logger.info("Unexpected date = {}", date);
				throw new SecuritiesException("Unexpected");
			}
		}
		return dateList.get(index);
	}
	
	public E get(String date) {
		String validDate = getValidDate(date);
		return map.get(validDate);
	}
	
	public int size() {
		return dateList.size();
	}
	
	public Map<String, E> getMap() {
		return map;
	}
}
