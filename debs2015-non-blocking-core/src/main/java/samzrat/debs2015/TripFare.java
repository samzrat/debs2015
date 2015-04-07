package samzrat.debs2015;

public class TripFare {

	//public byte[] cabId = "07290D3599E7A0D62097A346EFCC1FB5".getBytes();
	public String medallion;
	public double fare;
	
	public TripFare(String medallion, double fare) {
		this.medallion = medallion;
		this.fare = fare;
	}
	
}
