package yokwe.finance.stock;

@SuppressWarnings("serial")
public class UnexpectedException extends StockException {
	public UnexpectedException(String message) {
		super(message);
	}
	public UnexpectedException() {
		super();
	}
}
