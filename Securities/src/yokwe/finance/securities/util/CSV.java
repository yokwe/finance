package yokwe.finance.securities.util;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import yokwe.finance.securities.SecuritiesException;

public class CSV {
	private static final Logger logger = LoggerFactory.getLogger(CSV.class);

	public static List<List<String>> parse(String content) {
		return parse(new StringReader(content));
	}
	
	public static List<List<String>> parse(Reader r) {
		try (PushbackReader pr = new PushbackReader(r)) {
			List<List<String>> ret = new ArrayList<>();
			List<String> record = new ArrayList<>();
			StringBuilder value = new StringBuilder();

			for (;;) {
				int firstChar = pr.read();
				
				if (firstChar == -1) {
					break;
				} else if (firstChar == ',') {
					// end of value
					record.add(value.toString());
					// prepare for next iteration
					value.setLength(0);
				} else if (firstChar == '\n') {
					// end of value
					record.add(value.toString());
					// end of record
					ret.add(record);
					// prepare for next iteration
					value.setLength(0);
					record = new ArrayList<>();
				} else if (firstChar == '"') {
					// start quoted value
					for(;;) {
						int c = pr.read();
						if (c == -1) {
							logger.warn("Unexpected EOF");
							throw new SecuritiesException("Unexpected EOF");
						}
						if (c == '"') {
							int nextChar = pr.read();
							if (nextChar == -1) {
								logger.warn("Unexpected EOF");
								throw new SecuritiesException("Unexpected EOF");
							}
							
							if (nextChar == ',' || nextChar == '\n') {
								// end of value
								pr.unread(nextChar);
								break;
							}
								
							if (nextChar == '"') {
								// two double quote is one double quote
								value.append('"');
							} else {
								logger.warn("Unexptected char after closing double qoute {}", String.format("%02x", c));
								throw new SecuritiesException("Unexptected char after closing double qoute");
							}
							
						}
						
						// skip control character
						if (c < ' ') {
							logger.warn("skip char {}", String.format("%02x", c));
						} else {
							value.append((char)c);
						}
					}
				} else {
					value.append((char)firstChar);
					for(;;) {
						int c = pr.read();
						if (c == -1) {
							logger.warn("Unexpected EOF");
							throw new SecuritiesException("Unexpected EOF");
						}
						if (c == ',' || c == '\n') {
							// end of value
							pr.unread(c);
							break;
						}
						
						// skip control character
						if (c < ' ') {
							logger.warn("skip char {}", String.format("%02x", c));
						} else {
							value.append((char)c);
						}
					}
				}
			}
			
			return ret;
		} catch (IOException e) {
			logger.error("IOException {}", e.toString());
			throw new SecuritiesException("IOException");
		}
	}
	
	public static void main(String[] args) {
		logger.info("{}", parse("a,b,c,d\n"));
		logger.info("{}", parse("aa,bb,cc,dd\n"));
		logger.info("{}", parse(",bb,cc,dd\n"));
		logger.info("{}", parse("aa,,cc,dd\n"));
		logger.info("{}", parse("aa,bb,cc,\n"));
		logger.info("{}", parse("aa,,,cc\n"));
		logger.info("{}", parse(",,,\n"));
		
		logger.info("{}", parse("a,b,c,d\ne,f,g,h\n"));
		
		logger.info("{}", parse("a,\"b,c\",d\n"));
		logger.info("{}", parse("a,\"b,c\",\n"));
		logger.info("{}", parse("a,\"b,c\",\"\"\n"));
		logger.info("{}", parse("a,\"b,c\"\n"));
		logger.info("{}", parse("a,\"\"\"\",\n"));
		logger.info("{}", parse("a,\"\"\"\"\"\",\n"));
		logger.info("{}", parse("a,\"\"\"\"\"\",\"\"\n"));

	}

}
