package yokwe.finance.stock.iex.cloud;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IEXCloud {
	static final Logger logger = LoggerFactory.getLogger(IEXCloud.class);
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface JSONName {
		String value();
	}
	
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface IgnoreField {
	}
	


	public static void main(String[] args) {
		logger.info("START");
		
		Context context = new Context(Type.PRODUCTION, Version.V1);
		logger.info("context = {}", context);
		
		{
			Status status = Status.getObject(context, Status.class);
			logger.info("status = {}", status);
		}
		
		{
			Account.Metadata metadata = Account.Metadata.getObject(context, Account.Metadata.class);
			logger.info("metadata = {}", metadata);
		}
		
		{
			Account.Usage usage = Account.Usage.getObject(context, Account.Usage.class);
			logger.info("usage = {}", usage);
		}
		
		{
			List<RefData.IEX.Symbols> symbols = RefData.IEX.Symbols.getArray(context, RefData.IEX.Symbols.class);
			logger.info("array symbols = {}", symbols.size());
		}
		{
			List<RefData.IEX.Symbols> symbols = RefData.IEX.Symbols.getCSV(context, RefData.IEX.Symbols.class);
			logger.info("csv   symbols = {}", symbols.size());
		}
		
		
		logger.info("STOP");
	}
}
