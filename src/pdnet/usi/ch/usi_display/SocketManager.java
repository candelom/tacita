package pdnet.usi.ch.usi_display;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.NotYetConnectedException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.java_websocket.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pdnet.usi.ch.usi_display.AllAppsFragmentTab.AllAppsItemAdapter;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SocketManager {
	
	Context mContext;
	private static final String TAG = "SocketManager";
	//stores the active application sockets
	HashMap<String, WebSocketClient> activeSockets;
	private HashMap<String, String[]> displays;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private ApplicationDBAdapter appDBAdapter;
	private boolean influenced = false;
	
	//socket responsible for communicating with display controller
	private WebSocketClient displaySocket;
	
//	static String PDNET_HOST = "http://10.9.6.142:8888";
	static String PDNET_HOST = "http://pdnet.inf.unisi.ch:9000";
//	public static final String PDNET_HOST = "http://10.61.99.5:8888";
//	public static final String PDNET_HOST = "http://192.168.1.37:8888";
	private ArrayList<String> locations = new ArrayList<String>();
	private HashMap<String, JSONArray> itemsMap = new HashMap<String, JSONArray>();
	private static SocketManager instance = null;
	private String requestID = "";
	
	private static String PDNET_SOCKET_HOST = "ws://pdnet.inf.unisi.ch:9000";
//	private static String PDNET_SOCKET_HOST = "ws://172.17.253.201:9000";

	static String DISPLAYS_URL = "/displays.xml";
	private static String APPS_URL = "/assets/applications/list.xml";
	
	
	 public static SocketManager getInstance(Context context) {
	    	if(null == instance) {
	            instance = new SocketManager(context);
	        }
	        return instance;
	}
	
	
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
        	String socketAddress = readXMLFile(PDNET_HOST+"/assets/applications/list.xml", appNamespace);
        	return socketAddress;
        }
        

        @Override
        protected void onPostExecute(final String result) {
	        Log.v(TAG,  "/*** LEFT DATA ***/\n"+
	        			"appName : "+appNamespace+"\n"+
	        		    "socket : "+result+"\n");
	        Set<String> appsSockets = activeSockets.keySet();
	        if(!appsSockets.contains(appNamespace)) {
	        	createAppSocketWithAddress(appNamespace, "1", result);
	        } else {
				WebSocketClient appSocket = activeSockets.get(appNamespace);
				sendGetItemsRequest(appSocket, "1");
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
	
	public SocketManager(Context c) {
		mContext = c;
		this.activeSockets = new HashMap<String, WebSocketClient>();
		this.displays = new HashMap<String, String[]>();
		this.userPrefDBAdapter = UserPreferenceDBAdapter.getInstance(mContext);
		this.appDBAdapter = ApplicationDBAdapter.getInstance(mContext);
	}
	
	
	public HashMap<String, WebSocketClient> getActiveSockets() {
		
		return activeSockets;
	}
	
	
	public HashMap<String, String[]> getDisplays() {
		return displays;
	}
	
	public String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		Node nValue = nlList.item(0);
		return nValue.getNodeValue();
	}


	/**
	 * Reads XML file containing display information
	 * @param path
	 */
	public void readXMLFile(String path) {

		Log.v("LocationService", "reading file");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			Log.v("LocationService", "before");
			Document doc = db.parse(new URL(path).openStream());
			Log.v("LocationService", "after");
			Element root = doc.getDocumentElement();

			//get the list of displays
			NodeList pd_list = root.getElementsByTagName("display");
			if(pd_list != null && pd_list.getLength() > 0) {
				for(int i = 0 ; i < pd_list.getLength();i++) {

					//get the employee element
					Element cur_display = (Element)pd_list.item(i);
					Log.v("cur_display", cur_display.toString());
					String id = getTagValue("id", cur_display);
					String lat = getTagValue("latitude", cur_display);
					String lng = getTagValue("longitude", cur_display);
					Log.v("lat", lat);
					Log.v("lng", lng);

					String[] pd_info = new String[]{lat, lng};

					HashMap<String, String[]> pub_displays = getDisplays();
					pub_displays.put(id, pd_info);

				}
			}

		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	
	/**
	 * Checks if the position of the user is in proximity of a display.
	 * If so it return the displayID.
	 * @param user_location
	 * @return
	 */
	public String checkNearDisplays(Location user_location) {

		printProximityCheckMessage();
		HashMap<String, String[]> pub_displays = getDisplays();
		Set<String> keys = pub_displays.keySet();

		for(String id: keys) {

			//display position
			String[] cur_info = pub_displays.get(id); 
			double lat = new Double(cur_info[0]);
			double lng = new Double(cur_info[1]);

			double user_lat = user_location.getLatitude();
			double user_lng = user_location.getLongitude();

			Log.v("user_lat", ""+user_lat);
			Log.v("user_lng", ""+user_lng);

			Location pd_location = new Location(LocationManager.NETWORK_PROVIDER);
			pd_location.setLatitude(lat);
			pd_location.setLongitude(lng);
			float distance = user_location.distanceTo(pd_location);

			printDistance(distance);
			//if the display is in a range of 50 meters
			if(distance < 1000) {
				return id;
			}
		}
		return null;
	}
	

	public void printDistance(float distance) {
		System.out.println("/*** DISTANCE  =  " + distance + "***/");
	}

	public void printProximityCheckMessage() {
		System.out.println("/*** CHECKING NEARBY DISPLAYS ***/");
	}


	
	/**
	 * Triggers all active preferences and checks whether 
	 * the corresponding app has already an active socket. If not it call createAppSocket method.
	 * Else it calls sendPreferences method
	 * @param displayID
	 */
	public void triggerPreferences(String displayID) {

		
		printTriggerPreferenceMessage(displayID);
		
		List<Application> apps = appDBAdapter.getActiveApplications();
		if(apps.size() == 0) {
			System.out.println("/*** NO APPS ACTIVE ***/");
		} else {
			
			System.out.println("/*** "+apps.size()+" APPS ARE ACTIVE ***/");
			for (int i = 0; i < apps.size(); i++) {
				String appNamespace = apps.get(i).getNamespace();
				Log.v(TAG, "COAO => "+appNamespace);
				UserPreference curPref = userPrefDBAdapter.getPreference(appNamespace);
				Log.v(TAG, "ASDFASD");
				Log.v(TAG, "curPref => "+curPref);
				String appName = curPref.getAppName();
				String prefValue = curPref.getPrefValue();
				Set<String> appsSockets = activeSockets.keySet();
			
				//if the socket is not already stored create a new one
				
				Log.v(TAG, "/**** SEND PREFERENCES ****/"+
							"\n" + prefValue);
				if(!appsSockets.contains(appName)) {
					createAppSocket(appName, prefValue, displayID);
					Log.v(TAG, "CREATE APP SOCKET");
					Log.v(TAG, "/**** ACTUALLY SEND PREFERENCES ****/"+
							"\n" + "prefValue : "+ prefValue+"\n\n");
				} 
				else {
					Log.v(TAG, "/*** SOCKET ALREADY EXISTS ***/");
					WebSocketClient appSocket = activeSockets.get(appName);
					Log.v(TAG, "appSocket => " + appSocket.getConnection());
					Log.v(TAG, "appSocket2 => " + appSocket.getConnection().getRemoteSocketAddress());

					//if the socket is not connected to the server
					if(appSocket.getConnection().getRemoteSocketAddress() != null) {
						Log.v(TAG, "appSocket3 => " + appSocket.getConnection().getRemoteSocketAddress().getAddress());
						Log.v(TAG, "appSocket4 => " + appSocket.getConnection().getRemoteSocketAddress().getAddress().getHostAddress());
						Log.v(TAG,  "appName = "+appName+"\n"+
									"socket = "+appSocket.getConnection().getRemoteSocketAddress().getAddress().getHostAddress() +"\n" +
									"prefValue = "+prefValue+"\n");
						
						printSendMessage(appName, appSocket.getConnection().getRemoteSocketAddress().getAddress().getHostAddress(), prefValue);
						printInfluencedMessage(influenced);
						if(!influenced) {
							
							if(appName.equals("weather")) {
								Log.v(TAG, "/*** SOCKET ALREADY EXISTS ***/");
								Log.v(TAG, "/**** ACTUALLY SEND PREFERENCES ****/"+
										"\n" + "prefValue : "+prefValue+"\n\n");
								
							}else if(appName.equals("newsfeed")) {
								
								Log.v(TAG, "/*** SOCKET ALREADY EXISTS ***/");
								Log.v(TAG, "/**** ACTUALLY SEND PREFERENCES ****/"+
										"\n" + "prefValue : "+ deserializeObject(prefValue)+"\n\n");
							}
							sendPreference(appSocket, prefValue, displayID, appName);
						}
					} else {
						Log.v(TAG, "/*** SOCKET OF "+appName+" NOT CONNECTED TO SERVER");
					}
				}
			}
		}
	}

	
	
	

	public void printInfluencedMessage(boolean influenced) {
		if(influenced) {
			System.out.println("/*** ALREADY INFLUENCED  ***/\n\n");
		} else {
			System.out.println("/*** STILL TO INFLUENCE ***/\n\n");
		}
	}


	public void printTriggerPreferenceMessage(String displayID) {
		System.out.println("/*** TRIGGER PREFERENCES ***/\n\n");
	}


	public void sendPreference(WebSocketClient socket, String prefValue, String displayID, String appName) {
		
		Log.v(TAG, "APP NAME => "+appName+" AND PREF VALUE => "+prefValue.length());
	
		//check if a preference exists
		if(prefValue.length() > 0) {
			if(appName.equals("newsfeed")) {
			
				try {
					JSONObject newsMsg = new JSONObject();
					HashMap<String, String> prefMap = (HashMap<String, String>) deserializeObject(prefValue);
					
					Log.v(TAG, "PREPARING JSON topic");
					JSONObject topicsObj = new JSONObject();
					
					for(String topic : prefMap.keySet()) {
						boolean bool = new Boolean(prefMap.get(topic));
						topicsObj.put(topic, bool);
					}
						
					Log.v(TAG, "TOPIC OBJ IS => "+topicsObj.toString());
					newsMsg.put("kind", "mobileRequest");
					newsMsg.put("action", "customize");
					newsMsg.put("preference", topicsObj);
					newsMsg.put("displayID", displayID);
					newsMsg.put("username", "mattia");
					Log.v(TAG, "news send request => "+newsMsg.toString());
					try {
						socket.send(newsMsg.toString());
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					influenced = true;
						
					}  catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
	
					} catch (NotYetConnectedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				
			} else if(appName.equals("weather")){
				ArrayList<String> f_values = new ArrayList<String>();
				String[] inner_values = prefValue.split("::");
				for(int j =0; j < inner_values.length; j++) {
					f_values.add(inner_values[j].split(",")[0]);
				}
				try {
					JSONObject weatherMsg = new JSONObject();
					weatherMsg.put("kind", "mobileRequest");
					weatherMsg.put("action", "customize");
					weatherMsg.put("preference", f_values.get(0));
					weatherMsg.put("displayID", displayID);
					weatherMsg.put("username", "mattia");
					Log.v(TAG, "weather msg send => "+weatherMsg.toString());
					socket.send(weatherMsg.toString());
					influenced = true;

				}		
				catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
				}
	
				catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
	
			} catch (NotYetConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		} else if(appName.equals("twitter") || appName.equals("instagram")) {
			try {
				JSONObject twitterMsg = new JSONObject();
				twitterMsg.put("kind", "mobileRequest");
				twitterMsg.put("action", "customize");
				twitterMsg.put("preference", prefValue);
				twitterMsg.put("displayID", displayID);
				twitterMsg.put("username", "mattia");
				Log.v(TAG, appName + " msg send => "+twitterMsg.toString());
				socket.send(twitterMsg.toString());
				influenced = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotYetConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	} else {
		
		Log.v(TAG, "/*** PREFERENCE FOR APP "+ appName +" DOES NOT EXIST ***/");
	}
}	


	/**
	 * Create WebSocketClient for the given application
	 * @param appName
	 * @param prefValue
	 */
	public void createAppSocket(final String appName, final String prefValue, final String displayID) {

		System.out.println("/*** CREATE SOCKET FOR "+appName+" ***/\n\n");
		try {

			final String socketAddress = PDNET_SOCKET_HOST+"/"+appName+"/socket";
			try {
				WebSocketClient app_socket = new WebSocketClient(new URI(socketAddress)) {
				
					@Override
					public void onClose(int arg0, String arg1, boolean arg2) {
						// TODO Auto-generated method stub
						Log.v("close", "close");
					}

					@Override
					public void onError(Exception arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onMessage(String msg) {
							// TODO Auto-generated method stub
							Log.v("SocketManager", "/*** MESSAGE RECEIVED FROM "+appName+" socket ***/"+msg+"\n");
							try {
								
								JSONObject json = new JSONObject(msg);
								Log.v(TAG, "JSON => "+json.toString());
								JSONArray items = json.getJSONArray("data");
								Log.v(TAG, "ITEMS => "+items);
								for(int j = 0 ; j < items.length(); j++) {
									JSONObject item;
									item = items.getJSONObject(j);
									Log.v(TAG, "/** item here => "+item);
									String title = item.getString("title");
									Log.v(TAG, "/*** TITLE => "+title+" ****/");
								}
									
								
								JSONObject newJSON = new JSONObject();
								newJSON.put("items", items);

//								Log.v(TAG, "/*** CLOSING SOCKET "+appName+" ***/");
//								close();

								Intent appsIntent = new Intent();
							    appsIntent.setClass(mContext, ItemsFragment.class);
							    appsIntent.putExtra("items", newJSON.toString());
							    appsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							    Log.v(TAG, "starting activity");
						    	mContext.startActivity(appsIntent);
							}
							 catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
					}

					@Override
					public void onOpen(ServerHandshake arg0) {
						// TODO Auto-generated method stub
						Log.v("connect", "connect");
//						printSendMessage(appName, socketAddress, prefValue);
						sendPreference(this, prefValue, displayID, appName);
					}
				};

			app_socket.connect();
			activeSockets.put(appName, app_socket);

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NotYetConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	
	
	public void createAppSocketWithAddress(final String appName, final String displayID, final String socketAddress) {
		
		
		try {
			WebSocketClient app_socket = new WebSocketClient(new URI(socketAddress)) {
			
				@Override
				public void onClose(int arg0, String arg1, boolean arg2) {
					// TODO Auto-generated method stub
					Log.v("close", "close");
				}

				
				@Override
				public void onError(Exception arg0) {
					// TODO Auto-generated method stub
				}
				

				@Override
				public void onMessage(String msg) {
					// TODO Auto-generated method stub
					// TODO Auto-generated method stub
					Log.v("SocketManager", "/*** MESSAGE RECEIVED FROM "+appName+" socket ***/"+msg+"\n");
					Log.v(TAG, "msg => "+msg);
					
					try {
						JSONObject json = new JSONObject(msg);
						Log.v(TAG, "JSON => "+json.toString());
						JSONArray items = json.getJSONArray("data");
						Log.v(TAG, "ITEMS => "+items);
						for(int j = 0 ; j < items.length(); j++) {
							JSONObject item;
							item = items.getJSONObject(j);
							String title = item.getString("title");
							Log.v(TAG, "/*** TITLE => "+title+" ****/");
						}
						
						
						JSONObject newJSON = new JSONObject();
						newJSON.put("items", items);
						
//						Log.v(TAG, "/*** CLOSING SOCKET "+appName+" ***/");
//						close();
						
		            	Log.v(TAG, "start activity");
				    	Intent appsIntent = new Intent();
					    appsIntent.setClass(mContext, ItemsFragment.class);
					    appsIntent.putExtra("items", newJSON.toString());
					    appsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					    Log.v(TAG, "starting activity");
				    	mContext.startActivity(appsIntent);
					}
					 catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				@Override
				public void onOpen(ServerHandshake arg0) {
					// TODO Auto-generated method stub
					Log.v("connect", "connect");
					sendGetItemsRequest(this, displayID);
				}
			};

		app_socket.connect();
		activeSockets.put(appName, app_socket);

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public void sendGetItemsRequest(WebSocketClient appSocket, String displayID) {
		
			Log.v(TAG, "SENDING GET ITEMS REQUEST");
			try {
				JSONObject getItemsObj = new JSONObject();
				getItemsObj.put("kind", "getItems");
				getItemsObj.put("displayID", displayID);
				getItemsObj.put("reqID", requestID);
				getItemsObj.put("username", "mattia");
				appSocket.send(getItemsObj.toString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotYetConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	}
	
	
	
	
	public void deactivateSocket(String appName) {
		
		System.out.println("/*** DEACTIVATE SOCKET "+appName+" ***/\n\n");
		HashMap<String, WebSocketClient> activeSockets =  getActiveSockets();
		if(activeSockets.keySet().contains(appName)) {
			activeSockets.remove(appName);
			System.out.println(activeSockets);
		}
		
	}
	
	
	public void createDisplaySocket() {
		
		System.out.println("/*** CREATE SOCKET FOR DISPLAY CONTROLLER ***/\n\n");
		try {

			final String socketAddress = PDNET_SOCKET_HOST+"/display/socket";
			try {
				WebSocketClient app_socket = new WebSocketClient(new URI(socketAddress)) {
				
					@Override
					public void onClose(int arg0, String arg1, boolean arg2) {
						// TODO Auto-generated method stub
						Log.v("close", "close");
					}

					@Override
					public void onError(Exception arg0) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onMessage(String msg) {
						// TODO Auto-generated method stub
						Log.v("SocketManager", "/*** MESSAGE FROM CONTROLLER : "+msg+"\n");
						
						try {
							JSONObject jsonObject = new JSONObject(msg);
							String left = jsonObject.getString("left");
							String right = jsonObject.getString("right");
							String displayID = jsonObject.getString("displayID");
							String reqID = jsonObject.getString("reqID");
							
							requestID = reqID;
//							DownloadXMLFile task = new DownloadXMLFile(mContext, right);
//							task.execute();
////							
//							DownloadXMLFile task2 = new DownloadXMLFile(mContext, "newsfeed");
//							task.execute();
							ArrayList<String> screenApps = new ArrayList<String>();
							screenApps.add(left);
							screenApps.add(right);
							
							JSONObject appsJSON = new JSONObject();
							appsJSON.put("apps", new JSONArray(screenApps));
							
							Log.v(TAG, "start applist activity");
							Intent appsIntent = new Intent();
						    appsIntent.setClass(mContext, AppList.class);
						    appsIntent.putExtra("json", appsJSON.toString());
						    appsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						    Log.v(TAG, "starting activity");
					    	mContext.startActivity(appsIntent);
							
						} catch (JSONException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

					}

					@Override
					public void onOpen(ServerHandshake arg0) {
						// TODO Auto-generated method stub
						Log.v("connect", "connect");
					}
				};

			app_socket.connect();
			displaySocket = app_socket;

			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NotYetConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	public JSONObject createTestJSONItem() {
		
		
		JSONObject jsonObj = new JSONObject();
		try {
			jsonObj.put("title", "Title");
			jsonObj.put("description", "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.");
			jsonObj.put("link", "http://www.astro.caltech.edu/palomar/images/images.jpg");
			jsonObj.put("img", "http://www.astro.caltech.edu/palomar/images/images.jpg");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return jsonObj;
	}
	
	
	
	
	
	public void sendGetRequest(String displayID){
		
		Log.v(TAG, "SENDING GET REQUEST");
		try {
			JSONObject getObj = new JSONObject();
			getObj.put("displayID", displayID);
			getObj.put("kind", "getRequest");
			getObj.put("username", "mattia");
			displaySocket.send(getObj.toString());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotYetConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	public void printPreferences(ArrayList<String> values) {
		
		for(int i = 0 ; i < values.size(); i++) {
			Log.v("pref_value", values.get(i));
		}
		
	}
	
	/**
	 * Prints information of preference message
	 * @param appName
	 * @param appSocket
	 * @param prefValue
	 */
	public void printSendMessage(String appName, String appSocket, String prefValue) {
		
		System.out.println("/*** SENDING PREFERENCE ***/\n"+
							"Name : "+appName+"\n"+
							"Socket : "+appSocket+"\n"+
							"Data : "+prefValue+"\n\n");
		
	}

	
	
	/**
	 * Prints message when user is in proximity of a display.
	 */
	public void printProximityMessage(String displayID) {
		
		System.out.println("/*** NEXT TO DISPLAY ***/\n"+
				"DisplayID : "+displayID+"\n\n");
		
	}
	
	
	public void printCoordinates(String lat, String lng, String accuracy) {
		
		System.out.println("/*** USER COORDINATES ***/\n"+
							"Lat : "+lat+"\n"+
							"Lng : "+lng+"\n"+
							"Accuracy : "+accuracy+"\n"+
							"\n\n");
		
	}
	
	
	
	public ArrayList<String> createArrayList(String[] array) {
		
		ArrayList<String> list = new ArrayList<String>();
		for(int j =0; j < array.length; j++) {
			list.add(array[j]);
		}
	
		return list;
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
