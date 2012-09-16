package pdnet.usi.ch.usi_display;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

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

import pdnet.usi.ch.usi_display.MyAppsFragmentTab.MyAppsItemAdapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

public class AppList extends Activity {

	private static final String TAG = "AppList";
	
	private HashMap<String, JSONArray> itemsMap = new HashMap<String, JSONArray>();
	private SocketManager socketManager;
	
	private class DownloadXMLFile extends AsyncTask<Void,String, String>
    {
		Context context;
		String appNamespace;
		
		public DownloadXMLFile(Context context, String appNamespace) {
			
			this.context = context;
			this.appNamespace = appNamespace;
		}
		
        @Override
        protected String doInBackground(Void... params) {
        	Log.v(TAG, "doing in the background");
        	String socketAddress = readXMLFile(socketManager.PDNET_HOST+"/assets/applications/list.xml", appNamespace);
        	return socketAddress;
        }
        
        @Override
        protected void onPostExecute(final String result) {
        	
	        Log.v(TAG,  "/*** LEFT DATA ***/\n"+
	        			"appName : "+appNamespace+"\n"+
	        		    "socket : "+result+"\n");
	        
	        Set<String> appsSockets = socketManager.activeSockets.keySet();
	        if(!appsSockets.contains(appNamespace)) {
	        	socketManager.createAppSocketWithAddress(appNamespace, "1", result);
	        } else {
				WebSocketClient appSocket = socketManager.activeSockets.get(appNamespace);
				socketManager.sendGetItemsRequest(appSocket, "1");
	        }
        }
    }
	

	
	public String readXMLFile(String path, String appNamespace) {
		
		Log.v(TAG, "reading xml searching for "+appNamespace+" socket");
		String appSocket = "";
		ArrayList<AllAppsItem> apps = new ArrayList<AllAppsItem>();
		Log.v("LocationService", "reading file");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Document doc = db.parse(new URL(path).openStream());
			Element root = doc.getDocumentElement();

			//get the list of displays
			NodeList pd_list = root.getElementsByTagName("app");
			if(pd_list != null && pd_list.getLength() > 0) {
				for(int i = 0 ; i < pd_list.getLength();i++) {
					
					//get the employee element
					Element cur_app = (Element)pd_list.item(i);
					Log.v("cur_display", cur_app.toString());
					String namespace = getTagValue("namespace", cur_app);
					String socketAddress = getTagValue("websocket_address", cur_app);
					if(namespace.equals(appNamespace)) {
						appSocket = socketAddress;
					}
				}
			}
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v(TAG, "EXCEPTION 1");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v(TAG, "EXCEPTION 2");

		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v(TAG, "EXCEPTION 3");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.v(TAG, "EXCEPTION 3");
		}
		return appSocket;
}
	
	

	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}

	
	
	
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_list);
        Log.v(TAG, "created activity appList");
        
        socketManager = SocketManager.getInstance(this);
        String jsonString = getIntent().getExtras().getString("json");
        JSONObject appsJSON = null;
		try {
			appsJSON = new JSONObject(jsonString);
			Log.v(TAG, "received json => " + appsJSON.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        try {
			JSONArray apps = appsJSON.getJSONArray("apps");
			for(int j =0; j < apps.length(); j++) {
				Log.v(TAG, "app => "+apps.get(j));
				itemsMap.put(apps.getString(j), new JSONArray());
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        updateUI();
	}
	
	
	
	
	public void updateUI() {
		
		final ListView lv = (ListView) findViewById(R.id.appList);
		//Make a new listadapter
		Log.v(TAG, "lv => "+lv);
		
		Set<String> apps = itemsMap.keySet();
		ArrayList<AppListItem> appsObjs = new ArrayList<AppListItem>();
		
		for(String app : itemsMap.keySet()) {
			AppListItem item = new AppListItem(app);
			appsObjs.add(item);
		}
		
		
	    lv.setAdapter(new AppListAdapter(this, android.R.layout.simple_list_item_1, appsObjs));
		
		// Assign adapter to ListView
		lv.setTextFilterEnabled(true);
		Log.v(TAG, "set on item click");
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//OPEN WEBVIEW WITH MOBILE PLUGIN
				Log.v(TAG, "see items of current app");
				AppListItem curApp = (AppListItem) lv.getItemAtPosition(position);
				goToAppItemsView(curApp.getName());
			}
		}); 
		
	}
	
	
	public void goToAppItemsView(String appName) {
		
		
		DownloadXMLFile task = new DownloadXMLFile(this, appName);
		task.execute();
		//{ app: list_of_items, app2 : list_of_items_2 }
		
	}

	
	
	public class AppListItem {
		
		
		private String appName;
//		private JSONArray items;
		
		public AppListItem(String appName) {
			
			this.appName = appName;
		}
		
		
		public String getName() {
			
			return appName;
		}
	
		public void setName(String appName) {
			
			this.appName = appName;
		}
		
		
	}
	
	
	
	public class AppListAdapter extends ArrayAdapter<AppListItem> {
	    
		private static final String TAG = "AppListAdapter";
		private ArrayList<AppListItem> apps;
		LayoutInflater viewInflator;

	    public AppListAdapter(Context context, int textViewResourceId, ArrayList<AppListItem> apps) {
	        super(context, textViewResourceId, apps);
	        this.apps = apps;
	        viewInflator = LayoutInflater.from(getContext());
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        
	    	View v = convertView;
	    	Log.v(TAG, "view => "+ convertView);
	        if (v == null) {
	        	Log.v(TAG, "here");
	        	String inflater = Context.LAYOUT_INFLATER_SERVICE;
	        	LayoutInflater vi;
	        	vi = (LayoutInflater)getContext().getSystemService(inflater);
	        	Log.v(TAG, "vi > "+vi);
	            v = vi.inflate(R.layout.app_list_item, null);
	        }
	        
	        
	        Log.v(TAG, "v => "+v);
	        AppListItem curApp = apps.get(position);
	        
	        
	        TextView tv = (TextView) v.findViewById(R.id.appListName);
	        Log.v(TAG, "curApp => "+tv);
	        tv.setText(curApp.getName());
	        
	        ImageView appIcon = (ImageView) v.findViewById(R.id.appListIcon);
	        
	        int id = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + curApp.getName()+"_icon", null, null);
	        appIcon.setImageResource(id);
	        
	        return v; 
	    }

		private LayoutInflater getSystemService(String layoutInflaterService) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	
}
