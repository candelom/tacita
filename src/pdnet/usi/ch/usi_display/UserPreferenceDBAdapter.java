package pdnet.usi.ch.usi_display;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class UserPreferenceDBAdapter extends AbstractDBAdapter {

	private static UserPreferenceDBAdapter instance = null;
    private static final String[] columns = new String[]{"id", "appName", "pref_values"};
    private static final String DATABASE_TABLE = "preferences";
    
    
    
    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    private UserPreferenceDBAdapter(Context ctx) {
    	super(ctx);
    }
    
    
    public static UserPreferenceDBAdapter getInstance(Context context) {
    	if(null == instance) {
            instance = new UserPreferenceDBAdapter(context);
        }
        return instance;
    }
    
    	
    
    /**
	 * Creates a preference object.
	 * @param pref
	 * @return
	 */
	public UserPreference createPreference(String appName, String prefValue) {
		
		ContentValues val = new ContentValues();
		val.put("pref_values", prefValue);
		val.put("appName", appName);
		long insertId = mDb.insert(DATABASE_TABLE, null, val);
		
		Cursor cursor = mDb.query(DATABASE_TABLE,
				columns, "id = " + insertId, null, null, null, null);
		
		cursor.moveToFirst();
		UserPreference newPreference = cursorToPref(cursor);
		cursor.close();
		return newPreference;
	}
	
	
	
	
	/**
	 * Selects the preference given the appName (unique)
	 * @param appName
	 * @return
	 */
	public UserPreference getPreference(String appName) {
		
		UserPreference selPref = null;
		Cursor cursor = mDb.query(DATABASE_TABLE, columns, "appName = '" + appName+"'", null, null, null, null);
		Log.v("DB Interface", "row numbers => "+cursor.getCount());
		if(cursor.getCount() > 0) {
			Log.v("DB Interface", "entry already exists");
			cursor.moveToFirst();
			selPref = cursorToPref(cursor);
		}
		cursor.close();
		Log.v(TAG, "returning "+selPref);
		return selPref;
	}
	
	

	
	/**
	 * Update the given preference object.
	 * @param pref
	 */
	public void updatePreference(String appName, String prefValue) {
		
		String strFilter = "appName = '"+ appName+"'";
		ContentValues args = new ContentValues();
		args.put("pref_values", prefValue);
		
		Log.v(TAG, "update db with entry value => " + prefValue);
		mDb.update(DATABASE_TABLE, args, strFilter, null);
	}
	
	
	
	
	/**
	 * Deletes the given preference object.
	 * @param pref
	 */
	public void deletePreference(UserPreference pref) {
		long id = pref.getId();
		mDb.delete(DATABASE_TABLE, "id = " + id, null);
	}
	
	

	
	/**
	 * Deletes all entries
	 */
	public void deleteAll() {
		mDb.delete(DATABASE_TABLE, null, null);
	}

	
	
	
	/**
	 * Lists all preference entries.
	 * @return
	 */
	public List<UserPreference> getAllPreferences() {
		List<UserPreference> prefs = new ArrayList<UserPreference>();

		Cursor cursor = mDb.query(DATABASE_TABLE,
				columns, null, null, null, null, null);
		if(cursor.getColumnCount() > 0) {
			
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				UserPreference pref = cursorToPref(cursor);
				prefs.add(pref);
				cursor.moveToNext();
			}
		}
		// Make sure to close the cursor
		cursor.close();
		return prefs;
	}
	
	
	
	
	private UserPreference cursorToPref(Cursor cursor) {
		UserPreference pref = new UserPreference();
		pref.setId(cursor.getLong(0));
		pref.setAppName(cursor.getString(1));
		pref.setPrefValue(cursor.getString(2));
		return pref;
	}
	
	

}