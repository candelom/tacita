package pdnet.usi.ch.usi_display;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pdnet.usi.ch.usi_display.MyAppsFragmentTab.MyAppsItemAdapter;
import pdnet.usi.ch.usi_display.SettingsFragmentTab.OnServiceSelectedListener;



import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class AllAppsFragmentTab extends Fragment {

	
	public static final String TAG = "AllApps";
	public SocketManager socketManager;
	private ApplicationDBAdapter appDBAdapter;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private Context context;
	private boolean isUpdated = false;
	public static final String PDNET_HOST = "http://pdnet.inf.unisi.ch:9000/assets/applications/list.xml";
//	public static final String PDNET_HOST = "http://10.62.96.140:8888/list.xml";
//	public static final String PDNET_HOST = "http://10.61.99.5:8888/list.xml";
//	public static final String PDNET_HOST = "http://192.168.1.39:8888/list.xml";
    OnServiceSelectedListener mListener;


	
//	public AllAppsFragmentTab(ApplicationDBAdapter appDBAdapter, UserPreferenceDBAdapter userPrefDBAdapter, Context context) {
//		this.appDBAdapter = appDBAdapter;
//		this.userPrefDBAdapter = userPrefDBAdapter;
//		this.context = context;
//	}

	
	
	private class DownloadXMLFile extends AsyncTask<Void,Void, ArrayList<AllAppsItem>>
    {
		Context context;
		public DownloadXMLFile(Context context) {
			
			this.context = context;
		}
		
        @Override
        protected ArrayList<AllAppsItem> doInBackground(Void... params) {
        	ArrayList<AllAppsItem> values = readXMLFile(PDNET_HOST);
        	Log.v(TAG, "returning values => "+ values);
        	return values;
        }
        

        @Override
        protected void onPostExecute(final ArrayList<AllAppsItem> result)
        {
        	getActivity().runOnUiThread(new Runnable() {
				
				public void run() {
					final ListView lv = (ListView) getActivity().findViewById(R.id.myList);
					//Make a new listadapter
					Log.v(TAG, "lv => "+lv);
					
					// Assign adapter to ListView
				    lv.setAdapter(new AllAppsItemAdapter(context, android.R.layout.simple_list_item_1, result));
					lv.setTextFilterEnabled(true);
					lv.setOnItemClickListener(new OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
							AllAppsItem appName = (AllAppsItem) lv.getItemAtPosition(position);
							Log.d(TAG, "Creating ViewFragment for APP "+ appName);
					        
					        //change to VIEW FRAGMENT
					        FragmentManager fragmentManager = getFragmentManager();
					        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
						
					        mListener.setCurAppName(appName);
					        ViewAppFragment vpf = new ViewAppFragment();
					        fragmentTransaction.replace(R.id.fragment_place, vpf);
					        fragmentTransaction.commit();
						}
					}); 
					
					Log.v(TAG, "set update to true");
					isUpdated = true;
				}
			});
        }
    }
	
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnServiceSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnServiceSelectedListener");
        }
    }

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.all_apps_layout, container, false);
		
		Log.v(TAG, "ppdbadapter => "+mListener.getAppDBAdapter());
		this.appDBAdapter = mListener.getAppDBAdapter();
		this.userPrefDBAdapter = mListener.getUserPrefDBAdapter();
		this.context = mListener.getContext();
		Log.v(TAG, "created All apps View");
		//read all apps
		if(isUpdated) {
			Log.v(TAG, "read all apps");
			List<Application> apps = appDBAdapter.getAllApps();
			updateAllAppsUI(apps, view);
		} else {
			Log.v(TAG, "download apps");
	        DownloadXMLFile task = new DownloadXMLFile(context);
	        task.execute();
		}
		
		Log.v(TAG, "return view");
		return view;
	}
	
	
	
	public void updateAllAppsUI(List<Application> apps_objs, View view) {
		
		ArrayList<AllAppsItem> apps = new ArrayList<AllAppsItem>();
		
		for(Application app : apps_objs) {
			Log.v(TAG, "adding app => " + app.getName());
			AllAppsItem appItem = new AllAppsItem(app.getName(), app.getIcon());
			apps.add(appItem);
		}
		
		final ListView lv = (ListView) view.findViewById(R.id.myList);
		//Make a new listadapter
		Log.v(TAG, "lv => "+lv);
		
		// Assign adapter to ListView
//		lv.setAdapter(adapter); 
	    lv.setAdapter(new AllAppsItemAdapter(context, android.R.layout.simple_list_item_1, apps));

		
		lv.setTextFilterEnabled(true);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				AllAppsItem appName = (AllAppsItem) lv.getItemAtPosition(position);
				Log.d(TAG, "Creating ViewFragment for APP "+ appName);
		        
				goToAppInfoView(appName);
		        
			}
		}); 
		
	}
	
	
	public void goToAppInfoView(AllAppsItem appName) {
		
		//change to VIEW FRAGMENT
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	
        mListener.setCurAppName(appName);
        ViewAppFragment vpf = new ViewAppFragment();
        fragmentTransaction.replace(R.id.fragment_place, vpf);
        fragmentTransaction.commit();
	}
	
	
    /**
	 * Reads XML file containing display information
	 * @param path
	 */
	public ArrayList<AllAppsItem> readXMLFile(String path) {
		
		ArrayList<AllAppsItem> apps = new ArrayList<AllAppsItem>();
		Log.v("LocationService", "reading file");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
//			Log.v("LocationService", "before");
			
//			context.getAssets().openFd("thefilename.xml");
			Document doc = db.parse(new URL(path).openStream());
			Log.v("LocationService", "after");

//			File file = new File("file:///android_asset/list.xml");
			
//			AssetManager assetManager = context.getAssets();
//			Document doc = db.parse(assetManager.open("server/list.xml"));
			Element root = doc.getDocumentElement();

			//get the list of displays
			NodeList pd_list = root.getElementsByTagName("app");
			if(pd_list != null && pd_list.getLength() > 0) {
				for(int i = 0 ; i < pd_list.getLength();i++) {
					//get the employee element
					Element cur_app = (Element)pd_list.item(i);
					Log.v("cur_display", cur_app.toString());
					String name = getTagValue("name", cur_app);
					String namespace = getTagValue("namespace", cur_app);
					Log.v(TAG, "parsing app => "+name);
					if(!name.equals("Live scores") && !name.equals("Slideshow")) {
						String view = getTagValue("view", cur_app);
						String socketAddress = getTagValue("websocket_address", cur_app);
						String description = getTagValue("description", cur_app);
						String icon = getTagValue("icon", cur_app);
						String previewImage = getTagValue("preview-image", cur_app);
						AllAppsItem appItem = new AllAppsItem(name, icon);
						Log.v(TAG, appDBAdapter.checkAppExistence(name)+"");
						if(!appDBAdapter.checkAppExistence(name)) {
							appDBAdapter.createApp(name, namespace, view, socketAddress, description, icon);
						}
						apps.add(appItem);
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
			e.printStackTrace();
			Log.v(TAG, "EXCEPTION 4");
			final TextView tv = new TextView(context);
			tv.setText("NO CONNECTION. TRY AGAIN");
			LinearLayout.LayoutParams lastTxtParams = new LinearLayout.LayoutParams(400, 400);
			lastTxtParams.setMargins(50, 200, 10, 10);
			tv.setLayoutParams(lastTxtParams);
			tv.setTextSize(18);
			LinearLayout myAppsLayout =(LinearLayout) getActivity().findViewById(R.id.allAppsLayout);
			myAppsLayout.addView(tv);
//			final ImageView connectionImg = new ImageView(context);
//			myAppsLayout.addView(connectionImg);
		}
		Log.v(TAG, "returning apps => "+apps);
		return apps;
	}
	
	
	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	
	
	public class AllAppsItemAdapter extends ArrayAdapter<AllAppsItem> {
	    
		private static final String TAG = "AllAppsItemAdapter";
		private ArrayList<AllAppsItem> apps;

		
	    public AllAppsItemAdapter(Context context, int textViewResourceId, ArrayList<AllAppsItem> apps) {
	        super(context, textViewResourceId, apps);
	        this.apps = apps;
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        
	    	Log.v(TAG, "position => "+position);
	    	View v = convertView;
	    	Log.v(TAG, "view => "+ convertView);
	        if (v == null) {
	        	Log.v(TAG, "v is null");
	            LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = vi.inflate(R.layout.all_apps_row, null);
	        }
	      
	        AllAppsItem app = apps.get(position);
	        Application application = appDBAdapter.getAppByName(app.getName());
	        
	        Log.v(TAG, app+" askjdfsajkldfsajkladfs");
	        if (app != null) {
	            final TextView appName = (TextView) v.findViewById(R.id.appName);
	            appName.setText(app.getName());
	            
	            final ImageView icon = (ImageView) v.findViewById(R.id.appIcon);
		        int id = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + application.getNamespace()+"_icon", null, null);
	            icon.setImageResource(id);
	            
	            
	            if(userPrefDBAdapter.getPreference(application.getNamespace()) != null) {
	            	final TextView appType = (TextView) v.findViewById(R.id.appType);
	            	appType.setText("Installed");
	            } else {
	            	final TextView appType = (TextView) v.findViewById(R.id.appType);
	            	appType.setText("Get it!");
	            }
	        }
	        return v;
	    }	

		private LayoutInflater getSystemService(String layoutInflaterService) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
}
