package yokwe.finance.securities.eod;

import java.util.List;

import yokwe.finance.securities.util.CSVUtil;

public class Preferred {
	public String symbol;
	public String type;
	public String parent;
	public String country;
	
	public String cpnRate;
	public String annAmt;
	public String liqPref;
	public String remark;
	public String callPrice;
	public String callDate;
	public String maturDate;
	
	public String name;
	
	public Preferred(String symbol, String type, String parent, String country, String name,
			String cpnRate, String annAmt, String liqPref, String remark, String callPrice, String callDate, String maturDate
			) {
		this.symbol  = symbol;
		this.type    = type;
		this.parent  = parent;
		this.country = country;
		this.name    = name;
		
		this.cpnRate   = cpnRate;
		this.annAmt    = annAmt;
		this.liqPref   = liqPref;
		this.remark    = remark;
		this.callPrice = callPrice;
		this.callDate  = callDate;
		this.maturDate = maturDate;
	}
	
	@Override
	public String toString() {
		return String.format("%-8s  %20s  %-8s  %s", symbol, type, parent, country);
	}
	
	public static List<Preferred> load() {
		return CSVUtil.loadWithHeader(UpdatePreferred.PATH_FILE, Preferred.class);
	}
	
	public static void save(List<Preferred> preferredList) {
		CSVUtil.saveWithHeader(preferredList, UpdatePreferred.PATH_FILE);
	}

}
