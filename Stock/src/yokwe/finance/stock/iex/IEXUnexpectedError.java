package yokwe.finance.stock.iex;

import yokwe.finance.stock.UnexpectedException;

@SuppressWarnings("serial")
public class IEXUnexpectedError extends UnexpectedException {
	public IEXUnexpectedError(String message) {
		super(message);
	}
	public IEXUnexpectedError() {
		super();
	}
}
