package yokwe.finance.etf;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.LoggerFactory;

public abstract class Extract {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Extract.class);

	protected final String  name;
	protected final int     count;
	protected final Matcher matcher;
	
	protected Extract(String name, int count, String pattern) {
		this.name  = name;
		this.count = count;
		matcher    = Pattern.compile(pattern, Pattern.MULTILINE).matcher("");
	}
	
	protected abstract String getValue(String fileName);
	
	public String getValue(String fileName, String contents) {
		matcher.reset(contents);
		if (!matcher.find()) {
			logger.error("{}  NAME {}", fileName, name);
			logger.error("pat {}", matcher.toString());
			throw new RuntimeException(name);
		}
		if (matcher.groupCount() != count) {
			logger.error("{}  COUNT {}  groupCount = {}", fileName, name, matcher.groupCount());
			throw new RuntimeException("COUNT");
		}
		
		return getValue(fileName);
	}
	public String getValue(int group) {
		return matcher.group(group);
	}
	
	public static class Simple extends Extract {
		public Simple(String name, String pattern) {
			super(name, 1, pattern);
		}
		public Simple(String name, int count, String pattern) {
			super(name, count, pattern);
		}
		protected String getValue(String fileName) {
			return matcher.group(1);
		}
	}
	
	public static class MMDDYY extends Extract {
		public MMDDYY(String name, String pattern) {
			super(name, 3, pattern);
		}
		protected String getValue(String fileName) {
			String mm = matcher.group(1);
			String dd = matcher.group(2);
			String yy = matcher.group(3);

			return String.format("20%s-%s-%s", yy, mm, dd);
		}
	}
	
}