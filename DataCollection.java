//test
//test 2
package gather;
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
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

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
	Map<Integer, Double> index;					//maps quarters to the index price
	Map<String, String> fieldsMap;				//maps the Bloomberg code to the fields real name
	List<String> fields;						//list of Bloomberg field names
	List<String> reFields;						//list of real names of the Bloomberg codes, in the same order
	Map<Integer, List<String>> securitiesByYear;//maps each year to a list of the securities in the index that year
	List<QuarterData> quarters;					//List of all QuarterData elements, the meat of the analysis
	Set<String> missing;						//all securities whose forward prices could not be found
	
	
	public DataCollection(String[] args){
		session =setupSession();
		service = session.getService("//blp/refdata");
		
		startYr = Integer.parseInt(args[0]);
		endYr = Integer.parseInt(args[1]);
	}
	public static void main(String[] args) {
		
		DataCollection data = new DataCollection(args);
		
		data.readFiles();
		
		data.gatherData();
		
		QuarterData.calculateForwards(data);

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
				Request req = formRequest(service, yr, fields, securitiesGroup);
				CorrelationID id = new CorrelationID();
				try {
					session.sendRequest(req, id);
				} catch (IOException e) {
					System.out.println("error in sending request");
				}
				resultDG.addAll(collectEvents(session));
				i+=jump;
				j+=jump;
			}
			resultQD.addAll(QuarterData.convertDGtoQD(resultDG,securitiesFullYr,fields,yr));
		}
		quarters = resultQD;
	}
	//creates a request with the given fields and companies
	public static Request formRequest(Service serv, int y ,List<String> fields, List<String> companies){
		Request req = serv.createRequest("HistoricalDataRequest");
		for(String f: fields){
			req.getElement("fields").appendValue(f);
		}
		for(String c: companies){
			req.getElement("securities").appendValue(c);
		}
		req.set("startDate", y + "0101");
		req.set("endDate", y + "1231");
		req.set("periodicitySelection", "QUARTERLY");
		return req;
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
	//Bloomberg written method for collecting events until Response is seen. returns all found data from the event responses waiting
	public static List<DataGroup> collectEvents(Session sess) {
		List<DataGroup> result = new LinkedList<DataGroup>();
		boolean keepLooping = true;
		while(keepLooping){
			Event event = null;
			try {
				event = sess.nextEvent(); 
			} 
			catch (InterruptedException e) {
				System.out.println("error in getting next event"); e.printStackTrace();
			}
			switch (event.eventType().intValue()){
				case Event.EventType.Constants.RESPONSE: // final event
					keepLooping = false; // fall through
				case Event.EventType.Constants.PARTIAL_RESPONSE:
					result.addAll(handleResponseEvent(event));
					break;
				default:
					handleOtherEvent(event);
					break;
			}
		}
		return result;
	}
	//Bloomberg written method for handling other(setup) events
	public static void handleOtherEvent(Event event) {
		MessageIterator iter = event.messageIterator();
		while (iter.hasNext()) {
			Message message = iter.next();
			/*try {
				message.print(System.out);
			} catch (IOException e) {
				System.out.println("error in printing other message");
				e.printStackTrace();
			}*/
			if (Event.EventType.Constants.SESSION_STATUS == event.eventType().intValue() && "SessionTerminated" == message.messageType().toString()){
				System.out.println("Terminating: " + message.messageType());
				System.exit(1);
			}
		}
	}
	//iterates through event object, calling processMessage
	public static List<DataGroup> handleResponseEvent(Event e){
		List<DataGroup> result = new LinkedList<DataGroup>();
		MessageIterator it = e.messageIterator();
		while(it.hasNext()){
			Message m = it.next();
			try{
			result.addAll(processMessage(m));
			}
			catch(Exception exc){
				System.out.println("error in processing message");
			}
		}
		return result;
	}
	//loops through Message object and returns field information as DataGroup objects
	private static List<DataGroup> processMessage(Message m) throws Exception {
		List<DataGroup> result = new LinkedList<DataGroup>();
		Element securityData = m.getElement("securityData");
		String sec = securityData.getElementAsString("security");
		
		Element fieldDataArray = securityData.getElement("fieldData");
		for (int i = 0; i < fieldDataArray.numValues(); i++) {
			Element fieldData = fieldDataArray.getValueAsElement(i);
			DataGroup cluster = new DataGroup(sec);
			for(int j = 0; j<fieldData.numElements(); j++){
				Element field = fieldData.getElement(j);
				cluster.enterData(field.name().toString(), field.getValueAsString());
			}
			result.add(cluster);
		}
		return result;
	}
	//setup information, written by Bloomberg
	public static Session setupSession(){
		SessionOptions sessionOptions = new SessionOptions();
		sessionOptions.setServerHost("localhost");
		sessionOptions.setServerPort(8194);
		Session session = new Session(sessionOptions);
		try{
			if (!session.start()) {
				System.out.println("Could not start session.");
				System.exit(1);
			}
			if (!session.openService("//blp/refdata")) {
				System.out.println("Could not open service //blp/refdata");
				System.exit(1);
			}
		}
		catch(Exception e){
			tools.exceptionEnd("error in setting up session", e);
		}
		return session;
	}
	public static Integer formatDate(int date, int end){
		int year = date/10;
		int month = ((date%10-1) * 3) + 1;
		return year *10000 + month*100 + end;
	}
	//sends a request for the security in the given quarter. Once the event is collected, it returns the value from handleResponseEventPrice
	public static Double requestPrice(String security, int futureDate, Session session, Service service) {
		try {
			Request req = service.createRequest("HistoricalDataRequest");
			req.getElement("fields").appendValue("PX_LAST");
			req.getElement("securities").appendValue(security);
			
			req.set("startDate", formatDate(futureDate, 1));
			req.set("endDate", formatDate(tools.iterateDate(futureDate, 1), 28));
			req.set("periodicitySelection", "QUARTERLY");
			
			session.sendRequest(req, new CorrelationID(1));
			
			boolean continueToLoop = true;
			while (continueToLoop) {
				Event event = session.nextEvent();
				switch (event.eventType().intValue()) {
					case Event.EventType.Constants.RESPONSE: // final event
						continueToLoop = false; // fall through
					case Event.EventType.Constants.PARTIAL_RESPONSE:
						Double d = handleResponseEventPrice(event);
						return d;
					default:
						handleOtherEvent(event);
						break;
				}
			}
			System.out.println("\tnot found");
			return -1.0;
		} catch (Exception e) {
			System.out.println("error in sending request");
			e.printStackTrace();
			return -1.0;
		}
	}
	//goes through the event looking for a price field, and returns this price once found. If no price is found -1.0 is returned
	public static Double handleResponseEventPrice(Event event) throws Exception {
		MessageIterator iter = event.messageIterator();
		 while (iter.hasNext()) {
			Message message = iter.next();
			Element securityData = message.getElement("securityData");
			Element fieldDataArray = securityData.getElement("fieldData");
			for (int i = 0; i < fieldDataArray.numValues(); i++) {
				Element fieldData = fieldDataArray.getValueAsElement(i);
				for(int j = 0; j<fieldData.numElements(); j++){
					Element field = fieldData.getElement(j);
					if(field.name().toString().equals("PX_LAST"))
						return Double.parseDouble(field.getValueAsString());
				}
			}
		 }
		 return -1.0;
	}
}
