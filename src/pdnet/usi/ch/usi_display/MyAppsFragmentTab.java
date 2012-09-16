package pdnet.usi.ch.usi_display;

import java.util.ArrayList;
import java.util.List;

import pdnet.usi.ch.usi_display.SettingsFragmentTab.OnServiceSelectedListener;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ToggleButton;

public class MyAppsFragmentTab extends Fragment {

	
	public static final String TAG = "MyApps";
	private ApplicationDBAdapter appDBAdapter;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private Context context;
	private View view;
    OnServiceSelectedListener mListener;

	
	
//	public MyAppsFragmentTab(ApplicationDBAdapter appDBAdapter, UserPreferenceDBAdapter userPrefDBAdapter, Context context) {
//		super();
//		this.appDBAdapter = appDBAdapter;
//		this.userPrefDBAdapter = userPrefDBAdapter;
//		this.context = context;
//	}

	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v(TAG, "created My apps View");
		View view = inflater.inflate(R.layout.my_apps_layout, container, false);
		this.view = view;
		this.appDBAdapter = mListener.getAppDBAdapter();
		this.userPrefDBAdapter = mListener.getUserPrefDBAdapter();
		this.context = mListener.getContext();
		return view;
	}
	
	
	public void showInstalledApps(List<UserPreference> prefs, View view) {
		
		
		ArrayList<MyAppsItem> apps = new ArrayList<MyAppsItem>();
		
		for(UserPreference pref : prefs) {
			Application app = appDBAdapter.getAppByNameSpace(pref.getAppName());
			Log.v(TAG, "app "+app.getNamespace() +" is active = "+app.isAppActive());
			MyAppsItem appItem = new MyAppsItem(app.getName());
			apps.add(appItem);
		}
		
		
		final ListView lv = (ListView) view.findViewById(R.id.installedList);
		//Make a new listadapter
		Log.v(TAG, "lv => "+lv);
		
		Log.v(TAG, "set array dapter with "+apps.size());
	    lv.setAdapter(new MyAppsItemAdapter(context, android.R.layout.simple_list_item_1, apps));
		
		// Assign adapter to ListView
		lv.setTextFilterEnabled(true);
		
		Log.v(TAG, "set on item click");
		
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				//OPEN WEBVIEW WITH MOBILE PLUGIN
				AllAppsItem appName = (AllAppsItem) lv.getItemAtPosition(position);
				Application app = appDBAdapter.getAppByName(appName.getName());
				openMobilePlugin(app.getNamespace());
			}
		}); 
	}
	
	
	public void openMobilePlugin(String appNameSpace) {
		  Log.v(TAG, "loading WebView");
	      Intent intent = new Intent();
	      intent.setClass(getActivity(), MobilePluginActivity.class);
	      intent.putExtra("appNameSpace", appNameSpace);
	      startActivity(intent);
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
	
	public class MyAppsItemAdapter extends ArrayAdapter<MyAppsItem> {
	    
		private static final String TAG = "MYAppsItemAdapter";
		private ArrayList<MyAppsItem> apps;

	    public MyAppsItemAdapter(Context context, int textViewResourceId, ArrayList<MyAppsItem> apps) {
	        super(context, textViewResourceId, apps);
	        this.apps = apps;
	    }

	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        
	    	
	    	View v = convertView;
	        if (v == null) {
	            LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            v = vi.inflate(R.layout.my_apps_row, null);
	        }
	      
	        MyAppsItem app = apps.get(position);
	        if (app != null) {
	            final TextView appName = (TextView) v.findViewById(R.id.appName);
	            appName.setText(app.getName());
				final Application p = appDBAdapter.getAppByName((String)appName.getText());

	            appName.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Log.v(TAG, "open webview");
						//OPEN WEBVIEW WITH MOBILE PLUGIN
						openMobilePlugin(p.getNamespace());
					}
				});
	            
	            
	            final ImageView icon = (ImageView) v.findViewById(R.id.myAppIcon);
		        int id = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + p.getNamespace()+"_icon", null, null);
		        icon.setImageResource(id);
	            
	            final ToggleButton switchButton = (ToggleButton) v.findViewById(R.id.onOffButton);
	            
	            Application application = appDBAdapter.getAppByName(app.getName());
	            Log.v(TAG, "applicaiton activtiy => "+application.isAppActive()+" "+application.getNamespace());
	            if(application.isAppActive() == 1) {
	            	Log.v(TAG, "activating "+application.getNamespace());
	            	switchButton.setChecked(true);
	            }
	            switchButton.setOnClickListener(new View.OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Log.v(TAG, "button is => "+switchButton.getText());
						String text = (String) switchButton.getText();
						if(text.equals("ON")) {
							//activate Apps
							Application cur_app = appDBAdapter.getAppByName((String) appName.getText());
							Log.v(TAG, "activating => "+cur_app.getNamespace());
							appDBAdapter.activateApplication(cur_app.getNamespace());
							Application updated_app = appDBAdapter.getAppByNameSpace(cur_app.getNamespace());
							
						} 
						else if(text.equals("OFF")) {
							//deactivate Apps
							Application cur_app = appDBAdapter.getAppByName((String) appName.getText());
							Log.v(TAG, "activating => "+cur_app.getNamespace());
							appDBAdapter.deactivateApplication(cur_app.getNamespace());
						}
					}
				});
	        }
	        return v;
	    }

		private LayoutInflater getSystemService(String layoutInflaterService) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.v(TAG, "get user preference");
		List<UserPreference> prefs = userPrefDBAdapter.getAllPreferences();
		Log.v(TAG, "got user preference => "+prefs);
		
		if(prefs.size() > 0) {
			//show installed apps
			Log.v(TAG, "show installed apps");
			LinearLayout myAppsLayout =(LinearLayout) getActivity().findViewById(R.id.myAppsLayout);
			showInstalledApps(prefs, view);
			
		} else {
				
			final TextView tv = new TextView(context);
			tv.setText("NO APPS INSTALLED");
			LinearLayout.LayoutParams lastTxtParams = new LinearLayout.LayoutParams(400, 400);
			lastTxtParams.setMargins(100, 200, 10, 10);
			tv.setLayoutParams(lastTxtParams);
			tv.setTextSize(18);
			LinearLayout myAppsLayout =(LinearLayout) getActivity().findViewById(R.id.myAppsLayout);
			myAppsLayout.addView(tv);
			
		}
		
		
	}
	
}
