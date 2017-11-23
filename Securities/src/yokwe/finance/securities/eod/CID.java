package yokwe.finance.securities.eod;

import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class CID {
	public String symbol;
	public String exchange;
	public String name;
	public String cid;
	
	public CID(String symbol, String exchange, String name, String cid) {
		this.symbol   = symbol;
		this.exchange = exchange;
		this.name     = name;
		this.cid      = cid;
	}
	
	@Override
	public String toString() {
		return String.format("%8s %8s  %s  %s", symbol, exchange, name, cid);
	}
	
	public boolean isEqual(CID that) {
		return this.symbol.equals(that.symbol) && this.exchange.equals(that.exchange) && this.name.equals(that.name) && this.cid.equals(that.cid);
	}
	
	public static void save(List<CID> cidList) {
		CSVUtil.saveWithHeader(cidList, UpdateCID.PATH_CID);
	}
	public static List<CID> load() {
		return CSVUtil.loadWithHeader(UpdateCID.PATH_CID, CID.class);
	}
}
