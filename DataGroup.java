package api;
import general.Date;
import general.Datum;

public class DataGroup extends Datum{
	final String price ="PX_LAST";
	Value px;
	
	public DataGroup(String name){
		super(name, null);
		this.px =null;
	}
	public void enterData(String field, String x){
		if(field.equals("date"))
			date = Date.parseDate(x, "yyyy-mm-dd");
		else
			enterValue(field, Double.parseDouble(x));
	}
	//initializes px object and removes Price from the list
	public void pxGen(){
		if(hasValue(price)){
			px = new Value(name, getValue(price), date);
			removeValue(price);
		}
	}
	public boolean hasPx(){
		return px!=null;
	}
}
