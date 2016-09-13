package api;

import general.Date;
import general.Tools;
public class Value{
	String name;
	Integer quarter;
	double value;
	public Date date;

	public Value(String name, double value, int year, int month, int day){
		this.name = name;
		this.value = value;
		this.date = new Date(year, month, day);
		this.quarter = Tools.convertToQuarter(year, month);
	}
	public Value(String name, double value, Date date){
		this.name = name;
		this.value = value;
		this.date = date;
		this.quarter = Tools.convertToQuarter(date.year, date.month);
	}
	public String toString(){
		return String.format("%s @ %s = %f ", name, date.toString(), value);
	}
}
