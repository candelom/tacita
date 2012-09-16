package pdnet.usi.ch.usi_display;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pdnet.usi.ch.usi_display.SettingsFragmentTab.OnServiceSelectedListener;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;

public class MapFragmentTab extends Fragment {
	
	
	public static final String TAG = "Map";
	public static final String PDNET_HOST = "http://pdnet.inf.unisi.ch:9000/assets/displays/list.xml";
//	public static final String PDNET_HOST = "http://10.9.6.139:8888/displays.xml";
//	public static final String PDNET_HOST = "http://10.61.99.5:8888/displays.xml";
//	public static final String PDNET_HOST = "http://192.168.1.39:8888/display.xml";
    OnServiceSelectedListener mListener;
	
	
	private boolean isUpdated = false;
	private Context context;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private ApplicationDBAdapter appDBAdapter;
	protected ViewGroup mapContainer;
    protected MapView mapView;
	private View view = null;
	private ArrayList<Display> displays = new ArrayList<Display>();
	
//	public MapFragmentTab(ApplicationDBAdapter appDBAdapter, UserPreferenceDBAdapter userPrefDBAdapter, Context context) {
//		this.appDBAdapter = appDBAdapter;
//		this.userPrefDBAdapter = userPrefDBAdapter;
//		this.context = context;
//	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnServiceSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnServiceSelectedListener");
        }
    }

	
	private class DownloadDisplayXMLFile extends AsyncTask<Void,Void, ArrayList<Display>>
    {
		Context context;
		
		public DownloadDisplayXMLFile(Context context) {
			this.context = context;
		}
		
        @Override
        protected ArrayList<Display> doInBackground(Void... params) {
//         ArrayList<Display> values = readXMLFile(PDNET_HOST);
           ArrayList<Display> values = readXMLFile("file:///android_asset/server/displays.xml");
           Log.v(TAG, "FILE HAS BEEN READ");
           return values;
        }
        
        

        @Override
        protected void onPostExecute(final ArrayList<Display> result)
        {
        	
        	Log.v(TAG, "result => "+null);
        	getActivity().runOnUiThread(new Runnable() {
				public void run() {
					isUpdated = true;
					Log.v(TAG, "set map markers");
					ArrayList<Display> fake_displays = new ArrayList<Display>();
			        ArrayList<String> fake_supported_apps = new ArrayList<String>();
			        fake_supported_apps.add("News");
			        fake_supported_apps.add("Weather");
			        
			        fake_displays.add(new Display("1", 46.011030f, 8.957926f, "usi - 1", fake_supported_apps));
			        fake_displays.add(new Display("1", 46.011108f, 8.957424f, "usi - 2", fake_supported_apps));
			        
			        Display fake_first = fake_displays.get(0);
			        Display real_first = result.get(0);
			        
			        Log.v(TAG, "/*** fake display ***/\n"+ fake_first.printDisplay());
			        Log.v(TAG, "/*** real display ***/\n"+ real_first.printDisplay());

					setMapMarkers(result);
				}
			});
        }
    }
	
	
	
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v(TAG, "created Map View");
		// Retrieve the map and initial extent from XML layout
		this.appDBAdapter = mListener.getAppDBAdapter();
		this.userPrefDBAdapter = mListener.getUserPrefDBAdapter();
		this.context = mListener.getContext();
		//read all apps
		if(!isUpdated) {
			Log.v(TAG, "download display");
	        DownloadDisplayXMLFile task = new DownloadDisplayXMLFile(context);
	        task.execute();
		}
		
		Log.v(TAG, "view => "+view);
		if(view == null) {
			view  = inflater.inflate(R.layout.map_layout, container, false);
			this.view = view;
		}
		return view;
	}
	
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		Log.v(TAG, "checking map => "+mapView);
		if(mapView == null) {
			setMap();
		}
	}
	
	
	public void setMap() {
		
		Log.v(TAG, "setting map");
		mapView = ((MainActivity) getActivity()).getMapView();
		Log.v(TAG, "got map view => "+mapView);
		
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(true);
//        mapView.setSatellite(true);
        
        MapController controller = mapView.getController();
        float lat = 46.011048f;
        float lng = 8.957701f;
        GeoPoint gp = new GeoPoint((int)(lat * 1E6), (int)(lng * 1E6));
        controller.animateTo(gp);
        controller.setZoom(20);
        
//        ArrayList<Display> fake_displays = new ArrayList<Display>();
//        ArrayList<String> fake_supported_apps = new ArrayList<String>();
//        fake_supported_apps.add("News");
//        fake_supported_apps.add("Weather");
//        
//        fake_displays.add(new Display("1", 46.011030f, 8.957926f, "usi - 1", fake_supported_apps));
//        fake_displays.add(new Display("1", 46.011108f, 8.957424f, "usi - 2", fake_supported_apps));

//        setMapMarkers(fake_displays);
        
        
        setCurrentPosition();
	}
	
	
	
	public void setMapMarkers(ArrayList<Display> pub_displays) {
		
		Log.v(TAG, "setting markers => "+ mapView);
        List<Overlay> mapOverlays = mapView.getOverlays();
        Log.v(TAG, "overlays => "+mapOverlays);
        Drawable drawable = this.getResources().getDrawable(R.drawable.display_marker);
        HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, context);
        
        Log.v(TAG, "SETTING "+pub_displays.size()+" MARKERS");
        
        for(Display cur_display : pub_displays) {
        	Log.v(TAG, "/*** adding marker ***/\n"+
        			"Name : "+ cur_display.getName()+"\n"+
        			"Lat : "+ cur_display.getLatitude()+"\n"+
        			"Name : "+ cur_display.getLongitude()+"\n\n");
        			
        	
        	GeoPoint point = new GeoPoint((int) (cur_display.getLatitude()*1E6), (int) (cur_display.getLongitude()*1E6));
        	String snippet = createSnippet(cur_display.getSupportedApps());
        	
        	Log.v(TAG, "supported apps => " + cur_display.getSupportedApps());
        	
        	OverlayItem overlayitem = new OverlayItem(point, cur_display.getName(), snippet);
        	itemizedoverlay.addOverlay(overlayitem);
        	mapOverlays.add(itemizedoverlay);
        }
        
        Log.v(TAG, "overlays => "+mapOverlays);
	}
	
	
	
	public String createSnippet(ArrayList<String> supportedApps) {
		
		String snippet = "";
		int i = 0;
		for(String appName : supportedApps) {
			if(i == supportedApps.size()-1) {
				snippet += appName;
			} else {
				snippet += appName + ", ";
			}
		}
		
		return snippet;
	}
	
	
	
	public ArrayList<Display> getDisplays() {
		
		return displays;
	}
	
	
	
	/**
	 * Reads XML file containing display information
	 * @param path
	 */
	public ArrayList<Display> readXMLFile(String path) {
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
//			Document doc = db.parse(new URL(path).openStream());
			
			AssetManager assetManager = context.getAssets();
			Document doc = db.parse(assetManager.open("server/displays.xml"));
			Element root = doc.getDocumentElement();

			//get the list of displays
			NodeList pd_list = root.getElementsByTagName("display");
			if(pd_list != null && pd_list.getLength() > 0) {
				for(int i = 0 ; i < pd_list.getLength();i++) {
					
					//get the display element
					Element cur_display = (Element)pd_list.item(i);
					Log.v("cur_display", cur_display.toString());
					String id= getTagValue("id", cur_display);
					String latitude = getTagValue("latitude", cur_display);
					String longitude = getTagValue("longitude", cur_display);
					String name = getTagValue("name", cur_display);
					
					ArrayList<String> supportedApps = new ArrayList<String>();
					
                    NodeList app_list = cur_display.getElementsByTagName("app");
                    for(int j = 0 ; j < app_list.getLength(); j++) {
                    	//get the app element
    					Element cur_app = (Element) app_list.item(j);
    					String app_name = getTagValue("name", cur_app);
    					supportedApps.add(app_name);
                    }
                    
					Log.v(TAG, "adding display");
					Display display = new Display(id, new Float(latitude), new Float(longitude), name, supportedApps);
					displays.add(display);
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
		}
		return displays;
	}
	
	
	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}
	
	
	@Override
    public void onDestroyView() {
        super.onDestroyView();
//        removeMapView();
    }  

	
	public void removeMapView() {
		
		LinearLayout linearLayout = (LinearLayout) getActivity().findViewById(R.id.mapLayout);
		linearLayout.removeView(mapView);
	}
	
	
	public void setCurrentPosition() {

		Log.v(TAG, "setCurrent position");
		// Acquire a reference to the system Location Manager
		LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		    	// Called when a new location is found by the network location provider.
		    	Log.v(TAG, "got location changed");
		    	List<Overlay> mapOverlays = mapView.getOverlays();
		    	if(mapOverlays.size() > 0) {
		    		containUserMarker(mapOverlays);
		    	}
		    	
		    	Log.v(TAG, context.getResources()+"");
		        Drawable drawable = context.getResources().getDrawable(R.drawable.user_position);
		        Log.v(TAG, "creating UserMarkerOverlay -=> "+drawable+" => "+context);
		        UserMarkerOverlay userOverlay = new UserMarkerOverlay(drawable, context);
		    	
		        Log.v(TAG, "adding marker for user with data Lat : "+location.getLatitude()+" and Long : "+location.getLongitude()+"\n");
	        	
		        for(Display display : displays) {
		        	
		        	float cur_lat = display.getLatitude();
		        	float cur_long = display.getLongitude();
		        	
		        	Location displayLocation = new Location("Location");
		        	displayLocation.setLatitude(cur_lat);
		        	displayLocation.setLongitude(cur_long);
		        	
		        	float distance = location.distanceTo(displayLocation);
		        	Log.v(TAG, "DISTANCE BETWEEN USER AND NEAREST DISPLAY IS => "+distance);
		        	if(location.distanceTo(displayLocation) < 100) {
		        		//draw red circle
		        		Log.v(TAG, "draw red circle");
		        		
		        		Projection projection = mapView.getProjection();

		        		GeoPoint circlePoint = new GeoPoint((int) (displayLocation.getLatitude()*1E6), (int) (displayLocation.getLongitude()*1E6));
		        		OverlayItem circleItem = new OverlayItem(circlePoint, "Display", null);
		        		
	                    Point pt = new Point();

	                    projection.toPixels(circleItem.getPoint(), pt);
	                    float circleRadius = projection.metersToEquatorPixels(10);

	                    Paint innerCirclePaint;

	                    innerCirclePaint = new Paint();
	                    innerCirclePaint.setColor(Color.RED);
	                    innerCirclePaint.setAlpha(25);
	                    innerCirclePaint.setAntiAlias(true);

	                    innerCirclePaint.setStyle(Paint.Style.FILL);
	                    Canvas canvas = new Canvas();
	                    canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, innerCirclePaint);
		        		mapView.draw(canvas);
		        	}
		        }
		        
		        GeoPoint point = new GeoPoint((int) (location.getLatitude()*1E6), (int) (location.getLongitude()*1E6));
		        
		        OverlayItem overlayItem = new OverlayItem(point, "Your position", "Lat : "+location.getLatitude()+"\n"+"Long : "+location.getLongitude());
	        	userOverlay.addOverlay(overlayItem);
	        	mapOverlays.add(userOverlay);
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    
		    public void onProviderEnabled(String provider) {}

		    
		    public void onProviderDisabled(String provider) {}
		  };

		// Register the listener with the Location Manager to receive location updates
		Log.v(TAG, "requesting current location");
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}
	
	
	public void containUserMarker(List<Overlay> items) {
		int j = 0;
		int toErase = 0;
		for(Overlay item : items) {
			if(item instanceof UserMarkerOverlay) {
				toErase = j;
			}
			j++;
		}
		items.remove(toErase);
	}
	
	
	
    public class HelloItemizedOverlay extends ItemizedOverlay {

    	
    	private static final String TAG = "HelloItemizedOverlay";
    	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	Context mContext;

    	
    	public HelloItemizedOverlay(Drawable defaultMarker, Context context) {
    		super(boundCenterBottom(defaultMarker));
    		mContext = context;
    	}
    	
    	

    	@Override
    	protected OverlayItem createItem(int i) {
    	  return mOverlays.get(i);
    	}
    	
    	
    	@Override
    	protected boolean onTap(int index) {
    	  OverlayItem item = mOverlays.get(index);
//    	  AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
//    	  dialog.setTitle(item.getTitle());
//    	  dialog.setMessage(item.getSnippet());
//    	  dialog.show();
    	  
    	  final Dialog dialog = new Dialog(mContext);

    	  dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
    	  dialog.setContentView(R.layout.dialog_layout);
    	  
    	  LinearLayout dialogLayout = (LinearLayout) dialog.findViewById(R.id.dialogLayout);
    	  
//    	  createSnippetView(item.getSnippet());
    	  String[] supportedApps = item.getSnippet().split(", ");
    		
    	  for(int j = 0; j < supportedApps.length; j++) {
    			
    				final String appName = supportedApps[j];
    				LinearLayout ll = new LinearLayout(mContext);
    				LinearLayout.LayoutParams adaptLayout = new LinearLayout.LayoutParams(300, 80);
    				ll.setOrientation(LinearLayout.HORIZONTAL);
    				ll.setLayoutParams(adaptLayout);
    				
//    				Log.v(TAG, "herre => "+appName);
//    				Application app = appDBAdapter.getAppByName(appName);
    				
    				ImageView img = new ImageView(mContext);
    				int id = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + appName+"_icon", null, null);

    				img.setImageResource(id);
    				
    				
    				img.setLayoutParams(new FrameLayout.LayoutParams(100, 40));
    				String icon = "";
    				Log.v(TAG, "app name => "+appName);
    				final String icon_img = icon;
    				
    				TextView tv = new TextView(mContext);
    				tv.setText(appName);
    				tv.setTextSize(15);
    				LayoutParams params = new LayoutParams(200, 40);
    				tv.setLayoutParams(params);
    				ll.addView(img);
    				ll.addView(tv);
    				
    				//set on click listener
    				ll.setOnClickListener(new View.OnClickListener() {
    					@Override
    					public void onClick(View v) {
    						Application app = appDBAdapter.getAppByName(appName);
    						Log.v(TAG, "app name space => "+ app.getNamespace());
    						Log.v(TAG, "tapped => "+ userPrefDBAdapter);
    						if(userPrefDBAdapter.getPreference(app.getNamespace()) != null) {
    							//app is installed
    							goToPluginView(app.getNamespace());
    						} else {
    							goToAppInfoView(new AllAppsItem(appName, icon_img));
    							dialog.dismiss();
    						}
    					}
    				});
    				dialogLayout.addView(ll);
    		}
    	  
    	  dialog.setTitle("Supported Apps");
    	  dialog.show();
    	  dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.supported);
    	  return true;
    	}
    	

    	@Override
    	public int size() {
    	  return mOverlays.size();
    	}
    	

    	public void addOverlay(OverlayItem overlay) {
    	    mOverlays.add(overlay);
    	    populate();
    	}
    	
    	
    	
    	@Override
        public void draw(Canvas canvas, MapView mapV, boolean shadow){
    		Log.v(TAG, "DRAWING MARKER");
            if(shadow){
            	
            	for(OverlayItem item : mOverlays) {
            		
            		Projection projection = mapV.getProjection();

                    Point pt = new Point();

                    projection.toPixels(item.getPoint() ,pt);
                    float circleRadius = projection.metersToEquatorPixels(10);

                    Paint innerCirclePaint;
                    innerCirclePaint = new Paint();
                    innerCirclePaint.setColor(Color.BLUE);
                    innerCirclePaint.setAlpha(25);
                    innerCirclePaint.setAntiAlias(true);

                    innerCirclePaint.setStyle(Paint.Style.FILL);

                    canvas.drawCircle((float)pt.x, (float)pt.y, circleRadius, innerCirclePaint);
                    
                    
                    // Read the image
                    Bitmap markerImage = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.display_marker);

                    // Draw it, centered around the given coordinates
                    Log.v(TAG, "drawing marker");
                    canvas.drawBitmap(markerImage, pt.x - markerImage.getWidth() / 2, pt.y - markerImage.getHeight() / 2, null);


            	}
            }
        }
    	
    }

    
    
    public void goToAppInfoView(AllAppsItem appName) {
		
		//change to VIEW FRAGMENT
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        getActivity().getActionBar().setSelectedNavigationItem(0);
        mListener.setCurAppName(appName);
        ViewAppFragment vpf = new ViewAppFragment();
        fragmentTransaction.replace(R.id.fragment_place, vpf);
        fragmentTransaction.commit();
        
		
	}
    
    
    public void goToPluginView(String appNameSpace) {
		
    	  Log.v(TAG, "loading WebView");
	      Intent intent = new Intent();
	      intent.setClass(getActivity(), MobilePluginActivity.class);
	      intent.putExtra("appNameSpace", appNameSpace);
	      startActivity(intent);
	}

    
    
    
    
    
    
	
	
    public class UserMarkerOverlay extends ItemizedOverlay {

    	
    	private static final String TAG = "HelloItemizedOverlay";
    	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
    	Context mContext;
    	private float cur_lat;
    	private float cur_long;
    	private GeoPoint geopoint;
    	private int rad;

    	
    	public UserMarkerOverlay(Drawable defaultMarker, Context context) {
    		super(boundCenterBottom(defaultMarker));
    		Log.v(TAG, "CREATING 2");
    		mContext = context;
    	}
    	
    	

    	@Override
    	protected OverlayItem createItem(int i) {
    	  return mOverlays.get(i);
    	}
    	
    	
    	@Override
    	protected boolean onTap(int index) {
    	  OverlayItem item = mOverlays.get(index);
    	  
    	  final Dialog dialog = new Dialog(mContext);
    	  dialog.requestWindowFeature(Window.FEATURE_LEFT_ICON);
    	  dialog.setContentView(R.layout.user_dialog);
    	  dialog.setTitle("Your position");
    	  dialog.show();
    	  dialog.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.position);
    	  return true;
    	}
    	
    	
    	@Override
    	public int size() {
    	  return mOverlays.size();
    	}
    	

    	public void addOverlay(OverlayItem overlay) {
    	    mOverlays.add(overlay);
    	    populate();
    	}
   
    }

    
    
}
