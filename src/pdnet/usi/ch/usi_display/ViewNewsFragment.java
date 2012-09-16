package pdnet.usi.ch.usi_display;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewNewsFragment extends Activity {
	
	public static final String TAG = "AllApps";
	public SocketManager socketManager;
	private ApplicationDBAdapter appDBAdapter;
	private UserPreferenceDBAdapter userPrefDBAdapter;
	private Context context;
	private boolean isUpdated = false;
//	public static final String PDNET_HOST = "http://pdnet.inf.unisi.ch:9000/assets/applications/list.xml";
	public static final String PDNET_HOST = "http://10.9.6.142:8888/list.xml";
//	public static final String PDNET_HOST = "http://10.61.99.5:8888/list.xml";
//	public static final String PDNET_HOST = "http://192.168.1.39:8888/list.xml";
	
	
	public ViewNewsFragment(ApplicationDBAdapter appDBAdapter, UserPreferenceDBAdapter userPrefDBAdapter, Context context) {
		this.appDBAdapter = appDBAdapter;
		this.userPrefDBAdapter = userPrefDBAdapter;
		this.context = context;
	}
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_news_layout);
		Log.v(TAG, "created News Item view");
	}
	
	
	
	@Override
	public void onStart() {
		// TODO Auto-generated method stub
		super.onStart();
		AppItem item = (AppItem) getIntent().getSerializableExtra("apartment");
		
		TextView title = (TextView) findViewById(R.id.itemViewTitle);
		title.setText(item.getTitle());
		
		TextView desc = (TextView) findViewById(R.id.itemViewDescription);
		desc.setText(item.getDescription());
	
	}
	

}
