package yokwe.finance.securities.stats;

public class FinStats {
	final double alpha;
	final double beta;
	final double r2;
	
	public FinStats(UniStats market, UniStats stock) {
		this(market, stock, 0.0);
	}

	public FinStats(UniStats market, UniStats stock, double interestRatesOfSafeAssets) {
		BiStats biStats = new BiStats(market, stock);
		
		beta  = biStats.covariance / market.variance;
		alpha = stock.mean - (interestRatesOfSafeAssets + beta * (market.mean - interestRatesOfSafeAssets));
		// TODO Is calculation of r-squared correct?
		r2    = biStats.covariance / (market.sd * stock.sd);
	}
	
	@Override
	public String toString() {
		return String.format("{alpha %7.4f  beta %7.4f  r2 %7.4f}", alpha, beta, r2);
	}
}
