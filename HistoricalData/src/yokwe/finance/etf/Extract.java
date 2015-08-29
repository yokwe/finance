package yokwe.finance.etf;

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
		matcher         = Pattern.compile(pattern, Pattern.MULTILINE).matcher("");
	}
	
	protected abstract String getValue(String fileName);
	
	public String getValue(String fileName, String contents) {
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
}