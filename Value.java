package gather;


public class Value {
	String security;
	Integer quarter;
	double value;
	int year;
	int month;
	int day;

	public Value(String security, double value, int year, int month, int day){
		this.security = security;
		this.value = value;
		this.year = year;
		this.month = month;
		this.day = day;
		this.quarter = tools.convertToQuarter(year, day);

	}
	public String toString(){
		return "Val: " + security + "; " + dateString() + "; " + value;
	}
	public String dateString(){
		return year + "/" + month + "/" + day;
	}
	public String iterateDownDateString(int i){
		int d = day -i;
		int m = month;
		int y = year;
		if(d<1){
			m--;
			d+=30;
		}
		if(m<1){
			y--;
			m+=12;
		}
		return y + "/" + m + "/" + d;
			
	}
}
