package yokwe.finance.securities.quandl;

public class Database {
	// id,name,database_code,description,datasets_count,downloads,premium,image,favorite,url_name
	public int     id; // int
	public String  name;
	public String  database_code;
	public String  description;
	public int     dataset_count; // int
	public int     downloads; // int
	public boolean premium; // boolean
	public String  image;
	public boolean favorite; // boolean
	public String  url_name;
}
