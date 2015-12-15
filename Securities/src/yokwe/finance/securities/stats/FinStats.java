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
		r2    = biStats.correlation * biStats.correlation;
	}
	
	@Override
	public String toString() {
		return String.format("{alpha %7.4f  beta %5.2f  r2 %4.2f}", alpha, beta, r2);
	}
}
