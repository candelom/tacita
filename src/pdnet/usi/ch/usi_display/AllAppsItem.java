package pdnet.usi.ch.usi_display;

public class AllAppsItem {

	public String appName;
	public String icon;

	public AllAppsItem(String appName, String icon) {
		this.appName = appName;
		this.icon = icon;
	
	}
	
	public String getName() {
		
		return appName;
	}
	
	public String getIcon() {
		
		return icon;
	}
	
	
	
}
