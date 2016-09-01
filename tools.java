package api;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class tools {
	public static void printSetToFile(File f, Set<String> ls){
		try {
			BufferedWriter fileWriter = new BufferedWriter(new FileWriter(f));
			for(String s: ls)
				fileWriter.write(s + "\n");
			fileWriter.close();
		} catch (IOException e) {
			tools.exceptionEnd("error in printing", e);
		}
	}
	public static String joinListD(List<Double> foo){
		String x = "";
		int i = 0;
		while(i<foo.size()){
			Double d = foo.get(i);
			if(d ==null)
				x = x + "null";
			else
				x = x + d.toString();
			if(i!=foo.size()-1)
				x = x+ ",";
			i++;
		}
		return x;
	}
	public static String joinListS(List<String> foo){
		String x = "";
		int i = 0;
		while(i<foo.size()){
			x = x +foo.get(i);
			if(i!=foo.size()-1)
				x = x+ ",";
			i++;
		}
		return x;
	}
	//systematic error handling, ending the program
	public static void exceptionEnd(String code, Exception e){
		System.out.println(code);
		e.printStackTrace();
		System.exit(1);
	}
	public static void printMap(Map<?, ?> x){
		for(Object key: x.keySet()){
			System.out.println(key.toString() + " -> " + x.get(key));
		}
	}
	public static Map<String, Value> readIndex(File f){
		Map<String, Value> result = new HashMap<String, Value>();
		Scanner fileReader = openScan(f);
		while (fileReader.hasNextLine()){ 
			String[] items = fileReader.nextLine().split(",");
			String[] date = items[0].split("/");
			int year = Integer.parseInt(date[2]);
			int month = Integer.parseInt(date[0]);
			int day = Integer.parseInt(date[1]);
			Double x = Double.parseDouble(items[1]);
			Value v = new Value("index", x, year, month, day);
			result.put(v.dateString(), v);
		}
		fileReader.close();
		return result;
	}
	public static List<String> readCSVtoList(File f){
		List<String> result = new ArrayList<String>();
		Scanner fileReader = openScan(f);
		while (fileReader.hasNextLine()){ 
			String[] items = fileReader.nextLine().split(",");
			result.add(items[0]);
		}
		fileReader.close();
		return result;
	}
	public static Map<String, String> readCSVtoMap (File f){
		Map<String, String> result = new HashMap<String,String>();
		Scanner fileReader = openScan(f);
		while (fileReader.hasNextLine()){ 
			String[] items = fileReader.nextLine().split(",");
			result.put(items[0], items[1]);
		}
		fileReader.close();
		return result;
	}
	public static Scanner openScan(File f){
		Scanner scan = null;
		try {
			scan = new Scanner(f);
		} catch (FileNotFoundException e) {
			exceptionEnd("error in reading index",e);
		}
		return scan;
	}
	public static Map<Integer, List<String>> readAllSecurities(File f) {
		Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
		Scanner scan = openScan(f);
		Integer[] index = readTopLine(scan.nextLine());
		for(Integer i : index)
			result.put(i, new ArrayList<String>());
		while(scan.hasNextLine()){
			String[] s = scan.nextLine().split(",");
			for(int i = 0; i<s.length; i++){
				String x = s[i];
				if(x.length()>1)
					result.get(index[i]).add(x);
			}
		}
		return result;
	}
	public static Integer[] readTopLine(String s){
		String[] strIndex = s.split(",");
		Integer[] intIndex = new Integer[strIndex.length];
		for(int i = 0; i<strIndex.length;i++)
			intIndex[i] = Integer.parseInt(strIndex[i]);
		return intIndex;
	}
	public static Map<String, Value> readTerminalValues(File f) {
		Map<String, Value> result = new HashMap<String, Value>();
		Scanner scan = openScan(f);
		while(scan.hasNextLine()){
			String[] s = scan.nextLine().split(",");
			String security = s[0] + " Equity";
			String[] date = s[1].split("/");
			Value v = new Value(security,Double.parseDouble(s[2]),Integer.parseInt(date[2]), Integer.parseInt(date[0]), Integer.parseInt(date[1]) );
			result.put(security, v);
		}
		return result;
	}
	public static Map<String,Map<Integer, QuarterData>> convertToMap(List<QuarterData> list) {
		Map<String, Map<Integer,QuarterData>> result = new HashMap<String, Map<Integer, QuarterData>>();
		for(QuarterData x: list){
			if(!result.containsKey(x.security))
				result.put(x.security, new HashMap<Integer, QuarterData>());
			result.get(x.security).put(x.getDateCode(), x);
		}
		return result;
	}
	public static Integer convertToQuarter(int year, int month){
		return year*10 + (month-1)/3 + 1;
	}
	//takes the list of raw index values and converts them to fwd percentages of length q
	public static Map<Integer, Double> convertToPerc(Map<Integer, Double> index, int q) {
		Map<Integer, Double> result = new HashMap<Integer,Double>();
		for(Integer i : index.keySet()){
			double curr = index.get(i);
			int futureDate = iterateDate(i, q);
			double fwd = curr;
			if(index.containsKey(futureDate))
				fwd = index.get(futureDate);
			result.put(i, (fwd-curr)/curr);
		}
		return result;
	}
	public static int iterateDate(int code, int i){
		int year = code/10;
		int qrt = code%10 +i;
		while(qrt>4){
			qrt-=4;
			year++;
		}
		return year *10 + qrt;
	}
	public static List<QuarterData> removeEndYear(List<QuarterData> ls, int endYr) {
		List<QuarterData> result = new ArrayList<QuarterData>();
		for(QuarterData x: ls){
			if(x.year<endYr)
				result.add(x);
		}
		return result;
	}
}