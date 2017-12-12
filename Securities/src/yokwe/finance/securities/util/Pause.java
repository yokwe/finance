package yokwe.finance.securities.util;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Pause {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Pause.class);

	private long pauseTime;
	private long nextPause;
	
	private Pause(long pauseTime) {
		this.pauseTime = pauseTime;
		this.nextPause = System.currentTimeMillis() + pauseTime;
	}
	
	public void sleep() {
		long time = System.currentTimeMillis();
		if (time < nextPause) {
			try {
				Thread.sleep(nextPause - time);
			} catch (InterruptedException e) {
				logger.error("InterruptedException {}", e.toString());
				throw new SecuritiesException("InterruptedException");
			}
		}
		nextPause = time + pauseTime;
	}
	
	public static Pause getInstance(long pauseTime) {
		return new Pause(pauseTime);
	}
}
