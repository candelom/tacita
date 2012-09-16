package pdnet.usi.ch.usi_display;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import pdnet.usi.ch.usi_display.SettingsFragmentTab.OnServiceSelectedListener;



import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ViewAppFragment extends Fragment {

	public static final String TAG = "AllApps";
	private ApplicationDBAdapter appDBAdapter;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private Context context;
	private AllAppsItem appName;
//	public static String PDNET_HOST = "http://10.9.6.139:8888/plugins/";
	public static String PDNET_HOST = "http://pdnet.inf.unisi.ch/devel/mobile/plugins/";
//	public static String PDNET_HOST = "file:///android_asset/plugins/";
    OnServiceSelectedListener mListener;

	
	
	private  class DownloadPlugin extends AsyncTask<Void,Void, String>
    {
		Context context;
		String appView;
		
		public DownloadPlugin(Context context, String appView) {
			this.context = context;
			this.appView = appView;
		}
		
		
        @Override
        protected String doInBackground(Void... params) {
        	
        	Application cur_app = null;
        	File PATH = Environment.getExternalStorageDirectory();
        	try {
        		Log.v(TAG, "connecting to server to url => "+PDNET_HOST+appView);
        	    //set the download URL, a url that points to a file on the internet
        	    //this is the file to be downloaded
        	    URL url = new URL(PDNET_HOST+appView);
        	
        	    //create the new connection
        	    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        	
        	    //set up some things on the connection
        	    urlConnection.setRequestMethod("GET");
        	    urlConnection.setDoOutput(true);
        	
        	    //and connect!
        	    urlConnection.connect();
        	
        	    //create a new file, specifying the path, and the filename
        	    //which we want to save the file as.

        	    
        	    String pluginSubFolder = appView.split("/")[0];
        	    String pluginFile = appView.split("/")[1];
        	    
        	    
        	    cur_app = appDBAdapter.getAppByName(appName.getName());
        	    
        	    Log.v(TAG, "cur_app => "+cur_app.getName());
        	    File plugin_folder = createPluginFolder(PATH, cur_app.getNamespace());
        	   
        	    Log.v(TAG, pluginSubFolder);
        	    Log.v(TAG, pluginFile);
        	    
        	    
        	    File html_plugin_file = createFile(plugin_folder, "view.html");
        	    downloadFile(html_plugin_file, urlConnection);
        	
        	    Log.v(TAG, "downloaded file");
        	    	
        	//catch some possible errors...
        	} catch (MalformedURLException e) {
        	    e.printStackTrace();
        	} catch (IOException e) {
        	    e.printStackTrace();
        	}
        	
        	return cur_app.getNamespace();
       }
        

        @Override
        protected void onPostExecute(String result)
        {
        	Log.v(TAG, "on POst execute");
    	    goToMyAppsView(result);

        }
    }
	
	
	
//	public ViewAppFragment(ApplicationDBAdapter appDBAdapter, UserPreferenceDBAdapter userPrefDBAdapter, Context context, AllAppsItem appName) {
//		this.appDBAdapter = appDBAdapter;
//		this.userPrefDBAdapter = userPrefDBAdapter;
//		this.context = context;
//		this.appName = appName;
//	}
//	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v(TAG, "created View apps View => "+mListener);
		this.appDBAdapter = mListener.getAppDBAdapter();
		this.userPrefDBAdapter = mListener.getUserPrefDBAdapter();
		this.context = mListener.getContext();
		this.appName = mListener.getCurAppName();
		
		
		View view = inflater.inflate(R.layout.view_app_layout, container, false);
		final Application app = appDBAdapter.getAppByName(appName.getName());
		
		Log.v(TAG, "adding text view");
		TextView appTitle = (TextView) view.findViewById(R.id.appTitle);
		appTitle.setText(app.getName());
		
		TextView appDesc = (TextView) view.findViewById(R.id.appDesc);
		appDesc.setText(app.getDescription());
		
		final ImageView icon = (ImageView) view.findViewById(R.id.appViewIcon);
        int id = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + app.getNamespace()+"_icon", null, null);
        icon.setImageResource(id);
		
		
        final ImageView smallImage = (ImageView) view.findViewById(R.id.appSmallImage);
        Log.v(TAG, "setting small image => "+appName.getName());
        int smallId = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + app.getNamespace()+"_small", null, null);
        smallImage.setImageResource(smallId);
       
        final ImageView bigImage = (ImageView) view.findViewById(R.id.appBigImage);
        int bigId = getResources().getIdentifier("pdnet.usi.ch.usi_display:drawable/" + app.getNamespace()+"_big", null, null);
        bigImage.setImageResource(bigId);
        
		
		//set install button
		final Button button = (Button) view.findViewById(R.id.installButton);
		
		
		if(userPrefDBAdapter.getPreference(app.getNamespace()) == null) {
			
			button.setText("Install");
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// Perform action on click
					installApp(app.getView(), app.getNamespace());
				}
			});
			
		} else {
			
			button.setText("Uninstall");
			button.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					// Perform action on click
					uninstallApp(app.getView(), app.getNamespace());
				}
			});
		}
		
		return view;
	}
	
	
	
	public void installApp(String appView, String appName) {
		
		Log.v(TAG, "/*** install app "+ appName +" ***/\n\n");
		Log.v(TAG, "user pref adapter => "+ userPrefDBAdapter);
		userPrefDBAdapter.createPreference(appName, "");
		
		Log.v(TAG, "CREATED PREF => "+userPrefDBAdapter.getPreference(appName));
		//set active weather just for test
//		appDBAdapter.activateApplication("weather");
//		Log.v(TAG, "activated APP WEATHER");
		
		//CREATE PREF FOR WEATHER
		
		DownloadPlugin task = new DownloadPlugin(context, appView);
	    task.execute();
	    
	}
	
	
	
	public File createFile(File folder, String fileName) {
		
		 File file = new File(folder, fileName);
		 file.setExecutable(true);
		 file.setWritable(true);
		 file.setReadable(true);
		 try {
			 file.createNewFile();
		 } catch (IOException e) {
			 e.printStackTrace();
		 }
		 Log.v(TAG, "CREATED FILE WITH PATH => "+file.getAbsolutePath());
		
		 return file;
		
		
	}
	
	
	
	public File createFolder(String fullPath) {
		
		  //create imgs folder
	    File folder = new File(fullPath); 
	    if (!folder.exists()) {
	    	folder.mkdirs();
	    	folder.setWritable(true);
	    	folder.setExecutable(true);
	    	folder.setReadable(true);
	    	Log.v(TAG, "CREATED FOLDER WITH PATH => " + folder.getAbsolutePath());
	    }
		
	    return folder;
		
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
	
	public void printFiles(Collection collection) {
		
		Log.v(TAG, "PRINT FILES");
		Iterator iterator = collection.iterator();
		// while loop
		while (iterator.hasNext()) {
			System.out.println("value= " + iterator.next());
		}
		
	}
	
	
	
	public void goToMyAppsView(String appName) {
		
		Log.v(TAG, "GO TO MY APPS VIEW => "+appName);
		//change to VIEW FRAGMENT
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        
        MyAppsFragmentTab maf = new MyAppsFragmentTab();
        fragmentTransaction.remove(this);
        fragmentTransaction.add(R.id.fragment_place, maf);
        fragmentTransaction.commit();
        
        getActivity().getActionBar().setSelectedNavigationItem(1);
	}

	
	
	
	public void downloadFile(File file, HttpURLConnection urlConnection) {
		
		Log.v(TAG, "downloading file plugin");
		
		//this will be used to write the downloaded data into the file we created
	    FileOutputStream fileOutput;
		try {
			fileOutput = new FileOutputStream(file);
	    
			Log.v(TAG, "creating inputstream with => "+urlConnection);
	    //this will be used in reading the data from the internet
	    InputStream inputStream = urlConnection.getInputStream();
	    Log.v(TAG, "created inputstream");
	    
	    
	    //this is the total size of the file
	    int totalSize = urlConnection.getContentLength();
	    Log.v("Download", totalSize+"");
	    //variable to store total downloaded bytes
	
	    Log.v(TAG, "ciao");
	    //create a buffer...
	    byte[] buffer = new byte[1024];
	    int bufferLength = 0; //used to store a temporary size of the buffer
	
	    //now, read through the input buffer and write the contents to the file
			while((bufferLength = inputStream.read(buffer)) > 0) {
			        //add the data in the buffer to the file in the file output stream (the file on the sd card
			        fileOutput.write(buffer, 0, bufferLength);
			        Log.v(TAG, "adding");
			}
			fileOutput.close();
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			Log.v(TAG, "exception");
			e.printStackTrace();
		
		}
		Log.v(TAG, "outputstream created");
		
	}
	
	
	public File createPluginFolder(File root, String appNameSpace) {
		
		//create imgs folder
	    File folder = new File(root+"/plugins/"+appNameSpace); 
	    if (!folder.exists()) {
	    	folder.mkdirs();
	    	folder.setWritable(true);
	    	folder.setExecutable(true);
	    	folder.setReadable(true);
	    	Log.v(TAG, "CREATED FOLDER WITH PATH => " + folder.getAbsolutePath());
	    }
		
	    return folder;
	}
	
	
	public void uninstallApp(String appView, String appName) {
		
		UserPreference pref = userPrefDBAdapter.getPreference(appName);
		userPrefDBAdapter.deletePreference(pref);
		goToMyAppsView(appName);
	}
	
}
