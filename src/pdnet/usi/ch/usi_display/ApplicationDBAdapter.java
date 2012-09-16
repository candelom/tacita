package pdnet.usi.ch.usi_display;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ApplicationDBAdapter extends AbstractDBAdapter {

	private static ApplicationDBAdapter instance = null;
    private static final String[] columns = new String[]{"id", "name", "namespace", "view", "socket_address", "description", "icon", "isActive"};
    private static final String DATABASE_TABLE = "apps";

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created.
     * 
     * @param ctx the Context within which to work
     */
    private ApplicationDBAdapter(Context ctx) {
    	super(ctx);
    }
 
    
    public static ApplicationDBAdapter getInstance(Context context) {
    	if(null == instance) {
            instance = new ApplicationDBAdapter(context);
        }
        return instance;
    }
    
    
    
    
    /**
	 * Creates a preference object.
	 * @param pref
	 * @return
	 */
	public Application createApp(String name, String namespace, String view, String socket_address, String description, String icon) {
		Log.v("DB Interface", "craeting app entry");
		
		ContentValues val = new ContentValues();
		val.put("name", name);
		val.put("namespace", namespace);
		val.put("view", view);
		val.put("socket_address", socket_address);
		val.put("description", description);
		val.put("icon", icon);
		val.put("isActive", 0);
		
		long insertId = mDb.insertWithOnConflict(DATABASE_TABLE, null, val, SQLiteDatabase.CONFLICT_REPLACE);
		
		Log.v(TAG, "inserting app with ID => "+ insertId);
		
		Cursor cursor = mDb.query(DATABASE_TABLE,
				columns, "id = " + insertId, null, null, null, null);
		
		cursor.moveToFirst();
		Application newApp = cursorToApp(cursor);
		cursor.close();
		return newApp;
	}
	
	
	/**
	 * Selects the preference given the appName (unique)
	 * @param appName
	 * @return
	 */
	public Application getApp(long id) {
		
		Application selApp = null;
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "id = '" + id+"'", null, null, null, null);
		Log.v("rows number", cursor.getCount()+"");
		if(cursor.getCount() > 0) {
			Log.v("already", "exists");
			cursor.moveToFirst();
			selApp = cursorToApp(cursor);
		}
		cursor.close();
		return selApp;
	}
	
	
	public Application getAppByName(String appName) {
		
		Application selApp = null;
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "name = '" + appName +"'", null, null, null, null);
		Log.v("rows number", cursor.getCount()+"");
		if(cursor.getCount() > 0) {
			Log.v("already", "exists");
			cursor.moveToFirst();
			selApp = cursorToApp(cursor);
		}
		cursor.close();
		return selApp;
		
	}
	


	
	
	/**
	 * Deletes the given preference object.
	 * @param pref
	 */
	public void deleteApp(String namespace) {
		mDb.delete(DATABASE_TABLE, "namespace = '" + namespace+"'", null);
//		LocationService.socketManager.deactivateSocket(namespace);
		BtService.socketManager.deactivateSocket(namespace);
		
	}
	

	
	/**
	 * Deletes all entries
	 */
	public void deleteAllApps() {
		
		mDb.delete("apps", null, null);
		
	}
	
	/**
	 * Lists all preference entries.
	 * @return
	 */
	public List<Application> getAllApps() {
		List<Application> apps = new ArrayList<Application>();

		Cursor cursor = mDb.query(DATABASE_TABLE,
				columns, null, null, null, null, null);
		
		Log.v(TAG, "NUMBER OF INSTALLED APPS => "+cursor.getColumnCount());
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Application app = cursorToApp(cursor);
			apps.add(app);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return apps;
	}
	
	
	
	private Application cursorToApp(Cursor cursor) {
		Application app = new Application();
		app.setId(cursor.getLong(0));
		app.setName(cursor.getString(1));
		app.setNamespace(cursor.getString(2));
		app.setView(cursor.getString(3));
		app.setSocketAddress(cursor.getString(4));
		app.setDescription(cursor.getString(5));
		app.setIcon(cursor.getString(6));
		app.setIsActive(cursor.getInt(7));
		return app;
	}
	

	public boolean checkAppExistence(String appName) {

		Log.v(TAG, "checking app existence");
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "namespace = '"+appName+"'", null, null, null, null);
		
		Log.v(TAG, cursor.getCount()+"");
		if(cursor.getCount() > 0) {
			
			return true;
		} 
		cursor.close();
		return false;
	}
	
	
	
	/**
	 * Update the value of isActive to 1.
	 * @param pref
	 */
	public void activateApplication(String namespace) {
		String strFilter = "namespace = '"+ namespace+"'";
		ContentValues args = new ContentValues();
		args.put("isActive", "1");
		mDb.update(DATABASE_TABLE, args, strFilter, null);
	}
	
	
	

	/**
	 * Update the value of isActive to 0.
	 * @param pref
	 */
	public void deactivateApplication(String namespace) {
		
		String strFilter = "namespace = '"+ namespace+"'";
		ContentValues args = new ContentValues();
		args.put("isActive", 0);
		mDb.update(DATABASE_TABLE, args, strFilter, null);
		
		//update Location service activeSockets
		Log.v("DB interface", "deactivating app");
//		LocationService.socketManager.deactivateSocket(namespace);
		BtService.socketManager.deactivateSocket(namespace);
	}
    
	

	/**
	 * Lists all active applications.
	 * @return
	 */
	public List<Application> getActiveApplications() {
		List<Application> apps = new ArrayList<Application>();
		
		Log.v(TAG, "apps => "+mDb);
		Cursor cursor = mDb.query(DATABASE_TABLE,
				columns, "isActive = 1", null, null, null, null);
		
		Log.v(TAG, "ACTIVE APPS => "+cursor.getCount());
		
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			Application app = cursorToApp(cursor);
			apps.add(app);
			cursor.moveToNext();
		}
		
		// Make sure to close the cursor
		cursor.close();
		return apps;
	}
	
	
	public boolean checkAppStatus(String namespace) {
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "namespace = '"+ namespace +"'", null, null, null, null);
		cursor.moveToFirst();
		Application selApp = cursorToApp(cursor);
		if(selApp.isAppActive() == 1){
			cursor.close();
			return true;
		}else  {
			cursor.close();
			return false;
		}
	}


	public Application getAppByNameSpace(String appNameSpace) {
		
		Application selApp = null;
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "namespace = '" + appNameSpace +"'", null, null, null, null);
		Log.v("rows number", cursor.getCount()+"");
		if(cursor.getCount() > 0) {
			Log.v("already", "exists");
			cursor.moveToFirst();
			selApp = cursorToApp(cursor);
		}
		cursor.close();
		return selApp;
		
	}
	
	
    
}