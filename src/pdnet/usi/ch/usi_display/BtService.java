package pdnet.usi.ch.usi_display;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;


public class BtService extends Service {

	private final static String TAG = "Bluetooth Service";

	private Looper mServiceLooper;
	private LocationListener locationListener;
	static SocketManager socketManager;
	private BluetoothAdapter mBluetoothAdapter;
	private String[] displays = new String[]{"USI-DISPLAY-1", "USI-Display-2", "USI-Display-5"};
    private String content = "";

	private static ApplicationDBAdapter appDBAdapter;
	private static UserPreferenceDBAdapter userPrefDBAdapter;

	private ArrayList<BluetoothDevice> devices = new ArrayList<BluetoothDevice>();
	private Context mContext = this;
	//private ConnectThread mConnectThread;

	
	//used to stop Runnable that perform log of battery life
	private static boolean STOPPED = false;
	private static boolean BT_STOPPED = false;
	private static Date start_time = null;
	private static final String NAME = "bt_device";
	private static String curDisplay;
	
	
    //Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device
	
    private int mState;
    private Handler mServiceHandler;

	
	
	private Handler handler = new Handler();    
    private Runnable runnable = new Runnable() {   
    	
        public void run() {
        	if(!STOPPED) {
        		Log.v(TAG, "battery level => "+getBatteryLevel()+"%");
	            getBatteryLevel();
	            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	            Date date = new Date();
	            System.out.println(dateFormat.format(date));
	            Log.v(TAG, "num of devices => "+devices.size());
	            updateLogFile(dateFormat.format(date), getBatteryLevel());
	            handler.postDelayed(this, 600000);
        	}
        } 
    };

    
    public synchronized void setState(int state) {
    	mState = state;
    }

    
    /**
     * Return the current connection state. 
     */
    public synchronized int getState() {
        return mState;
    }

    
	public void setCurDisplay(String curDisplay) {
		
		this.curDisplay = curDisplay;
	}
	
	
	public static String getCurDisplay() {
		
		return curDisplay;
	}

	
	private BroadcastReceiver mTriggerReceiver = new BroadcastReceiver() {
	    
		@Override
	    public void onReceive(Context context, Intent intent) {
	        //handle the broadcast event here
      	    Log.v(TAG, "RECEIVED NFC INTENT");
      	  
	    	String action = intent.getAction();
	    	if (action.equals("nfc")) {
	    		System.out.println("GOT THE INTENT");
	    		String displayID = intent.getStringExtra("id");
	    		Log.v(TAG, "GOT ID => " + displayID);
	    		Log.v(TAG, "get active apps");
				socketManager.sendGetRequest(displayID);
	    	}
	    }
	};
	

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread becausef the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		Log.v("BtService", "SERVICE CREATED");
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
    	socketManager = SocketManager.getInstance(this);
    	socketManager.createDisplaySocket();
		
    	
		Log.v(TAG, "register recevier for nfc");
		IntentFilter filter = new IntentFilter();
		filter.addAction("nfc");
        this.registerReceiver(mTriggerReceiver, filter);
		
	}

	
	public static Thread performOnBackgroundThread(final Runnable runnable) {
	    final Thread t = new Thread() {
	        @Override
	        public void run() {
	            try {
	                runnable.run();
	            } finally {
	            }
	        }
	    };
	    t.start();
	    return t;
	}

	
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.v("BtService", "SERVICE STARTED");
		

		
		appDBAdapter = ApplicationDBAdapter.getInstance(this);
		
//		appDBAdapter.open();
//		appDBAdapter.deleteAllApps();
		
		userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(this);
//		userPrefDBAdapter.open();
//		userPrefDBAdapter.deleteAll();
		
		
		Log.v(TAG, "USER PREF DB ADAPTER => "+userPrefDBAdapter);
		 mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

	       // If BT is not on, request that it be enabled.
	       if(!mBluetoothAdapter.isEnabled()) {
	            Intent btIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
	            btIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	            mContext.startActivity(btIntent);
	       }
	       
	       Log.v(TAG, "start discovery");
	       doDiscovery();
	       start_time = getCurTime();
	       
	       // Register for broadcasts when a device is discovered
	       IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
	       this.registerReceiver(mReceiver, filter);

	        // Register for broadcasts when discovery has finished
	        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
	        this.registerReceiver(mReceiver, filter);
	        
	        IntentFilter filterr = new IntentFilter();
			filter.addAction("nfc");
	        this.registerReceiver(mTriggerReceiver, filterr);
