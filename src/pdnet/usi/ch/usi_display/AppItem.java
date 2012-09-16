package pdnet.usi.ch.usi_display;

public class AppItem {


	private String title;
	private String img;
	private String link;
	private String description;


	public AppItem(String title, String img, String link, String description) {
		this.title = title;
		this.img = img;
		this.link = link;
		this.description = description;
	}

	
	public void setTitle(String title) {
		
		this.title = title;
	}

	
	public String getTitle() {

		return title;
	}


	public void setImg(String img) {
		
		this.img = img;
	}
	
	
	public String getImg() {

		return img;
	}
	
	
	public void setDescription(String description) {
		
		this.description = description;
	}
	
	
	public String getDescription() {

		return description;
	}

	
	public void setLink(String link) {
		
		this.link = link;
	}
	
	
	public String getLink() {

		return link;
	}


}
