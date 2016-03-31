package gather;
import java.util.HashMap;
import java.util.Map;

public class DataGroup {
	final String price ="PX_LAST";
	
	int year, month, day;
	String security;
	Map<String, Double> values;
	Value px;
	
	public DataGroup(String security){
		this.year = -1;
		this.month = -1;
		this.day = -1;
		this.security = security;
		this.values = new HashMap<String, Double>();
		this.px =null;
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
	//initializes px object and removes Price from the list
	public void pxGen(){
		if(values.containsKey(price)){
			px = new Value(security,tools.convertToQuarter(year, month), values.get(price));
			values.remove(price);
		}
	}
	public boolean hasPx(){
		return px!=null;
	}
}
