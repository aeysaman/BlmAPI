package gather;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;

public class QuarterData {
	int year, quarter;
	String security;
	String industry;
	double[] forward;
	double[] premium;
	List<Double> values;
	List<String> fields;
	
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
		this.industry = "nullQ";
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
	private void setTerminalPrice(int i, double terminalPrice, Double beta) {
		this.premium[i] = terminalPrice-beta;
	}
	public void setPrice(int i, double futurePrice, String ps, Double beta){
		if(this.hasFieldVal(ps)&& futurePrice!=0.0)
			this.forward[i] = (futurePrice - this.fieldToVal(ps))/this.fieldToVal(ps) - beta;
	}

	//finding future prices
	public static void calculateForwards(DataCollection dataC){
		
		List<QuarterData> lsLookup = tools.removeEndYear(dataC.quarters, dataC.endYr);
		Map<String, Map <Integer,QuarterData>> dataMap = tools.convertToMap(dataC.quarters);
		Map<String, Map <Integer,Value>> foundPrices = new HashMap<String, Map<Integer, Value>>();
		Set<String> missing = new HashSet<String>();
		
		for(int i = 0; i<dataC.forwardQrtrs.length;i++){
			int qrt = dataC.forwardQrtrs[i];
			
			Map<Integer, Double> mktPerc = tools.convertToPerc(dataC.index,qrt);
			List<QuarterData> lsTerminal = new ArrayList<QuarterData>();
			System.out.println("checking gathered data q" + qrt + " amount: " + lsLookup.size());
			//check other QuarterDatas
			for(QuarterData current: lsLookup){
				Integer futureDate = tools.iterateDate(current.getDateCode(), qrt);
				if(dataMap.get(current.security).containsKey(futureDate)){
					QuarterData future = dataMap.get(current.security).get(futureDate);
					if(future.hasFieldVal(dataC.ps))
						current.setPrice(i,future.fieldToVal(dataC.ps),dataC.ps,mktPerc.get(current.getDateCode()));
				}
				else
					lsTerminal.add(current);
			}
			List<QuarterData> lsToQuery = new ArrayList<QuarterData>();
			System.out.println("checking terminal values q" + qrt + " amount: " + lsTerminal.size());
			//check terminal values                        
			for(QuarterData current:lsTerminal){
				double terminalPrice = findTerminalPrice(current,tools.iterateDate(current.getDateCode(), qrt),dataC.terminalPrices);
				if(terminalPrice>0.0)
					current.setTerminalPrice(i,terminalPrice,mktPerc.get(current.getDateCode()));
				else
					lsToQuery.add(current);
			}
			System.out.println("querying Bloomberg q" + qrt + " looking for: " + lsToQuery.size());
			//check Bloomberg prices
			int notFoundCount= 0;
			for(QuarterData current:lsToQuery){
				Integer futureDate = tools.iterateDate(current.getDateCode(), qrt);
				double futurePrice = queryFuturePrice(current.security,futureDate,missing,foundPrices, dataC.session, dataC.service);
				if(futurePrice>0.0)
					current.setPrice(i,futurePrice,dataC.ps,mktPerc.get(current.getDateCode()));
				else
					notFoundCount++;
			}
			System.out.println("forwards not found: " + notFoundCount);
		}
		dataC.missing = missing;
	}
	
	public static Double findTerminalPrice(QuarterData x,Integer futureDate, Map<String, Value> prices){
		if(prices.containsKey(x.security)){
			if(futureDate>=prices.get(x.security).date)
				return prices.get(x.security).value;
			//System.out.println("too early for terminal value " + x.security + " a: " + futureDate + " b: " + prices.get(x.security).date);
		}
		return 0.0;
	}
	public static Double queryFuturePrice(String s,Integer futureDate, Set<String> missing, Map<String, Map<Integer, Value>> foundPrices, Session session, Service service){
		if(futureDate<=20154){
			if(foundPrices.containsKey(s)){
				if(foundPrices.get(s).containsKey(futureDate))
					return foundPrices.get(s).get(futureDate).value;
			}
			else
				foundPrices.put(s, new HashMap<Integer, Value>());
			
			Double d = DataCollection.requestPrice(s, futureDate, session, service);
			if(d!=-1.0){
				foundPrices.get(s).put(futureDate, new Value(s, futureDate,d));
				return d;
			}
			else
				missing.add(s);
		}
		return 0.0;
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
	public static List<QuarterData> transfer(Map<String, Map<Integer, QuarterData>> quarters, List<DataGroup> rawData){
		List<QuarterData> result = new LinkedList<QuarterData>();
		for(DataGroup foo: rawData){
			try{
				QuarterData bar = quarters.get(foo.security).get(tools.convertToQuarter(foo.year, foo.month));
				bar.industry = foo.industry;
				bar.enterMap(foo.values);
			}catch(NullPointerException e){
				System.out.println("error in transferring to QuarterData");
			}
		}
		for(Map<Integer, QuarterData> m : quarters.values()){
			for(QuarterData q: m.values()){
				result.add(q);
			}
		}
		return result;
	}
	public static List<QuarterData> convertDGtoQD(List<DataGroup> rawData, List<String> securities,List<String> fields, int yr){
		return transfer(generateQuarters(securities, fields, yr), rawData);
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
