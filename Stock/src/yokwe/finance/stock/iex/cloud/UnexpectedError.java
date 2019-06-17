package yokwe.finance.stock.iex.cloud;

import yokwe.finance.stock.UnexpectedException;

@SuppressWarnings("serial")
public class UnexpectedError extends UnexpectedException {
	public UnexpectedError(String message) {
		super(message);
	}
	public UnexpectedError() {
		super();
	}
}
