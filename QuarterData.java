package api;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import general.Date;
import general.Datum;
public class QuarterData extends Datum{
	double[] forward;
	double[] premium;
	//List<Double> values;
	List<String> fields;
	Value px;
	
	public QuarterData(List<String> fields, String security, int year, int quarter){
		super(security, new Date(year, quarter));
		this.fields = fields;
		this.forward = new double[3];
		for(int i = 0; i<this.forward.length;i++)
			this.forward[i] = -2;
		this.premium = new double[3];
		for(int i = 0; i<this.premium.length;i++)
			this.premium[i] = 0;
		this.fields = fields;
	}
	public int missingCount(){
		return missingCount(fields);
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
				QuarterData bar = quarters.get(foo.name).get(foo.date.getQuarter());
				bar.enterMap(foo.data);
				if(foo.hasPx())
					bar.px = foo.px;
			}catch(NullPointerException e){
				System.out.println("error in transferring to QuarterData");
			}
		}
		for(Map<Integer, QuarterData> m : quarters.values()){
			for(QuarterData q: m.values()){
				if(q.px==null)
					System.out.println("missing price: " + q.name +" " + q.date.toString());
				else
					result.add(q);
			}
		}
		return result;
	}
	public String export(){
		return String.join(",", name, date.toString(), ""+date.getDateNumQrtDouble(), exportReturns(this.forward), exportPremiums(this.premium), exportDataJoined(fields));
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
}
