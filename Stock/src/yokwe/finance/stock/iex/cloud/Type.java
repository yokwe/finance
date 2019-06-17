package yokwe.finance.stock.iex.cloud;

public enum Type {
	PRODUCTION(Token.PRODUCTION, "https://cloud.iexapis.com"),
	SANDBOX   (Token.SANDBOX,    "https://sandbox.iexapis.com");
	
	public final Token  token;
	public final String url;
	
	Type(Token token, String newURL) {
		this.token = token;
		this.url   = newURL;
	}
	
	@Override
	public String toString() {
		return String.format("[%s  %s  %s]", this.name(), token, url);
	}
}