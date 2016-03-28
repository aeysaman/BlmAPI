package gather;
public class Value {
	String security;
	Integer date;
	double value;
	public Value(String security, Integer date, double value){
		this.security = security;
		this.date = date;
		this.value = value;
	}
	public String toString(){
		return "TV: " + security + "; " + date + "; " + value;
	}
}
