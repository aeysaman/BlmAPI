package gather;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;

public class Pricing {
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
			
			Double d = Api.requestPrice(s, futureDate, session, service);
			if(d!=-1.0){
				foundPrices.get(s).put(futureDate, new Value(s, futureDate,d));
				return d;
			}
			else
				missing.add(s);
		}
		return 0.0;
	}
}