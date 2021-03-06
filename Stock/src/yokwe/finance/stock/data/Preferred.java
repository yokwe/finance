package yokwe.finance.stock.data;

import java.util.List;

import yokwe.finance.stock.app.UpdatePreferred;
import yokwe.finance.stock.util.CSVUtil;

public class Preferred implements Comparable<Preferred> {
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
	public String distDates;
	public String ipo;
	
	public String name;
	
	public Preferred(String symbol, String type, String parent, String country, String name,
			String cpnRate, String annAmt, String liqPref, String remark, String callPrice, String callDate, String maturDate, String distDates, String ipo
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
		this.distDates = distDates;
		this.ipo       = ipo;
	}
	
	@Override
	public String toString() {
		return String.format("%-8s  %20s  %-8s  %s", symbol, type, parent, country);
	}
	
	@Override
	public int compareTo(Preferred that) {
		return this.symbol.compareTo(that.symbol);
	}

	public static List<Preferred> load() {
		return CSVUtil.loadWithHeader(UpdatePreferred.PATH_FILE, Preferred.class);
	}
	
	public static void save(List<Preferred> preferredList) {
		CSVUtil.saveWithHeader(preferredList, UpdatePreferred.PATH_FILE);
	}
}
