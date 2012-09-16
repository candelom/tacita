package pdnet.usi.ch.usi_display;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;


public class WiFiService extends Service {

	private final String TAG = "location_service";

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;
	static LocationManager locationManager;
	private LocationListener locationListener;
	static SocketManager socketManager;

	private static ApplicationDBAdapter appDBAdapter;
	private static UserPreferenceDBAdapter userPrefDBAdapter;

	
	//used to stop Runnable that perform log of battery life
	private static boolean STOPPED = false;
	
	private static String curDisplay;
	
	private Handler handler = new Handler();    
    private Runnable runnable = new Runnable() {   
    	
        public void run() {
        	if(!STOPPED) {
        		Log.v(TAG, "battery level => "+getBatteryLevel()+"%");
	            getBatteryLevel();
	            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	            Date date = new Date();
	            System.out.println(dateFormat.format(date));
	            updateLogFile(dateFormat.format(date), getBatteryLevel());
	            handler.postDelayed(this, 600000);
        	}
        } 
    };

    
	public void setCurDisplay(String curDisplay) {
		
		this.curDisplay = curDisplay;
	}
	
	
	public static String getCurDisplay() {
		
		return curDisplay;
	}
	
	
	
	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {

		public ServiceHandler(Looper looper) {
			super(looper);
			socketManager = new SocketManager(getApplicationContext());
		}
	
		@Override
		public void handleMessage(Message msg) {
			Log.v("LocationService", "reading "+socketManager.PDNET_HOST + socketManager.DISPLAYS_URL);
			socketManager.readXMLFile(socketManager.PDNET_HOST + socketManager.DISPLAYS_URL);
			Log.v(TAG, "run timer");
			createLogFile();

			// Acquire a reference to the system Location Manager
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

			// Define a listener that responds to location updates
			locationListener = new LocationListener() {
				@Override
				public void onLocationChanged(Location location) {
					if(location != null) {
						String user_lat = Double.toString(location.getLatitude());
						String user_lng = Double.toString(location.getLongitude());
						String accuracy = Double.toString(location.getAccuracy());
						socketManager.printCoordinates(user_lat, user_lng, accuracy);
						// Called when a new location is found by the network location provider.
						String displayID = socketManager.checkNearDisplays(location);
						if(displayID != null) {
							setCurDisplay(displayID);
							socketManager.printProximityMessage(displayID);
							socketManager.triggerPreferences(displayID);
						} 
						else {
							setCurDisplay("");
							System.out.println("/*** NO DISPLAYS IN PROXIMITY ***/");
						}
					}
				}


				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {

					System.out.println("/*** ON STATUS CHANGED ***/");
					
				}

				@Override
				public void onProviderEnabled(String provider) {

					System.out.println("/*** ON PROVIDER ENABLED ***/");

				}

				@Override
				public void onProviderDisabled(String provider) {

					System.out.println("/*** PROVIDER DISABLED ***/");
				}
			};

			// Register the listener with the Location Manager to receive location updates
			locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			
			
		}

	}

	
	


	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread becausef the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		Log.v("LocationService", "create service");
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

//		// Get the HandlerThread's Looper and use it for our Handler 
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		   
	        
	}

	

	
	

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("LocationService", "service starting");

		appDBAdapter = ApplicationDBAdapter.getInstance(this);
		Log.v("LocationService", "here"+appDBAdapter);
		appDBAdapter.open();
		
		userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(this);
		userPrefDBAdapter.open();
		
	        
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_NOT_STICKY;
	}

	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
	@Override
	public void onDestroy() {
		Log.v("LocationService", "stop service");
		super.onDestroy();
		appDBAdapter.close();
		userPrefDBAdapter.close();
		
        //stop the Runnable
        STOPPED = true;
        Log.v(TAG, "sending file");
	}

	
	
	public boolean isContained(String name, String[] array) {
		
		for(int j = 0; j < array.length; j++) {
			if(array[j].equals(name)) {
				return true;
			}
		}
		
		return false;
		
	}
	
	
	

    
	public void stopReceivingUpdates() {
		Log.v("LocationService", "STOP receiving updates");
		locationManager.removeUpdates(locationListener);
	}
	

	
	public static UserPreferenceDBAdapter getUserPreferenceDBAdapter() {

		return userPrefDBAdapter;
	}

	
	public static ApplicationDBAdapter getApplicationDBAdapter() {
		
		return appDBAdapter;
	}

	
	
	public float getBatteryLevel() {
		
		IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		Intent battery = registerReceiver(null, ifilter);
		int level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		float batteryPct = (level / (float)scale)*100;
		return batteryPct;
	}
	

	public void createLogFile() {
	
			Log.v(TAG, "CREATE LOG FILE");

		    // We can read and write the media
		    File myDir = new File("/sdcard/Logs");
		    myDir.mkdirs();
		    Random generator = new Random();
		    int n = 10000;
		    n = generator.nextInt(n);
		    File file = new File(myDir, "battery_bt_log.txt");
		    if (file.exists ()) file.delete(); 
		    try {
		         FileOutputStream out = new FileOutputStream(file);
		         out.write("Time \t Battery (%)\n\n".getBytes());
		         out.close();

		    } catch (Exception e) {
		         e.printStackTrace();
		    }
	}

	
	
	public void updateLogFile(String time, float battery_pctg) {
		Log.v(TAG, "UPDATE LOG FILE");
		FileWriter f;
		try {
			f = new FileWriter(new File("/sdcard/Logs/battery_bt_log.txt"), true);
			String row = time+"\t"+battery_pctg+"\n";
			f.append(row);
			f.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
}