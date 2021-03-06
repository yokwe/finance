package yokwe.finance.securities.stats;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.DividendTable;
import yokwe.finance.securities.database.PriceTable;

public final class Asset {
	private static final Logger logger = LoggerFactory.getLogger(Asset.class);

	public final String symbol;    // symbol
	public final double lastPrice; // last price
	public final double dividend;  // annual dividend
	public final double divYield;  // annual dividend yield (unit is percent)
	public final double price[];   // historical price
	
	public Asset (String symbol, double lastPrice, double dividend, double price[]) {
		this.symbol    = symbol;
		this.lastPrice = lastPrice;
		this.dividend  = dividend;
		this.divYield  = dividend / lastPrice;
		this.price     = price;
	}
	
	@Override
	public String toString() {
		return String.format("{%-5s %5.2f %5.3f %5.3f}", symbol, lastPrice, dividend, divYield);
	}
	
	public UniStats toLogReturnUniStats() {
		return new UniStats(DoubleArray.logReturn(price));
	}
	
	public UniStats toSimpleUniStats() {
		return new UniStats(price);
	}
	
	public static Asset getInstance(Connection connection, String symbol, LocalDate dateFrom, LocalDate dateTo) {
		List<PriceTable>    priceList    = PriceTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
		List<DividendTable> dividendList = DividendTable.getAllBySymbolDateRange(connection, symbol, dateFrom, dateTo);
		
		if (priceList == null || priceList.size() == 0) {
			logger.error("priceList is empty.  symbol = {}", symbol);
			throw new SecuritiesException("priceList is empty");
		}
		if (dividendList == null) {
			logger.error("dividendList is null.  symbol = {}", symbol);
			throw new SecuritiesException("dividendList is empty");
		}
		
		double lastPrice = priceList.get(priceList.size() - 1).close;
		double dividend = dividendList.stream().mapToDouble(o -> o.dividend).sum();
		double price[] = priceList.stream().mapToDouble(o -> o.close).toArray();
		
		return new Asset(symbol, lastPrice, dividend, price);
	}
}
