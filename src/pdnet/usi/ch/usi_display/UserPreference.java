package pdnet.usi.ch.usi_display;

public class UserPreference {

	
	private long id;
	private String username;
	private String appName;
	private String prefValue;
	
	

	
	public long getId() {
		return id;
	}
	
	
	public void setId(long id) {
		
		this.id = id;
	}

	
	
	public String getUsername() {
		
		
		return username;
	}
	
	
	public void setUsername(String username) {
		
		this.username = username;
	}
	
	
	public String getAppName() {
		
		return appName;
	}

	
	public void setAppName(String appName) {
		
		this.appName = appName;
	}
		
	
	
	public String getPrefValue() {
		
		return prefValue;
	}
	
	
	
	public void setPrefValue(String prefValue) {
		
		this.prefValue = prefValue;
	}
	
	
	
}
