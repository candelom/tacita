package pdnet.usi.ch.usi_display;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

import pdnet.usi.ch.usi_display.*;
import pdnet.usi.ch.usi_display.AllAppsFragmentTab.AllAppsItemAdapter;
import pdnet.usi.ch.usi_display.SettingsFragmentTab.OnServiceSelectedListener;
import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class MainActivity extends MapActivity implements OnServiceSelectedListener {

	
	private static final String TAG = "Main Activity";
	public static final String PDNET_HOST = "http://10.9.6.139:8888/list.xml";
//	public static final String PDNET_HOST = "http://pdnet.inf.unisi.ch:9000/assets/applications/list.xml";
//	public static final String PDNET_HOST = "http://10.61.99.5:8888/list.xml";
//	public static final String PDNET_HOST = "http://192.168.1.39:8888/list.xml";

	private AllAppsItem appName;

	private ApplicationDBAdapter appDBAdapter;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private HashMap<String, String> displays_ids = new HashMap<String, String>();
	BluetoothAdapter mBluetoothAdapter;
	
	
	//1 => bt, 2 => wifi, 3 => nfc
	private int cur_service = 1;
	
	NFCService mService;
	boolean mBound = false;
		
	private String[][] techListsArray;

	
	PendingIntent mNfcPendingIntent;
	IntentFilter[] intentFiltersArray;
	IntentFilter[] mNdefExchangeFilters;
	    
	private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
	private NdefMessage mNdefPushMessage;
	protected MapView mapView;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Log.v(TAG, "CREATED ACTIVITY");
        
        Log.v(TAG, "INITIALIZE DATABASE");
        appDBAdapter = ApplicationDBAdapter.getInstance(this);
		appDBAdapter.open();
//		appDBAdapter.deleteAllApps();

		Log.v(TAG, "INITIALIZE DATABASE");
		userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(this);
		userPrefDBAdapter.open();
        
        ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        ActionBar.Tab allAppsTab = bar.newTab().setIcon(getResources().getDrawable(R.drawable.all_apps_icon));
        
        ActionBar.Tab myAppsTab = bar.newTab().setIcon(getResources().getDrawable(R.drawable.my_apps_icon));
        
        ActionBar.Tab mapTab = bar.newTab().setIcon(getResources().getDrawable(R.drawable.map_icon));
        
        ActionBar.Tab settingsTab = bar.newTab().setIcon(getResources().getDrawable(R.drawable.settings_icon));

        Fragment allAppsFragment = new AllAppsFragmentTab();
        Fragment myAppsFragment = new MyAppsFragmentTab();
        Fragment mapFragment = new MapFragmentTab();
        Fragment settingsFragment = new SettingsFragmentTab();

        allAppsTab.setTabListener(new MyTabsListener(allAppsFragment));
        myAppsTab.setTabListener(new MyTabsListener(myAppsFragment));
        mapTab.setTabListener(new MyTabsListener(mapFragment));
        settingsTab.setTabListener(new MyTabsListener(settingsFragment));

        
        bar.addTab(allAppsTab);
        bar.addTab(myAppsTab);
        bar.addTab(mapTab);
        bar.addTab(settingsTab);
        
        Log.v(TAG, "init display HashMap");
        displays_ids.put("04bc15193e2580", "1");
        displays_ids.put("04b69f193e2580", "1");
        displays_ids.put("04dcbc193e2580", "1");
        displays_ids.put("04afc3193e2580", "1");

        
        Log.v(TAG, "STARTING SERVICE BT");
        setCurService(1);
        Intent intent = new Intent(this, BtService.class);
        startService(intent);
        
        
        Log.v(TAG, "register recevier for nfc");
		IntentFilter filter = new IntentFilter();
		filter.addAction("getRequest");
        this.registerReceiver(mNFCReceiver, filter);
        
        
        Log.v(TAG, "init NFC");
        initNFC();
        
    }
	
	
	public MapView getMapView() {
        if (mapView == null) {
        	Log.v(TAG, "creating map view");
            mapView = (MapView) findViewById(R.id.mapView);
        }
 
        Log.v(TAG, "map view here => "+mapView);
        
        return mapView;
    }
	
	
	@Override
	public void onResume() {
	    super.onResume();
//	    mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
	}

	
	
	
	@Override
	public void onPause() {
	    super.onPause();
//	    mNfcAdapter.disableForegroundDispatch(this);
	}   

	
	
	
	
	@Override
	public void onNewIntent(Intent intent) {
	    Tag tagFromIntent = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    Log.v(TAG, "tag from intent");
	    Log.v(TAG, bytesToHexString(tagFromIntent.getId())); 
	    String id = bytesToHexString(tagFromIntent.getId());
	    if(displays_ids.containsKey(id)) {
	    	String displayID = displays_ids.get(id);
	    	Log.v(TAG, "sending ID => " + displayID + " to NfcService");
	    	//download items
	    	Intent i = new Intent();
	    	i.setAction("nfc");
	    	i.putExtra("id", displayID);
	    	sendBroadcast(i);
	
	    	
	    	
//	    	
//	    	Log.v(TAG, "start activity");
//	    	Intent newsIntent = new Intent();
//		    newsIntent.setClass(this, NewsListFragment.class);
//	    	startActivity(newsIntent);

	    }
	}

	
	public void initNFC() {
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(getApplicationContext());
        
	    // Handle all of our received NFC intents in this activity.
        mPendingIntent = PendingIntent.getActivity(
        	    this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        
        // Intent filters for reading a note from a tag or exchanging over p2p.
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches. 
                                           You should specify only the ones that you need. */
        }
        catch (MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        
        intentFiltersArray = new IntentFilter[] { ndef };
        techListsArray = new String[][] { new String[] { NfcA.class.getName() } };
		
	}
    
    protected class MyTabsListener implements ActionBar.TabListener
    {
    	private Fragment fragment;
    	
    	public MyTabsListener(Fragment fragment)
    	{
    		this.fragment = fragment;
    	}
    	
    	
		@Override
		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			
			Log.v(TAG, "SELECTED TAB => "+tab.getText());
			Log.v(TAG, "/*** FRAGMENT ***/ => "+fragment);
			ft.replace(R.id.fragment_place, fragment, null);
		}
    
		@Override
		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.replace(R.id.fragment_place, fragment, null);
		}
		
		@Override
		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			// TODO Auto-generated method stub
			ft.remove(fragment);
		}
    }
    
    
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
        this.unregisterReceiver(mNFCReceiver);