//	        checkMediaAvailability();
//	        runnable.run();
	        
	        
		// start ID so we know which request we're stopping when we finish the job
	        
		// If we get killed, after returning from here, restart
		return START_NOT_STICKY;
	}

	
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	
	@Override
	public void onDestroy() {
		
		Log.v("BtService", "stop service");
		super.onDestroy();
//		appDBAdapter.close();
//		userPrefDBAdapter.close();
		for(String app : socketManager.activeSockets.keySet()) {
			socketManager.activeSockets.get(app).close();
		}
		
		
		
		// Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
        	mBluetoothAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
        //stop the Runnable
        STOPPED = true;
        this.unregisterReceiver(mTriggerReceiver);
	}
	
	
	public boolean isContained(String name, String[] array) {
		
		for(int j = 0; j < array.length; j++) {
			if(array[j].equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	
	// The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
		@Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            	Log.v("Main Activity", "FOUND DEVICE");
                //Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.v("Main Acitvity", "DEVICE NAME : "+device.getName());
                Log.v("Main Activity", "DEVICE ADDRESS : "+device.getAddress());
                //If it's already paired, skip it, because it's been listed already
                if (!devices.contains(device)) {
                    Log.v("Main Activity", "adding to devices "+device.getAddress());
                	devices.add(device);
                }
                
                //get display id and send preferences
                if(isContained(device.getName(), displays)) {
                	//assume name display has the form USI-Display-{ID}
                	String displayID = "1";
                	Log.v(TAG, "socket manager obj => "+socketManager);
                	socketManager.printProximityMessage(displayID);
                	Log.v(TAG, "trigger preferences display ID => "+displayID+" & name => "+device.getName());
                	Log.v(TAG, "socket Manager => "+socketManager);
                	socketManager.triggerPreferences(displayID);
                }
	          } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
	        	  Log.v(TAG, "restart discovery");
	        	  start_time = getCurTime();
	        	  mBluetoothAdapter.startDiscovery();
	        	  devices.clear();
	          } 
        }
    };
	
    
    public SocketManager getSocketManager() {
    	
    	return socketManager;
    }
    
    
    public Date getCurTime() {
    	Date date = new Date();
    	return date;
    }
    
	
	/**
     * Start device discover with the BluetoothAdapter
     */
    private void doDiscovery() {

        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
        	Log.v(TAG, "canceling discovery");
        	mBluetoothAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBluetoothAdapter.startDiscovery();
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
		         out.write("Time \t Battery (%) \n\n".getBytes());
		         out.close();
		    } catch (Exception e) {
		         e.printStackTrace();
		    }
	}

	
	
	public void createScanFile() {
		
		Log.v(TAG, "CREATE SCAN BT FILE");

	    // We can read and write the media
	    File myDir = new File("/sdcard/Logs");
	    myDir.mkdirs();
	    Random generator = new Random();
	    int n = 10000;
	    n = generator.nextInt(n);
	    File file = new File(myDir, "scan_bt_log.txt");
	    if (file.exists ()) file.delete(); 
	    try {
	         FileOutputStream out = new FileOutputStream(file);
	         out.write("Scan Duration (sec) \t N¼ Devices \n\n".getBytes());
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
	
	
	
	public void updateScanDurationFile(long time, int num_of_devices) {
		
		FileWriter f;
		try {
			f = new FileWriter(new File("/sdcard/Logs/scan_bt_log.txt"), true);
			String row = time+" \t "+num_of_devices+"\n";
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
	
	
	
	
	public void readFile(String fileName) {
		FileInputStream fis;
		try {
			fis = openFileInput(fileName);
			InputStreamReader isr = new InputStreamReader (fis) ;
	        BufferedReader buffreader = new BufferedReader (isr) ;

	        String readString = buffreader.readLine() ;
	        while ( readString != null ) {
                content += readString+"\n" ;
                readString = buffreader.readLine() ;
	        }
	        isr.close();
			fis.close();
			Log.v(TAG, "CURRENT CONTENT \n" + content +"\n\n");
			//sending the content to log_file on the server
			
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public void checkMediaAvailability() {
		
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
	
		if (Environment.MEDIA_MOUNTED.equals(state)) {
		    // We can read and write the media
		    mExternalStorageAvailable = mExternalStorageWriteable = true;
		    createLogFile();
		    createScanFile();
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
		    // We can only read the media
		    mExternalStorageAvailable = true;
		    mExternalStorageWriteable = false;
		} else {
		    // Something else is wrong. It may be one of many other states, but all we need
		    //  to know is we can neither read nor write
		    mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}

	
	
}