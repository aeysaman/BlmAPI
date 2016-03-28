package gather;
import java.util.HashMap;
import java.util.Map;

public class DataGroup {
	int year, month, day;
	String security;
	String industry;
	Map<String, Double> values;
	
	public DataGroup(String security){
		this.year = -1;
		this.month = -1;
		this.day = -1;
		this.security = security;
		this.industry = "nullD";
		this.values = new HashMap<String, Double>();
	}
	
	public void enterData(String field, String x){
		if(field.equals("date")){
			String[] bar = x.split("-");
			this.year = Integer.parseInt(bar[0]);
			this.month = Integer.parseInt(bar[1]);
			this.day = Integer.parseInt(bar[2]);
		}
		else{
			values.put(field, Double.parseDouble(x));
		}
	}
}
