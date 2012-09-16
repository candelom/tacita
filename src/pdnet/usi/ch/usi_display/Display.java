package pdnet.usi.ch.usi_display;

import java.util.ArrayList;

public class Display {

	private String id;
	private float latitude;
	private float longitude;
	private String name;
	private ArrayList<String> supportedApps;

	
	public Display(String id, float latitude, float longitude, String name, ArrayList<String> supportedApps) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.name = name;
		this.supportedApps = supportedApps;
	}


	public String getId() {

		return id;
	}


	public float getLatitude() {

		return latitude;
	}
	
	
	public float getLongitude() {

		return longitude;
	}

	
	public String getName() {
		
		return name;
	}
	
	
	public ArrayList<String> getSupportedApps() {
		
		return supportedApps;
	}
	
	
	
	public String printDisplay() {
		
		String print = "DISPLAY "+getId()+"\n\n"+
							"Name : "+ getName()+"\n"+
							"Lat : "+ getLatitude()+"\n"+
							"Long : "+ getLongitude()+"\n"+
							"supportedApps : "+ getSupportedApps()+"\n";
		
		return print;
		
	}

}
