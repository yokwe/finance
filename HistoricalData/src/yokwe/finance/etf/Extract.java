package yokwe.finance.etf;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

public abstract class Extract {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Extract.class);
	public static final String NO_VALUE = "*NOVALUE*";

	protected final   String  name;
	protected final   int     groupCount;
	protected final   String  expect;
	protected final   Matcher matcher;
	protected boolean haveValue;
	
	protected Extract(String name, int groupCount, String pattern, String expect) {
		this.name       = name;
		this.groupCount = groupCount;
		this.expect     = expect;
		matcher         = Pattern.compile(pattern).matcher("");
		haveValue        = false;
	}
	protected Extract(String name, int groupCount, String pattern) {
		this(name, groupCount, pattern, null);
	}
	
	protected abstract String getValue(String fileName);
	
	public void reset(String fileName, String contents) {
		haveValue = expect == null || contents.contains(expect);
		if (haveValue) {
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
	}
	public String getValue(String fileName, String contents) {
		reset(fileName, contents);
		return haveValue ? getValue(fileName) : NO_VALUE;
	}
	public String getValue(int group) {
		return haveValue ? matcher.group(group) : NO_VALUE;
	}
	
	public static class Simple extends Extract {
		public Simple(String name, int groupCount, String pattern, String expect) {
			super(name, groupCount, pattern, expect);
		}
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
			add(name, groupCount, pattern, null);
		}
		public void add(String name, int groupCount, String pattern, String expect) {
			Extract extract = new Extract.Simple(name, groupCount, pattern, expect);
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