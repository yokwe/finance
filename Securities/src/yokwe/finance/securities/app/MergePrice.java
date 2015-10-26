package yokwe.finance.securities.app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class MergePrice {
	private static final Logger logger = LoggerFactory.getLogger(MergePrice.class);
	
	public static void main(String[] args) {
		logger.info("START");
		String pathA = args[0];
		String pathB = args[1];
		String pathC = args[2];
		logger.info("pathA = {}", pathA);
		logger.info("pathB = {}", pathB);
		logger.info("pathC = {}", pathC);
		
		try {
			merge(pathA, pathB, pathC);
		} catch (SecuritiesException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		
		logger.info("STOP");
	}
	private static final int BUFFER_SIZE = 1024 * 256;
	
	private static final class PriceReader implements Closeable, Comparable<PriceReader> {
		private BufferedReader br;
		private String         line;
		private String         dateSymbol;
		
		PriceReader(String path) {
			try {
				File file = new File(path);
				if (!file.isFile()) {
					logger.error("Is not a file.  path = {}", path);
					throw new SecuritiesException("file");
				}
				
				br      = new BufferedReader(new FileReader(file), BUFFER_SIZE);
				line    = "";
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException("");
			}
		}
		
		@Override
		public void close() {
			try {
				br.close();
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException("IOException");
			}
		}
		
		String readLine() throws IOException {
			line = br.readLine();
			
			// special case
			if (line == null) {
				dateSymbol = null;
				return null;
			}
			
			int t0 = line.indexOf(',', 0);
			int t1 = line.indexOf(',', t0);
			
			dateSymbol = line.substring(0, t1);
			return line;
		}
		
		@Override
		public int compareTo(PriceReader that) {
			return compareToString(this.dateSymbol, that.dateSymbol);
		}
		
		private static int compareToString(String a, String b) {
			if (a == null && b == null) {
				logger.error("null");
				throw new SecuritiesException("null");
			}
			
			if (a == null) {
				return -1;
			} else if (b == null) {
				return 1;
			} else {
				return a.compareTo(b);
			}
		}
	}
	
	private static final class PriceWriter implements Closeable {
		private static final String NL = "\n";
		
		final BufferedWriter bw;
		
		PriceWriter(String path) {
			try {
				File file = new File(path);
				if (!file.isFile()) {
					logger.error("Is not a file.  path = {}", path);
					throw new SecuritiesException("file");
				}
				
				bw = new BufferedWriter(new FileWriter(file), BUFFER_SIZE);
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException("");
			}
		}
		
		@Override
		public void close() {
			try {
				bw.close();
			} catch (IOException e) {
				logger.error(e.getClass().getName());
				logger.error(e.getMessage());
				throw new SecuritiesException("IOException");
			}
		}
		
		void write(String line) throws IOException {
			bw.write(line);
			bw.write(NL);
		}
	}
	
	private static void merge(String pathA, String pathB, String pathC) {
		try (
			PriceReader inA = new PriceReader(pathA);
			PriceReader inB = new PriceReader(pathB);
			PriceWriter out = new PriceWriter(pathC);
			) {
			String lineA = inA.readLine();
			String lineB = inB.readLine();			
			for(;;) {
				if (lineA == null) break;
				if (lineB == null) break;
				
				int c = inA.compareTo(inB);
				if (c == 0) {
					lineB = inB.readLine();
					continue;
				}
				if (c < 0) {
					out.write(lineA);
					lineA = inA.readLine();
					continue;
				} else {
					out.write(lineB);
					lineB = inB.readLine();
				}
			}
			if (lineA != null) {
				for(;;) {
					if (lineA == null) break;
					out.write(lineA);
					lineA = inA.readLine();
				}
			}
			if (lineB != null) {
				for(;;) {
					if (lineB != null) break;
					out.write(lineB);
					lineB = inB.readLine();
				}
			}
		} catch (IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			throw new SecuritiesException("");
		}
	}
}
