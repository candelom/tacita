package pdnet.usi.ch.usi_display;

import android.os.Bundle;

import com.google.android.maps.MapActivity;


import com.google.android.maps.MapView;

public class MyMapActivity extends MapActivity {

	 @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.map_layout);
    }
	
	
	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}
