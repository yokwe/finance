package yokwe.finance.stock.iex.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.stock.UnexpectedException;
import yokwe.finance.stock.util.FileUtil;

public class Token {
	static final Logger logger = LoggerFactory.getLogger(Token.class);

	public static final Token PRODUCTION;
	public static final Token SANDBOX;
	
	static {
		final String PATH_TOKEN = "tmp/iex/token";

		final String PREFIX_COMMENT            = "#";
		final String PREFIX_PRODUCTION_PUBLISH = "pk_";
		final String PREFIX_PRODUCTION_SECRET  = "sk_";
		final String PREFIX_SANDBOX_PUBLISH    = "Tpk_";
		final String PREFIX_SANDBOX_SECRET     = "Tsk_";
		
		String productionPublish = null;
		String productionSecret  = null;
		String sandboxPublish    = null;
		String sandobxSecret     = null;
		
		String content = FileUtil.read(PATH_TOKEN);
		for(String line: content.split("[\\n\\r]+")) {
			line = line.trim();
			if (line.length() == 0) continue;
			
			if (line.startsWith(PREFIX_COMMENT)) {
				continue;
			} else if (line.startsWith(PREFIX_PRODUCTION_PUBLISH)) {
				productionPublish = line;
			} else if (line.startsWith(PREFIX_PRODUCTION_SECRET)) {
				productionSecret = line;
			} else if (line.startsWith(PREFIX_SANDBOX_PUBLISH)) {
				sandboxPublish = line;
			} else if (line.startsWith(PREFIX_SANDBOX_SECRET)) {
				sandobxSecret = line;
			} else {
				logger.error("Unexpected line = {}!", line);
				throw new UnexpectedException("Unexpected line");
			}
		}
		
		// Sanity check
		if (productionPublish == null) {
			logger.error("no productionPublish");
			throw new UnexpectedException("no productionPublish");
		}
		if (productionSecret == null) {
			logger.error("no productionSecret");
			throw new UnexpectedException("no productionSecret");
		}
		if (sandboxPublish == null) {
			logger.error("no sandboxPublish");
			throw new UnexpectedException("no sandboxPublish");
		}
		if (sandobxSecret == null) {
			logger.error("no productionSecret");
			throw new UnexpectedException("no sandobxSecret");
		}
		
		PRODUCTION = new Token(productionPublish, productionSecret);
		SANDBOX    = new Token(sandboxPublish,    sandobxSecret);
	}
	
	public final String publishable;
	public final String secret;
	
	private Token(String publishable, String secret) {
		this.publishable = publishable;
		this.secret      = secret;
	}
	
	@Override
	public String toString() {
		return String.format("[%s  %s]", publishable, secret);
	}
}