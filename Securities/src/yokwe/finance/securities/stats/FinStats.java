package yokwe.finance.securities.stats;

public final class FinStats {
	public static final double MIN_R2_FOR_BETA = 0.7;
	
	public final double   alpha;
	public final double   beta;
	public final double   r2;
	public final UniStats stock;
	
	public FinStats(UniStats market, Asset stock) {
		this(market, stock.toLogReturnUniStats(), 0.0);
	}

	public FinStats(UniStats market, UniStats stock, double interestRatesOfSafeAssets) {
		BiStats biStats = new BiStats(market, stock);
		
		beta  = biStats.covariance / market.variance;
		alpha = stock.mean - (interestRatesOfSafeAssets + beta * (market.mean - interestRatesOfSafeAssets));
		r2    = biStats.correlation * biStats.correlation;
		this.stock = stock;
	}
	
	public double getBeta() {
		return (MIN_R2_FOR_BETA <= r2) ? beta : 0;
	}
	
	@Override
	public String toString() {
		return String.format("{alpha %7.4f  beta %5.2f  r2 %4.2f}", alpha, beta, r2);
	}
}
