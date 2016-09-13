package api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import general.Tools;
public class Pricing {
	public static void setReturn(QuarterData x, Value current, Value forward, DataCollection dataC, int i) {
		double rawRtn = (forward.value - current.value) / current.value;
		
		Value currentIndex = getIndexPrice(current, dataC);
		Value forwardIndex = getIndexPrice(forward, dataC);
		double benchmark = (forwardIndex.value - currentIndex.value) / currentIndex.value;
		
		x.forward[i] = rawRtn - benchmark;
		
		System.out.printf("Return for %s: %f - %f from %s to %s\n", x.name, rawRtn, benchmark, current.date.toString(), forward.date.toString());
	}
	public static Value getIndexPrice(Value x, DataCollection dataC){
		for(int i = 0; i<10; i++){
			if(dataC.index.containsKey(x.date.iterateDownDateString(i)))
				return dataC.index.get(x.date.iterateDownDateString(i));
		}
		return null;
	}
	public static List<QuarterData> checkOtherQD(List<QuarterData> ls, Map<String, Map <Integer,QuarterData>> dataMap, DataCollection dataC, int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current: ls){
			Integer futureDate = Tools.iterateDate(current.getDateNumQrt(), qrt);
			if(dataMap.get(current.name).containsKey(futureDate)){
				QuarterData future = dataMap.get(current.name).get(futureDate);
				setReturn(current, current.px,future.px, dataC, i);
			}
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static List<QuarterData> checkTerminals(List<QuarterData> ls, DataCollection dataC, int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current:ls){
			Double terminalPrice = findTerminalPrice(current,Tools.iterateDate(current.getDateNumQrt(), qrt),dataC.terminalPrices);
			if(terminalPrice!=null)
				current.setTerminalPrice(i,terminalPrice);
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static Double findTerminalPrice(QuarterData x,Integer futureDate, Map<String, Value> prices){
		if(prices.containsKey(x.name)){
			if(futureDate>=prices.get(x.name).quarter)
				return prices.get(x.name).value;
		}
		return null;
	}
	public static List<QuarterData> checkBloomberg(List<QuarterData> ls, DataCollection dataC, Map<String,Map<Integer, Value>> foundPrices, int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current:ls){
			Integer futureDate = Tools.iterateDate(current.getDateNumQrt(), qrt);
			Value future = queryFuturePrice(current.name,futureDate,foundPrices, dataC.session, dataC.service);
			if(future !=null){
				setReturn(current, current.px, future, dataC, i);
			}
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static Value queryFuturePrice(String s,Integer futureDate, Map<String, Map<Integer, Value>> foundPrices, Session session, Service service){
		if(futureDate<=20154){
			Value v = Api.requestPrice(s, futureDate, session, service);
			if(v!=null){
				foundPrices.get(s).put(futureDate, v);
				return v;
			}
		}
		return null;
	}
	public static List<QuarterData> checkExisting(List<QuarterData> ls, DataCollection dataC, Map<String, Map <Integer,Value>> foundPrices ,int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current:ls){
			Integer futureDate = Tools.iterateDate(current.getDateNumQrt(), qrt);
			
			if(!foundPrices.containsKey(current.name))
				foundPrices.put(current.name, new HashMap<Integer, Value>());
			
			if(foundPrices.get(current.name).containsKey(futureDate))
				setReturn(current, current.px ,foundPrices.get(current.name).get(futureDate), dataC, i);
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static void calculateForwards(DataCollection dataC){
		Map<String, Map <Integer,QuarterData>> dataMap = Tools.convertToMap(dataC.quarters);
		Map<String, Map <Integer,Value>> foundPrices = new HashMap<String, Map<Integer, Value>>();
		Set<String> missing = new HashSet<String>();
		
		List<QuarterData> lsLookup = Tools.removeEndYear(dataC.quarters, dataC.endYr);
		for(int i = 0; i<dataC.forwardQrtrs.length;i++){
			int qrt = dataC.forwardQrtrs[i];
			
			//Map<Integer, Double> mktPerc = Tools.convertToPerc(dataC.index,qrt);
			
			System.out.println("checking gathered data q" + qrt + " amount: " + lsLookup.size());
			List<QuarterData> lsTerminal = checkOtherQD(lsLookup, dataMap, dataC, qrt, i);
			
			System.out.println("checking terminal values q" + qrt + " amount: " + lsTerminal.size());
			List<QuarterData> lsExisting = checkTerminals(lsTerminal, dataC, qrt, i);
			
			System.out.println("checking existing list q" + qrt + " amount: " + lsExisting.size());
			List<QuarterData> lsQuery = checkExisting(lsExisting, dataC, foundPrices, qrt, i);
			
			System.out.println("querying Bloomberg q" + qrt + " looking for: " + lsQuery.size());
			List<QuarterData> notFound = checkBloomberg(lsQuery, dataC, foundPrices, qrt, i);
			
			System.out.println("forwards not found: " + notFound.size());
		}
		dataC.missing = missing;
	}
	
	
}