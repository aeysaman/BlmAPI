package api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;

public class Pricing {
	public static void setReturn(QuarterData x, Value current, Value forward, DataCollection dataC, int i) {
		double rawRtn = (forward.value - current.value) / current.value;
		
		Value currentIndex = getIndexPrice(current, dataC);
		Value forwardIndex = getIndexPrice(forward, dataC);
		double benchmark = (forwardIndex.value - currentIndex.value) / currentIndex.value;
		
		x.forward[i] = rawRtn - benchmark;
		
		System.out.println("Return for " + x.security + ": " + rawRtn + " - " + benchmark + " from "+ current.dateString() + " to " + forward.dateString());
	}
	public static Value getIndexPrice(Value x, DataCollection dataC){
		for(int i = 0; i<10; i++){
			if(dataC.index.containsKey(x.iterateDownDateString(i)))
				return dataC.index.get(x.iterateDownDateString(i));
		}
		return null;
	}
	public static List<QuarterData> checkOtherQD(List<QuarterData> ls, Map<String, Map <Integer,QuarterData>> dataMap, DataCollection dataC, int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current: ls){
			Integer futureDate = tools.iterateDate(current.getDateCode(), qrt);
			if(dataMap.get(current.security).containsKey(futureDate)){
				QuarterData future = dataMap.get(current.security).get(futureDate);
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
			Double terminalPrice = findTerminalPrice(current,tools.iterateDate(current.getDateCode(), qrt),dataC.terminalPrices);
			if(terminalPrice!=null)
				current.setTerminalPrice(i,terminalPrice);
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static Double findTerminalPrice(QuarterData x,Integer futureDate, Map<String, Value> prices){
		if(prices.containsKey(x.security)){
			if(futureDate>=prices.get(x.security).quarter)
				return prices.get(x.security).value;
		}
		return null;
	}
	public static List<QuarterData> checkBloomberg(List<QuarterData> ls, DataCollection dataC, Map<String,Map<Integer, Value>> foundPrices, int qrt, int i){
		List<QuarterData> notFound = new ArrayList<QuarterData>();
		for(QuarterData current:ls){
			Integer futureDate = tools.iterateDate(current.getDateCode(), qrt);
			Value future = queryFuturePrice(current.security,futureDate,foundPrices, dataC.session, dataC.service);
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
			Integer futureDate = tools.iterateDate(current.getDateCode(), qrt);
			
			if(!foundPrices.containsKey(current.security))
				foundPrices.put(current.security, new HashMap<Integer, Value>());
			
			if(foundPrices.get(current.security).containsKey(futureDate))
				setReturn(current, current.px ,foundPrices.get(current.security).get(futureDate), dataC, i);
			else
				notFound.add(current);
		}
		return notFound;
	}
	public static void calculateForwards(DataCollection dataC){
		Map<String, Map <Integer,QuarterData>> dataMap = tools.convertToMap(dataC.quarters);
		Map<String, Map <Integer,Value>> foundPrices = new HashMap<String, Map<Integer, Value>>();
		Set<String> missing = new HashSet<String>();
		
		List<QuarterData> lsLookup = tools.removeEndYear(dataC.quarters, dataC.endYr);
		for(int i = 0; i<dataC.forwardQrtrs.length;i++){
			int qrt = dataC.forwardQrtrs[i];
			
			//Map<Integer, Double> mktPerc = tools.convertToPerc(dataC.index,qrt);
			
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