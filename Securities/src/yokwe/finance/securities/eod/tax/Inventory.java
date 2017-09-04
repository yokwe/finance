package yokwe.finance.securities.eod.tax;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import yokwe.finance.securities.eod.report.Position;
import yokwe.finance.securities.util.DoubleUtil;

public class Inventory {
//	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Inventory.class);
	
	private Map<String, Asset> assetMap = new TreeMap<>();
	
	
	private Asset get(String symbol) {
		Asset asset;
		if (assetMap.containsKey(symbol)) {
			asset = assetMap.get(symbol);
		} else {
			asset = new Asset(symbol);
			assetMap.put(symbol, asset);
		}

		return asset;
	}
	
	public void buy(String date, String symbol, double quantity, double price, double fee) {
		Asset asset = get(symbol);		
		asset.buy(date, symbol, quantity, price, fee);
	}
	
	public void sell(String date, String symbol, double quantity, double price, double fee) {
		Asset asset = get(symbol);		
		asset.sell(date, symbol, quantity, price, fee);
	}
	
	public void change(String date, String symbol, double quantity, String symbolNew, double quantityNew) {
		Asset asset = get(symbol);		
		asset.change(date, symbol, quantity, symbolNew, quantityNew);
	}
	
	public List<Position> getPositionList() {
		List<Position> ret = new ArrayList<>();
		
		for(Asset asset: assetMap.values()) {
			if (asset.quantityTotal == 0) continue;
			
			ret.add(new Position(asset.symbol, asset.quantityTotal));
		}
		return ret;
	}
	
	public double getStockTotal() {
		double ret = 0;
		for(Asset asset: assetMap.values()) {
			ret = DoubleUtil.round(ret + asset.costTotal, Asset.DIGIT_COST);
		}
		return ret;
	}
}
