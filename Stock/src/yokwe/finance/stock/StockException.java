package yokwe.finance.stock;

@SuppressWarnings("serial")
public class StockException extends RuntimeException {
	public StockException(String message) {
		super(message);
	}
	public StockException() {
		super();
	}
}
