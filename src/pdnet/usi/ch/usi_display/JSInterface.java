package pdnet.usi.ch.usi_display;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.util.ByteArrayBuffer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

public class JSInterface {
	
	Context mContext;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private ApplicationDBAdapter appDBAdapter;
	private final static String TAG = "JS Interface";
	public static String CUR_SERVICE = "bt";
	
	
    /** Instantiate the interface and set the context */
    JSInterface(Context c) {
        mContext = c;
        userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(mContext);
        appDBAdapter = ApplicationDBAdapter.getInstance(mContext);
    }
    

   
	   /**
	     * Update pref entry.
	     * @param appName
	     * @param values
	     */
	    public void updatePreferenceWeatherEntry(String appName, String[] values) {
	    	
//				JSONArray real_values = new JSONArray(json);
				Log.v(TAG, "locations => "+values);
		    	String prefValue = createPrefValue(values);
		    	Log.v("DB Interface", prefValue);
	//	    	Log.v("DB Interface", "updating entry "+appName);
		    	userPrefDBAdapter.updatePreference(appName, prefValue);
	    	
	    }
	    
	    
	    
	    
	    public void updatePreferenceNewsEntry(String appName, String json) {
	    	
	    	String json_true = new String(json);
	    	Log.v(TAG, "JSON RECEIVED => " + json_true);
	    	JSONObject real_values;
			try {
				real_values = new JSONObject(json);
				HashMap<String, String> prefValue = createNewsfeedPrefValue(real_values);
				Log.v("DB Interface", "prefValue => "+prefValue.toString());
 				Log.v("DB Interface", "updating newsfeed entry "+appName);
				userPrefDBAdapter.updatePreference(appName, serializeObject(prefValue));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	
	    }
	    
	    
	    
	    public void showPreferences(String appName, String[] prefValues) {
	    	
	    	String prefValue = createPrefValue(prefValues);
	    	Log.v("DB Interface", "updating newsfeed entry "+appName);
	    	userPrefDBAdapter.updatePreference(appName, prefValue);
	    	Log.v("DB Interface", "triggering prefs of "+appName);
	    	
	    }
	    
	    
	    
	    
	    
	    public void showNewsfeedPreferences(String appName, JSONObject prefValues) {
	    	
	    	HashMap<String, String> prefValue = createNewsfeedPrefValue(prefValues);
	    	String serializedObj = serializeObject(prefValue);
	    	
	    	Log.v("DB Interface", "updating newsfeed entry "+appName);
	    	userPrefDBAdapter.updatePreference(appName, serializedObj);
	    	Log.v("DB Interface", "triggering prefs of "+appName);
	    	
	    }
	    
	    
	    public void updateTwitterEntry(String appName, String prefValue) {
	    	
	    	userPrefDBAdapter.updatePreference(appName, prefValue);
	    }
	    
	    
	    public void updateInstagramrEntry(String appName, String prefValue) {
	    	
	    	userPrefDBAdapter.updatePreference(appName, prefValue);
	    }
	
	    
	    
	    private HashMap<String, String> createNewsfeedPrefValue(JSONObject json) {

	    	HashMap<String, String> prefValue = new HashMap<String, String>();
	    	
	    	try {
				Iterator<String> topics = json.keys();
				while(topics.hasNext()) {
					String topic = (String)topics.next();
					Log.v(TAG, "cur topic => "+topic);
					Log.v(TAG, "CUR TOPIC => "+topic);
					String activeOrNot = new Boolean(json.getBoolean(topic)).toString();
					prefValue.put(topic.toLowerCase(), activeOrNot);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	Log.v(TAG, "prefValue is => "+prefValue);
	    	return prefValue;
	    }

	    
	    

		/**
	     * Format given values to a string
	     * @param values
	     * @return
	     */
	    public String createPrefValue(String[] values) {
	    	
	    	String prefValue = "";
	    	for(int i = 0; i < values.length; i++) {
	    		if(i == values.length-1) {
	    			prefValue += values[i];
	    		} 
	    		else {
	    			prefValue += values[i] + "::";
	    		}
	    	}
	    	return prefValue;
	    }
	    
	    
	    public String serializeObject(Object o) { 
	        ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
	     
	        try { 
	          ObjectOutput out = new ObjectOutputStream(bos); 
	          out.writeObject(o); 
	          out.close(); 
	     
	          // Get the bytes of the serialized object 
	          byte[] buf = bos.toByteArray(); 
	         
	          return new String(buf, Charset.forName("ISO-8859-1")); 
	        } catch(IOException ioe) { 
	          Log.e("serializeObject", "error", ioe); 
	     
	          return null; 
	        } 
	      } 
	    
	    public static String diff(String str1, String str2) {
    	    int index = str1.lastIndexOf(str2);
    	    if (index > -1) {
    	      return str1.substring(str2.length());
    	    }
    	    return str1;
    	  }

	  
	    
	    public String getSavedLocations() {
	    	
	    	Log.v(TAG, "js interface here");
	    	Log.v(TAG, userPrefDBAdapter+" this is user pref db adapter");
	    	UserPreference weatherPref = userPrefDBAdapter.getPreference("weather");
	    	Log.v(TAG, "NUM OF PREF 2 => "+userPrefDBAdapter.getPreference("weather").getPrefValue());
	    	return userPrefDBAdapter.getPreference("weather").getPrefValue();
	    }
	    
	    
	    public String getSavedTopics() {
	    	
	    	ArrayList<String> savedTopics = new ArrayList<String>();
	    	UserPreference newsPref = userPrefDBAdapter.getPreference("newsfeed");
	    	String topicsSaved = "";
	    	if(newsPref.getPrefValue().length() > 0) {
	    		Log.v(TAG, "NEWS FEED PREFVALUE " + newsPref.getPrefValue());
	    		HashMap<String, String> prefMap = (HashMap<String, String>) deserializeObject(newsPref.getPrefValue());
	    		Log.v(TAG, "PREPARING JSON topic");
	    		for(String topic : prefMap.keySet()) {
	    			boolean bool = new Boolean(prefMap.get(topic));
	    			if(bool) {
	    				topicsSaved += topic+"::";
	    			}
	    		}
	    	}
			return topicsSaved;
	    }
	    
	    
		  public static Object deserializeObject(String obj) { 
		        
			  	byte[] b = obj.getBytes(Charset.forName("ISO-8859-1"));

		    	try { 
		          ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(b)); 
		          Object object = in.readObject(); 
		          in.close(); 
		     
		          return object; 
		        } catch(ClassNotFoundException cnfe) { 
		          Log.e("deserializeObject", "class not found error", cnfe); 
		     
		          return null; 
		        } catch(IOException ioe) { 
		          Log.e("deserializeObject", "io error", ioe); 
		     
		          return null; 
		        } 
		      } 
	    
	    
}
