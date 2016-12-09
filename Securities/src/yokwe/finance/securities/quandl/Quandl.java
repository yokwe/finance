package yokwe.finance.securities.quandl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class Quandl {
	private static final Logger logger = LoggerFactory.getLogger(Quandl.class);

	// https://www.quandl.com/api/v3/databases.csv?page=1&apiKey=BFLucSVjv5FBWkrSwFsH
	public static String URL_BASE = "https://www.quandl.com/api/v3/";
	
	public static String download(String target, int page, String apiKey) {
		return download(String.format("%s/%s?page=%d&apiKey=%s", URL_BASE, target, page, apiKey));
	}
	public static String download(String url) {
		StringBuilder ret = new StringBuilder();
		
		HttpGet httpGet = new HttpGet(url);
		httpGet.setHeader("User-Agent", "Mozilla");
		try (CloseableHttpClient httpClient = HttpClients.createDefault();
			CloseableHttpResponse response = httpClient.execute(httpGet)) {
			final int code = response.getStatusLine().getStatusCode();
			final String reasonPhrase = response.getStatusLine().getReasonPhrase();
			
			if (code == HttpStatus.SC_NOT_FOUND) { // 404
				logger.warn("{} {}  {}", code, reasonPhrase, url);
				return null;
			}
			if (code == HttpStatus.SC_BAD_REQUEST) { // 400
				logger.warn("{} {}  {}", code, reasonPhrase, url);
				return null;
			}
			if (code != HttpStatus.SC_OK) { // 200
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new SecuritiesException("download");
			}
			
		    HttpEntity entity = response.getEntity();
		    if (entity != null) {
		    	try (InputStreamReader isr = new InputStreamReader(entity.getContent(), "UTF-8")) {
			    	char cbuf[] = new char[64 * 1024];
			    	for(;;) {
			    		int len = isr.read(cbuf);
			    		if (len == -1) break;
			    		ret.append(cbuf, 0, len);
			    	}
		    	}
		    } else {
		    	logger.warn("entity null");
				logger.error("statusLine = {}", response.getStatusLine().toString());
				logger.error("url {}", url);
				logger.error("code {}", code);
				throw new SecuritiesException();
		    }
		} catch (UnsupportedOperationException e) {
			logger.error("UnsupportedOperationException {}", e.toString());
			throw new SecuritiesException("UnsupportedOperationException");
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
		return ret.toString();
	}

	// id,name,database_code,description,datasets_count,downloads,premium,image,favorite,url_name
	public static List<String[]> parse(String contents, String header) {
		List<String[]> ret = new ArrayList<>();
		
		String[] fields = header.split(",");
		int fieldCount = fields.length;
		
		PushbackReader pbr = new PushbackReader(new StringReader(contents));
		
		StringBuilder token = new StringBuilder();

		try {
			for(;;) {
				String[] record = new String[fieldCount];
				for(int i = 0; i < record.length; i++) {
					token.setLength(0);
					
					int firstChar = pbr.read();
					if (firstChar == -1) break; // end of stream

					if (firstChar == '"') {
						for(;;) {
							int c = pbr.read();
							if (c == -1) {
								logger.error("Unexptected EOS");
								throw new SecuritiesException("Unexptected EOS");
							}
							if (c == '"') {
								int nextChar = pbr.read();
								if (nextChar == -1) {
									logger.error("Unexptected EOS");
									throw new SecuritiesException("Unexptected EOS");
								}
								if (nextChar == '"') {
									// double quote
									token.append('"');
								} else if (nextChar == ',') {
									break; // end of token
								} else {
									logger.error("Unexptected char = {}", nextChar);
									throw new SecuritiesException("Unexptected char");
								}
							} else {
								if (' ' <= c) token.append(c);
							}
						}
					} else {
						token.append(firstChar);
						for(;;) {
							int c = pbr.read();
							if (c == -1) {
								logger.error("Unexptected EOS");
								throw new SecuritiesException("Unexptected EOS");
							}
							if (c == ',') break; // end of token
							token.append(c);
						}
					}
					record[i] = token.toString();
				}
				
				int newLine = pbr.read();
				if (newLine == -1) {
					logger.error("Unexptected EOS");
					throw new SecuritiesException("Unexptected EOS");
				} else if (newLine == '\n') {
					break;
				} else {
					logger.error("Unexptected char {}", newLine);
					throw new SecuritiesException("Unexptected char");
				}
			}
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}

		return ret;
	}
	public static <E> void parse(String contents, List<E> result) {
		
	}
}
