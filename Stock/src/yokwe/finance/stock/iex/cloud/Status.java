package yokwe.finance.stock.iex.cloud;

import java.time.LocalDateTime;

import javax.json.JsonObject;

public class Status extends Base {
	public static final String METHOD = "/status";
	
	public String 		 status;
	public String 		 version;
	public LocalDateTime time;
	
	public Status() {
		status  = null;
		version = null;
		time    = null;
	}
	
	public Status(JsonObject jsonObject) {
		super(jsonObject);
	}
}
