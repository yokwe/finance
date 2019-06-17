package yokwe.finance.stock.iex.cloud;

public class Context {
	public final Type   type;
	public final Version version;
	public final String  url;
	
	public Context(Type type, Version version) {
		this.type    = type;
		this.version = version;
		this.url = String.format("%s/%s", type.url, version.url);
	}
	
	@Override
	public String toString() {
		return String.format("%s %s  %s", type.toString(), version.toString(), url);
	}
	
	public String getURL(String method) {
		return String.format("%s%s?token=%s", url, method, type.token.secret);
	}
}