//		appDBAdapter.close();
//		userPrefDBAdapter.close();
	}
	
	
	
	
	public ApplicationDBAdapter getApplicationDBAdapter() {
		
		return appDBAdapter;
	}
	
	

	
	public UserPreferenceDBAdapter getUserPreferenceDBAdapter() {
		
		return userPrefDBAdapter;
	}
	
	
	public int getCurService() {
		return cur_service;
	}
	
	public void setCurService(int cur_service) {
		Log.v(TAG, "set cur service to => "+ cur_service);
		this.cur_service = cur_service;
	}
	
	public void startNewService(int service) {
		
		if(service == 1) {
			
			//start bt service
			Log.v(TAG, "start bt service");
			setCurService(1);

			 Intent intent = new Intent(this, BtService.class);
		     startService(intent);
			
		} else if(service == 2) {
			
			Log.v(TAG, "start wifi service");
			setCurService(2);

			 Intent intent = new Intent(this, WiFiService.class);
			 startService(intent);
			
			
		} else if(service == 3) {
			
			Log.v(TAG, "start nfc service");
			setCurService(3);
			 Intent intent = new Intent(this, NFCService.class);
		     startService(intent);
			
			
		}
		
	}


	public void stopCurService(int cur_service) {
		
		Log.v(TAG, "stop cur service");
		if(cur_service == 1) {
			
			Intent intent = new Intent(this, BtService.class);
			stopService(intent);

			
		} else if(cur_service == 2) {
			
			Intent intent = new Intent(this, WiFiService.class);
			stopService(intent);

			
		} else if(cur_service == 3) {
			
			Intent intent = new Intent(this, NFCService.class);
			stopService(intent);
			
		}
	}

	
	
	private String bytesToHexString(byte[] src) {
	    StringBuilder stringBuilder = new StringBuilder("");
	    if (src == null || src.length <= 0) {
	        return null;
	    }

	    char[] buffer = new char[2];
	    for (int i = 0; i < src.length; i++) {
	        buffer[0] = Character.forDigit((src[i] >>> 4) & 0x0F, 16);  
	        buffer[1] = Character.forDigit(src[i] & 0x0F, 16);  
	        System.out.println(buffer);
	        stringBuilder.append(buffer);
	    }

	    return stringBuilder.toString();
	}


	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	
	
	
	
    public interface OnChangeFragmentListener {
        public void goToAppInfoView(AllAppsItem appName);
    }

    
    
    public void goToAppInfoView(AllAppsItem appName) {
		
		//change to VIEW FRAGMENT
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        setCurAppName(appName);
        ViewAppFragment vpf = new ViewAppFragment();
        fragmentTransaction.replace(R.id.fragment_place, vpf);
        fragmentTransaction.commit();
	}


	@Override
	public ApplicationDBAdapter getAppDBAdapter() {
		return appDBAdapter;
	}


	@Override
	public UserPreferenceDBAdapter getUserPrefDBAdapter() {
		return userPrefDBAdapter;
	}


	@Override
	public Context getContext() {
		return this;
	}



	@Override
	public void setCurAppName(AllAppsItem appName) {
		// TODO Auto-generated method stub
		this.appName = appName;
	}
    
	
	@Override
	public AllAppsItem getCurAppName() {
		// TODO Auto-generated method stub
		return appName;
	}
    
    
	
	public void disableNFC() {
		mNfcAdapter.disableForegroundDispatch(this);
	}


	@Override
	public void activateNFC() {
		// TODO Auto-generated method stub
	    mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
	
	}


	@Override
	public NfcAdapter getNFCAdapter() {
		// TODO Auto-generated method stub
		return mNfcAdapter;
	}
	
	
	 private final BroadcastReceiver mNFCReceiver = new BroadcastReceiver() {
	        
			@Override
	        public void onReceive(Context context, Intent intent) {
	            String action = intent.getAction();

	            // When discovery finds a device
	            if (action.equals("getRequest")) {
		          Log.v(TAG, "really starting activity");
	            
	            } 
	        }
	    };
}
