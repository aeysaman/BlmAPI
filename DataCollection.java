package api;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;

public class DataCollection {
	
	//set by User
	File fieldsFile = new File("fields.csv");
	File secByYrFile = new File("allSecurities.csv");
	File terminalFile = new File("terminalValues.csv");
	File outFile = new File("output.csv");
	File indexFile = new File("marketIndex.csv");
	File missingFile = new File("missingSecurities.csv");
	File weedFile = new File("weededSecurities.csv");
	
	int startYr;
	int endYr;
	
	int weed = 4; 	//limit to number of missing values before getting printed
	int jump = 50; 	// how many securities per request
	
	int[] forwardQrtrs = {1,2,4}; //must be of size 3

	//internal usage
	Session session;
	Service service;
	
	public final String ps = "PX_LAST";
	
	Map<String, Value> terminalPrices; 			//maps the Securities name to its time and premium for M&A
	Map<String, Value> index;					//maps quarters to the index price
	Map<String, String> fieldsMap;				//maps the Bloomberg code to the fields real name
	List<String> fields;						//list of Bloomberg field names
	List<String> reFields;						//list of real names of the Bloomberg codes, in the same order
	Map<Integer, List<String>> securitiesByYear;//maps each year to a list of the securities in the index that year
	List<QuarterData> quarters;					//List of all QuarterData elements, the meat of the analysis
	Set<String> missing;						//all securities whose forward prices could not be found
	
	
	public DataCollection(String[] args){
		session =Api.setupSession();
		service = session.getService("//blp/refdata");
		
		startYr = Integer.parseInt(args[0]);
		endYr = Integer.parseInt(args[1]);
	}
	public static void main(String[] args) {
		
		DataCollection data = new DataCollection(args);
		
		data.readFiles();
		
		data.gatherData();
		
		Pricing.calculateForwards(data);

		tools.printSetToFile(data.missingFile, data.missing);
		
		data.renameFields();
		
		data.printQuarters();
		
		System.out.println("All Done!");
	}
	public void readFiles(){
		terminalPrices = tools.readTerminalValues(terminalFile);
		index = tools.readIndex(indexFile);
		
		fields = new ArrayList<String>();
		fieldsMap = tools.readCSVtoMap(fieldsFile);
		for(String s : fieldsMap.keySet())
			fields.add(s);
		
		fields.add(ps);
		fieldsMap.put(ps, "Price");
		System.out.println("Years: " + startYr + " to " + endYr);
		System.out.println("fields: " + fields.toString());
		
		securitiesByYear = tools.readAllSecurities(secByYrFile);
	}
	private void renameFields() {
		List<String> s = new ArrayList<String>();
		for(int i = 0; i<fields.size(); i++)
			s.add("null");
		for(int i = 0; i<fields.size(); i++)
			s.set(i, fieldsMap.get(fields.get(i)));
		reFields = s;
	}
	public void gatherData(){
		List<QuarterData> resultQD = new ArrayList<QuarterData>();
		List<String> securitiesGroup = new LinkedList<String>();
		List<String> securitiesFullYr = new LinkedList<String>();
		for(int yr = startYr; yr<=endYr; yr++){
			System.out.println("\tyear" + yr + " out of " + startYr + " to " + endYr);
			int i = 0;
			int j = jump;
			List<DataGroup> resultDG = new ArrayList<DataGroup>();
			securitiesFullYr = securitiesByYear.get(yr);
			while(i<securitiesFullYr.size()){
				if(j>securitiesFullYr.size())
					j = securitiesFullYr.size();
				System.out.println("gathering: " + i + " to " + j );
				securitiesGroup = securitiesFullYr.subList(i, j);
				Request req = Api.formRequest(service, yr, fields, securitiesGroup);
				CorrelationID id = new CorrelationID();
				try {
					session.sendRequest(req, id);
				} catch (IOException e) {
					System.out.println("error in sending request");
				}
				resultDG.addAll(Api.collectEvents(session));
				i+=jump;
				j+=jump;
			}
			resultQD.addAll(QuarterData.convertDGtoQD(resultDG,securitiesFullYr,fields,yr));
		}
		quarters = resultQD;
	}
	
	public void printQuarters(){
		try{
			int weedCount=0;
			BufferedWriter fileWriter = new BufferedWriter(new FileWriter(outFile));
			BufferedWriter weedFileWriter = new BufferedWriter(new FileWriter(weedFile));
			fileWriter.write("Security,Date,DateNum," + titleJoin("Forward", forwardQrtrs) + titleJoin("Premium", forwardQrtrs) + tools.joinListS(reFields) + "\n");
			for(QuarterData bar : quarters){
				if(bar.missingCount() <=weed)
					fileWriter.write(bar.export()+ "\n");
				else{
					weedFileWriter.write(bar.security + "," + bar.exportDate() + "," + bar.missingCount() + "\n");
					weedCount++;
				}
			}
			fileWriter.close();
			weedFileWriter.close();
			System.out.println("Amount weeded: " + weedCount);
		}
		catch(Exception e){
			tools.exceptionEnd("error in printing", e);
		}
	}
	public static String titleJoin(String label, int[] qrts){
		String result = "";
		for(int i =0; i<3; i++)
			result+=label + (qrts[i]*3) + "m,";
		return result;
	}
	
	
	
	
	
	
	
	
}
