package yokwe.finance.stock.iex.cloud;

public enum Version {
	V1    ("v1"),
	BETA  ("beta"),
	STABLE("stable"),
	LATEST("latest");
	
	public final String url;
	
	Version(String newValue) {
		url = newValue;
	}
	
	@Override
	public String toString() {
		return String.format("[%s  %s]", name(), url);
	}
}