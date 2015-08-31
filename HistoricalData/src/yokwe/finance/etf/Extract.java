package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

public abstract class Extract {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Extract.class);

	protected final String  name;
	protected final int     groupCount;
	protected final Matcher matcher;
	
	protected Extract(String name, int groupCount, String pattern) {
		this.name       = name;
		this.groupCount = groupCount;
		matcher         = Pattern.compile(pattern).matcher("");
	}
	
	protected abstract String getValue(String fileName);
	
	public void reset(String fileName, String contents) {
		matcher.reset(contents);
		if (!matcher.find()) {
			logger.error("{}  NAME {}", fileName, name);
			logger.error("pat {}", matcher.toString());
			throw new RuntimeException("NAME");
		}
		if (matcher.groupCount() != groupCount) {
			logger.error("{}  GROUP_COUNT {}  groupCount = {}", fileName, name, matcher.groupCount());
			throw new RuntimeException("GROUP_COUNT");
		}
	}
	public String getValue(String fileName, String contents) {
		reset(fileName, contents);
		return getValue(fileName);
	}
	public String getValue(int group) {
		return matcher.group(group);
	}
	
	public static class Simple extends Extract {
		public Simple(String name, int groupCount, String pattern) {
			super(name, groupCount, pattern);
		}
		protected String getValue(String fileName) {
			return matcher.group(1);
		}
	}
	
	public static class Set {
		Map<String, Extract> map = new TreeMap<>();
		String fileName = null;
		String contents = null;
		
		public void add(String name, int groupCount, String pattern) {
			Extract extract = new Extract.Simple(name, groupCount, pattern);
			
			map.put(name, extract);
		}
		public void add(Extract extract) {
			map.put(extract.name, extract);
		}
		
		public void reset(File file) {
			fileName = file.getName();
			contents = Util.getContents(file);

			for(Extract extract: map.values()) {
				extract.reset(fileName, contents);
			}
		}
		public String getValue(String name, int group) {
			return map.get(name).getValue(group);
		}
		public String getValue(String name) {
			return getValue(name, 1);
		}
	}
}