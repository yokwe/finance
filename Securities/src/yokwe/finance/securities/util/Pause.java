package yokwe.finance.securities.util;

import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Pause {
	private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Pause.class);

	private long pauseTime;
	private long nextPause;
	
	private Pause(long pauseTime) {
		long time = System.currentTimeMillis();

		this.pauseTime = pauseTime;
		this.nextPause = time + pauseTime;
	}
	
	public void reset() {
		long time = System.currentTimeMillis();
		
		nextPause = time + pauseTime;
	}
	public void sleep() {
		long time = System.currentTimeMillis();
		long waitTime = nextPause - time;
		
		if (0 < waitTime) {
//			logger.debug("sleep waitTime = {}", waitTime);
			try {
				Thread.sleep(waitTime);
			} catch (InterruptedException e) {
				logger.error("InterruptedException {}", e.toString());
				throw new SecuritiesException("InterruptedException");
			}
			nextPause = nextPause + pauseTime;
		} else if (-waitTime < pauseTime) {
//			logger.debug("skip  waitTime = {}", waitTime);
			nextPause = nextPause + pauseTime;
		} else {
			logger.debug("reset waitTime = {}", waitTime);
			reset();
		}
	}
	
	public static Pause getInstance(long pauseTime) {
		return new Pause(pauseTime);
	}
}
