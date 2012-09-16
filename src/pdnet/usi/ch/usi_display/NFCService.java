package pdnet.usi.ch.usi_display;

import java.util.Date;

import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.location.LocationListener;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;


public class NFCService extends Service {

	
	// Binder given to clients
//    private final IBinder mBinder = new LocalBinder();
	
	private final static String TAG = "NFC Service";

	private Looper mServiceLooper;
	private LocationListener locationListener;
	static SocketManager socketManager;
    private String content = "";
	private Context mContext = this;
    private Handler mServiceHandler;
	private static ApplicationDBAdapter appDBAdapter;
	private static UserPreferenceDBAdapter userPrefDBAdapter;
    
    private Runnable downloadRunnable = new Runnable() {   
        public void run() {
        	socketManager = new SocketManager(mContext);
    		socketManager.readXMLFile(socketManager.PDNET_HOST + socketManager.DISPLAYS_URL);
        } 
    };
    

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread becausef the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		Log.v(TAG, "create service");
		HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Register for broadcasts when discovery has finished
		Log.v(TAG, "unregister service");
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("ciao");
        this.registerReceiver(mTriggerReceiver, filter);
//		// Get the HandlerThread's Looper and use it for our Handler 
//		mServiceLooper = thread.getLooper();
//		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	
	private BroadcastReceiver mTriggerReceiver = new BroadcastReceiver() {
	    
		@Override
	    public void onReceive(Context context, Intent intent) {
	        //handle the broadcast event here
	    	String action = intent.getAction();
	    	if (action.equals("nfc")) {
	    		System.out.println("GOT THE INTENT");
	    		String displayID = intent.getStringExtra("id");
	    		Log.v(TAG, "GOT ID => " + displayID);
            	socketManager.printProximityMessage("5");
            	Log.v(TAG, "trigger preferences display ID => "+displayID);
            	socketManager.triggerPreferences("5");
	    	}
	    }
	};
	
	
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
		Log.v(TAG, "service started");
		performOnBackgroundThread(downloadRunnable);
		
		appDBAdapter = ApplicationDBAdapter.getInstance(this);
		Log.v(TAG, "here"+appDBAdapter);
		appDBAdapter.open();
		appDBAdapter.deleteAllApps();
		
		userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(this);
		userPrefDBAdapter.open();
		userPrefDBAdapter.deleteAll();
		
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
		appDBAdapter.close();
		userPrefDBAdapter.close();
		Log.v(TAG, "unregister service");
		unregisterReceiver(mTriggerReceiver);
	}

	
	public boolean isContained(String name, String[] array) {
		for(int j = 0; j < array.length; j++) {
			if(array[j].equals(name)) {
				return true;
			}
		}
		return false;
	}
	
	
    public Date getCurTime() {
    	Date date = new Date();
    	return date;
    }

	
    public SocketManager getSocketManager() {
    	
    	return socketManager;
    }
    
    
	public static UserPreferenceDBAdapter getUserPreferenceDBAdapter() {

		return userPrefDBAdapter;
	}

	
	public static ApplicationDBAdapter getApplicationDBAdapter() {
		
		return appDBAdapter;
	}
	
}