package gather;

import java.util.LinkedList;
import java.util.List;

import com.bloomberglp.blpapi.CorrelationID;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;

public class Api {

	//goes through the event looking for a price field, and returns this price once found. If no price is found -1.0 is returned
	public static Value handleResponseEventPrice(Event event, String security) throws Exception {
		MessageIterator iter = event.messageIterator();
		 while (iter.hasNext()) {
			Message message = iter.next();
			Element securityData = message.getElement("securityData");
			Element fieldDataArray = securityData.getElement("fieldData");
			for (int i = 0; i < fieldDataArray.numValues(); i++) {
				Element fieldData = fieldDataArray.getValueAsElement(i);
				
				int year=0,month=0,day=0;
				Double d =null;
				for(int j = 0; j<fieldData.numElements(); j++){
					Element field = fieldData.getElement(j);
					if(field.name().toString().equals("PX_LAST"))
						d = Double.parseDouble(field.getValueAsString());
					else if(field.name().toString().equals("date")){
						String[] bar = field.getValueAsString().split("-");
						year = Integer.parseInt(bar[0]);
						month = Integer.parseInt(bar[1]);
						day = Integer.parseInt(bar[2]);
					}
				}
				if(d!=null){
					return new Value(security, d, year, month, day);
				}
			}
		 }
		 return null;
	}
	//sends a request for the security in the given quarter. Once the event is collected, it returns the value from handleResponseEventPrice
	public static Value requestPrice(String security, int futureDate, Session session, Service service) {
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
						return handleResponseEventPrice(event, security);
					default:
						handleOtherEvent(event);
						break;
				}
			}
			System.out.println("\tnot found");
			return null;
		} catch (Exception e) {
			System.out.println("error in sending request");
			e.printStackTrace();
			return null;
		}
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
	public static Integer formatDate(int date, int end){
		int year = date/10;
		int month = ((date%10-1) * 3) + 1;
		return year *10000 + month*100 + end;
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
}