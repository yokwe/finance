package yokwe.finance.securities.app;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;
import yokwe.finance.securities.database.CorrelationTable;

public class PrintCorrelation {
	private static final Logger logger = LoggerFactory.getLogger(PrintCorrelation.class);
		
	static void print(Connection connection, Writer w, String[] symbols) throws IOException {
		final int SIZE = symbols.length;
		
		logger.info("symbols = {}", Arrays.asList(symbols));
		
		// Build ccMap
		final Map<Integer, double[][]> ccMap = new TreeMap<>();
		for(int indexA = 0; indexA < SIZE; indexA++) {
			final String a = symbols[indexA];
			for(int indexB = 0; indexB < SIZE; indexB++) {
				final String b = symbols[indexB];
				List<CorrelationTable> result = CorrelationTable.getAllByAB(connection, a, b);
				if (result == null) {
					logger.error("Unknown symbol pair  a = {}  b = {}", a, b);
					throw new SecuritiesException("Unknown symbol");
				}
				for(CorrelationTable table: result) {
					final Integer    month = table.month;
					final double     cc    = table.cc;
					final double[][] matrix;
					if (ccMap.containsKey(month)) {
						matrix = ccMap.get(month);
					} else {
						matrix = new double[SIZE][SIZE];
						for(int i = 0; i < SIZE; i++) {
							for(int j = 0; j < SIZE; j++) {
								matrix[i][j] = 999;
							}
						}
						ccMap.put(table.month, matrix);
					}
					matrix[indexA][indexB] = cc;
				}
			}
		}
		
		// Output ccMap
		for(Integer month: ccMap.keySet()) {
			final double[][] matrix = ccMap.get(month);
			w.append(String.format("Month = %d", month)).append("\n");
			
			// Output header
			for(int i = 0; i < SIZE; i++) {
				w.append(String.format(",%s", symbols[i]));
			}
			w.append("\n");
			
			for(int i = 0; i < SIZE; i++) {
				w.append(symbols[i]);
				for(int j = 0; j < SIZE; j++) {
					w.append(String.format(",%.2f", matrix[i][j]));
				}
				w.append("\n");
			}
			w.append("\n");
		}
		
	}
	
	private static final String OUTPUT_PATH = "tmp/printCorrelation.csv";
	public static void main(String[] args) {
		logger.info("START");
		try {
			Class.forName("org.sqlite.JDBC");
			try (Connection connection = DriverManager.getConnection("jdbc:sqlite:tmp/sqlite/correlation.sqlite3");
				BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUT_PATH))) {
//				String[] symbols = {"VCLT", "PGX", "ARR", "CLM", "NRF"};
				String[] symbols = args;
				print(connection, bw, symbols);
			}
		} catch (ClassNotFoundException | SQLException | IOException e) {
			logger.error(e.getClass().getName());
			logger.error(e.getMessage());
			e.printStackTrace();
		}
		logger.info("STOP");
	}
}
