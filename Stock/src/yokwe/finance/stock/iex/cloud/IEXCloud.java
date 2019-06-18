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
		
//		{
//			List<RefData.IEX.Symbols> symbols = RefData.IEX.Symbols.getArray(context, RefData.IEX.Symbols.class);
//			logger.info("array symbols = {}", symbols.size());
//			
//			RefData.IEX.Symbols.saveCSV(symbols);
//		}
		{
			List<RefData.IEX.Symbols> iexSymbols = RefData.IEX.Symbols.getCSV(context, RefData.IEX.Symbols.class);
			logger.info("iexSymbols = {}", iexSymbols.size());
			
			RefData.IEX.Symbols.saveCSV(iexSymbols);
		}

//		{
//			List<RefData.Symbols> symbols = RefData.Symbols.getCSV(context, RefData.Symbols.class);
//			logger.info("symbols = {}", symbols.size());
//			
//			RefData.Symbols.saveCSV(symbols);
//		}

//		{
//			List<RefData.OTC.Symbols> otcSymbols = RefData.OTC.Symbols.getCSV(context, RefData.OTC.Symbols.class);
//			logger.info("otcSymbols = {}", otcSymbols.size());
//			
//			RefData.OTC.Symbols.saveCSV(otcSymbols);
//		}

//		{
//			List<RefData.Exchanges> exchanges = RefData.Exchanges.getCSV(context, RefData.Exchanges.class);
//			logger.info("exchanges = {}", exchanges.size());
//			
//			RefData.Exchanges.saveCSV(exchanges);
//		}

//		{
//			List<RefData.Market.US.Exchanges> usExchanges = RefData.Market.US.Exchanges.getCSV(context, RefData.Market.US.Exchanges.class);
//			logger.info("usExchanges = {}", usExchanges.size());
//			
//			RefData.Market.US.Exchanges.saveCSV(usExchanges);
//		}

//		{
//			List<RefData.MutualFunds.Symbols> mfSymbols = RefData.MutualFunds.Symbols.getCSV(context, RefData.MutualFunds.Symbols.class);
//			logger.info("mfSymbols = {}", mfSymbols.size());
//			
//			RefData.MutualFunds.Symbols.saveCSV(mfSymbols);
//		}

		logger.info("STOP");
	}
}
