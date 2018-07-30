package yokwe.finance.securities.iex;

import yokwe.finance.securities.SecuritiesException;

@SuppressWarnings("serial")
public class IEXUnexpectedError extends SecuritiesException {
	public IEXUnexpectedError(String message) {
		super(message);
	}
	public IEXUnexpectedError() {
		super();
	}
}
