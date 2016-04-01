package gather;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class QuarterData {
	int year, quarter;
	String security;
	double[] forward;
	double[] premium;
	List<Double> values;
	List<String> fields;
	Value px;
	
	public QuarterData(List<String> fields, String security, int year, int quarter){
		this.fields = fields;
		this.security = security;
		this.year = year;
		this.quarter = quarter;
		this.forward = new double[3];
		for(int i = 0; i<this.forward.length;i++)
			this.forward[i] = -2;
		this.premium = new double[3];
		for(int i = 0; i<this.premium.length;i++)
			this.premium[i] = 0;
		this.values = new ArrayList<Double>();
		for(int i = 0; i<fields.size(); i++)
			this.values.add(null);
		this.fields = fields;
	}
	//non-Static methods
	public int missingCount() {
		int count = 0;
		for(Double s: values){
			if(s == null)
				count++;
		}
		return count;
	}
	public String exportDate(){
		return year + "/" + ((quarter-1)*3+1) + "/01";
	}
	public Integer getDateCode(){
		return year*10 + quarter;
	}
	private void enterMap(Map<String, Double> x) {
		for(String f : x.keySet())
			values.set(fields.indexOf(f), x.get(f));
	}
	public double fieldToVal(String field){
		return this.values.get(this.fields.indexOf(field));
	}
	public boolean hasFieldVal(String field){
		if(this.values.get(this.fields.indexOf(field))==null)
			return false;
		return true;
	}
	public void setTerminalPrice(int i, double terminalPrice) {
		this.premium[i] = terminalPrice;
	}
	//Entering Fundamentals
	public static Map<String, Map<Integer, QuarterData>> generateQuarters(List<String> securities,List<String> fields, int yr){
		Map<String, Map<Integer, QuarterData>> result = new HashMap<String, Map<Integer, QuarterData>>();
		for(String sec: securities){
			Map<Integer, QuarterData> foo = new HashMap<Integer, QuarterData>();
			for(int q = 1; q<=4; q++)
				foo.put((yr*10 + q), new QuarterData(fields,sec,yr,q));
			result.put(sec, foo);
		}
		return result;
	}
	public static List<QuarterData> convertDGtoQD(List<DataGroup> rawData, List<String> securities,List<String> fields, int yr){
		Map<String, Map<Integer, QuarterData>> quarters = generateQuarters(securities, fields, yr);
		
		for(DataGroup d: rawData)
			d.pxGen();
		
		List<QuarterData> result = new LinkedList<QuarterData>();
		for(DataGroup foo: rawData){
			try{
				QuarterData bar = quarters.get(foo.security).get(tools.convertToQuarter(foo.year, foo.month));
				bar.enterMap(foo.values);
				if(foo.hasPx())
					bar.px = foo.px;
			}catch(NullPointerException e){
				System.out.println("error in transferring to QuarterData");
			}
		}
		for(Map<Integer, QuarterData> m : quarters.values()){
			for(QuarterData q: m.values()){
				if(q.px==null)
					System.out.println("missing price: " + q.security +" " + q.exportDate());
				else
					result.add(q);
			}
		}
		return result;
	}
	//exporting
	public String export(){
		return security + "," + exportDate() + "," + exportDateNum() + "," + exportReturns(this.forward) + ","+ exportPremiums(this.premium) + "," + tools.joinListD(values);
	}
	public static String exportReturns(double[] bar){
		String[] foo = {"null", "null", "null"};
		for(int i = 0; i<3;i++){
			if(bar[i] != -2)
				foo[i] = Double.toString(bar[i]);
		}
		return foo[0] + "," + foo[1] + "," + foo[2];
	}
	public static String exportPremiums(double[] bar){
		return bar[0] + "," + bar[1] + "," + bar[2];
	}
	private String exportDateNum() {
		double d = (double)year +((double)quarter-1)/4;
		return "" + d;
	}
}
