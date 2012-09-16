package pdnet.usi.ch.usi_display;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.java_websocket.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pdnet.usi.ch.usi_display.R;
import pdnet.usi.ch.usi_display.AppList.AppListItem;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class ItemsFragment extends Activity {

	
	private static final String TAG = "ItemsList";
	private LayoutInflater vi;
	private String curUrl;
	private ArrayList<String> images = new ArrayList<String>();
	private String lastDescription = "";
	
	private class DownloadXMLFile extends AsyncTask<Void,String, Bitmap>
    {
		Context context;
		
		public DownloadXMLFile(Context context) {
			
			this.context = context;
		}
		
        @Override
        protected Bitmap doInBackground(Void... params) {
        	Log.v(TAG, "doing in the background");
        	Bitmap bm = getBitmapFromURL(curUrl);
        	return bm;
        }

        @Override
        protected void onPostExecute(final Bitmap result) {
        }
    }
	
	
	private Drawable grabImageFromUrl(String url) throws Exception {
    	return Drawable.createFromStream((InputStream)new URL(url).getContent(), "src");
    }

	public Object fetch(String address) throws MalformedURLException,IOException {
		URL url = new URL(address);
		Object content = url.getContent();
		return content;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.app_item_list);
		JSONArray items = null;
		
		String jsonString = getIntent().getExtras().getString("items");
		
		JSONObject itemsJSON = null;
		try {
			itemsJSON = new JSONObject(jsonString);
			Log.v(TAG, "received json => " + itemsJSON.toString());
			items = itemsJSON.getJSONArray("items");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		updateUI(items);
	}
	
	
	
	public void updateUI(JSONArray items) {
		
		ArrayList<AppItem> appItems = new ArrayList<AppItem>();
		final ListView lv = (ListView) findViewById(R.id.appItemsList);
		
		for(int j = 0 ; j < items.length(); j++) {
			JSONObject item;
			try {
				item = items.getJSONObject(j);
				Log.v(TAG, "/	*** item **/"+item.toString());
				
				String title = item.getString("title");
				String img = item.getString("img");
				String description = item.getString("desc");
				String link = item.getString("link");
				
				AppItem appItem = new AppItem(title, img, link, description);
				appItems.add(appItem);
				Log.v(TAG, "description => "+appItem.getDescription());
			
				Log.v(TAG, "/*** CREATING JSON OBJECT ITEM ****/"+
						"Title : "+title+"\n"+
						"Img : "+img+"\n"+
						"Description : "+description+"\n"+
						"Link : "+link+"\n"
				);
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		Log.v(TAG, "lv => "+lv);
		lv.setAdapter(new AppItemAdapter(this, android.R.layout.simple_list_item_1, appItems));
		
		// Assign adapter to ListView
		lv.setTextFilterEnabled(true);
		Log.v(TAG, "set on item click");
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//OPEN WEBVIEW WITH MOBILE PLUGIN
				Log.v(TAG, "see items of current app");
				AppItem curApp = (AppItem) lv.getItemAtPosition(position);
				Log.v(TAG, "item link => "+curApp.getLink());
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(curApp.getLink()));
				startActivity(browserIntent);

			}
		}); 
	}
	
	
	public void goToInfoItem(AppItem item) {
		Intent newsIntent = new Intent();
	    newsIntent.setClass(this, ItemsFragment.class);
	    newsIntent.putExtra("title", item.getTitle());
		newsIntent.putExtra("description", item.getDescription());
		newsIntent.putExtra("link", item.getLink());
		newsIntent.putExtra("img", item.getImg());
    	startActivity(newsIntent);
	}
	
	
	
	 public static Bitmap getBitmapFromURL(String src) {
	        try {
	            Log.e("src",src);
	            URL url = new URL(src);
	            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	            connection.setDoInput(true);
	            connection.connect();
	            InputStream input = connection.getInputStream();
	            Bitmap myBitmap = BitmapFactory.decodeStream(input);
	            Log.e("Bitmap","returned");
	            return myBitmap;
	        } catch (IOException e) {
	            e.printStackTrace();
	            Log.e("Exception",e.getMessage());
	            return null;
	        }
	    }

	 
	 
	public class AppItemAdapter extends ArrayAdapter<AppItem> {
	    
		private static final String TAG = "AppItemAdapter";
		private ArrayList<AppItem> items;

		
	    public AppItemAdapter(Context context, int textViewResourceId, ArrayList<AppItem> items) {
	        super(context, textViewResourceId, items);
	        this.items = items;
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        
	    	Log.v(TAG, "position => "+position);
	    	View v = convertView;
	    	Log.v(TAG, "view => "+ convertView);
	        
	    	if (v == null) {
	        	Log.v(TAG, "v is null");
	            vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            Log.v(TAG, "VI IS => "+vi);
	            v = vi.inflate(R.layout.app_item, null);
	        }
	    	
	    	Log.v(TAG, "creating News Item");
	        AppItem item = items.get(position);
	        	
	        final TextView itemTitle = (TextView) v.findViewById(R.id.appItemTitle);
	        itemTitle.setText(item.getTitle());
	       
	        final TextView itemDesc = (TextView) v.findViewById(R.id.appItemDescription);
	        itemDesc.setText(item.getDescription());
	        
	        curUrl = item.getImg();
	     
	        final ImageView itemImage = (ImageView) v.findViewById(R.id.appItemImage);
        	// load image view control
	        
	        if(!images.contains(item.getImg())) {
	        	images.add(item.getImg());
	        	DownloadXMLFile task = new DownloadXMLFile(parent.getContext());
	        	Bitmap result;
	        	try {
	        		result = task.execute().get();
	        		Log.v(TAG, "result => " + result);
	        		Log.v(TAG, "itemImage => "+itemImage);
	        		itemImage.setImageBitmap(result);
	        	} catch (InterruptedException e) {
	        		// TODO Auto-generated catch block
	        		e.printStackTrace();
	        	} catch (ExecutionException e) {
	        		// TODO Auto-generated catch block
	        		e.printStackTrace();
	        	}
	        }
	        lastDescription = item.getDescription();
	        
	        Log.v(TAG, "SETTING IAMGE = "+item.getImg());
	        return v;
	    }	
	    
		private LayoutInflater getSystemService(String layoutInflaterService) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	
	


	
	
	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	

	 private class Callback extends WebViewClient{  

	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            return (false);
	        }
	  }
	
	
}
