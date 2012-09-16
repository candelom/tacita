package pdnet.usi.ch.usi_display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class SettingsFragmentTab extends Fragment {

	public static final String TAG = "Settings";
	private int cur_service;
	private Context context;
    OnServiceSelectedListener mListener;

	
//	public SettingsFragmentTab(int cur_service, Context context) {
//		
//		this.context = context;
//		this.cur_service = cur_service;
//	}
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		Log.v(TAG, "created Settings View");
		
		
		return inflater.inflate(R.layout.settings_layout, container, false);
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
	

	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		setCheckboxes();
		setInitialCheckBox(mListener.getCurService());
	}
	
	public void setCheckboxes() {
		
		Log.v(TAG, "set checkboxes");
		
		CheckBox nfcCheckbox = (CheckBox) getActivity().findViewById( R.id.checkboxNFC );
		nfcCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		    {
		        if (isChecked)
		        {
		            // perform logic
		        	Log.v(TAG, "NFC IS CHECKED");
		        	mListener.activateNFC();

//		        	mListener.stopCurService(mListener.getCurService());
//		        	mListener.startNewService(3);
//		        	stopCurService(getCurService());
//		        	startNewService(3);
		        } else {
		        	
		        	Log.v(TAG, "NFC IS UNCHECKED");
		        	mListener.disableNFC();
		        }
		    }
		});
		
		
		
		CheckBox btCheckbox = (CheckBox) getActivity().findViewById( R.id.checkboxBT);
		btCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
		    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		    {
		        if (isChecked)
		        {
		            // perform logic
		        	Log.v(TAG, "BT IS CHECKED");
		        	uncheckWifi();
		        	mListener.stopCurService(mListener.getCurService());
		        	mListener.startNewService(1);
		        	
		        } else {
		        	mListener.stopCurService(mListener.getCurService());
		        }

		    }
		});
		
		
		CheckBox wifiCheckbox = (CheckBox) getActivity().findViewById( R.id.checkboxWIFI );
		wifiCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
		       public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		    {
		        if (isChecked)
		        {
		            // perform logic
		        	Log.v(TAG, "WIFI IS CHECKED");
		        	uncheckBt();
		        	mListener.stopCurService(mListener.getCurService());
		        	mListener.startNewService(2);
		        	
		        }
		    }
		});
	}
	
	
	
	
	public void uncheckWifi() {
		Log.v(TAG, "uncheck wifi");
		CheckBox nfcCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxWIFI);
		nfcCheckbox.setChecked(false);
	}
	
	
	public void uncheckBt() {
		Log.v(TAG, "UNCHECK BT");
		CheckBox nfcCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxBT);
		nfcCheckbox.setChecked(false);
	}
	
	public void setInitialCheckBox(int cur_service) {
		
		Log.v(TAG, "SET INITIAL CHECKBOX => "+cur_service);
		Log.v(TAG, "NFC STATUS => "+mListener.getNFCAdapter().isEnabled());
		
		if(mListener.getNFCAdapter().isEnabled()) {
			CheckBox nfcCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxNFC);
			nfcCheckbox.setChecked(true);
		}
		
		
		if(cur_service == 1) {
			Log.v(TAG, "SETTING BT SERVICE");
			CheckBox btCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxBT);
			btCheckbox.setChecked(true);
			
		} else if(cur_service == 2) {
			CheckBox wifiCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxWIFI);
			wifiCheckbox.setChecked(true);
			
		} else if(cur_service == 3) {
			
			CheckBox nfcCheckbox = (CheckBox) getActivity().findViewById(R.id.checkboxNFC);
			nfcCheckbox.setChecked(true);
		}
	}
	
	
	
    public interface OnServiceSelectedListener {
        public void stopCurService(int cur_service);
        public void startNewService(int cur_service);
        public ApplicationDBAdapter getAppDBAdapter();
        public UserPreferenceDBAdapter getUserPrefDBAdapter();
        public Context getContext();
        public int getCurService();
		public void setCurAppName(AllAppsItem appName);
		public AllAppsItem getCurAppName();
		public void disableNFC();
		public void activateNFC();
		public NfcAdapter getNFCAdapter();
    }
	
}
