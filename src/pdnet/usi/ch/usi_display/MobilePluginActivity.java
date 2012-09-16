package pdnet.usi.ch.usi_display;

import pdnet.usi.ch.usi_display.*;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MobilePluginActivity extends Activity {

	private WebView webView;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mobile_plugin);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        String appNameSpace = (String) intent.getExtras().get("appNameSpace");
        
        webView = (WebView) findViewById(R.id.pluginWebView);
		webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new JSInterface(this), "JSInterface");
        
//        webView.loadUrl("file://"+Environment.getExternalStorageDirectory()+"/plugins/"+appNameSpace+"/view.html");
        if(appNameSpace.equals("weather")) {
        	webView.loadUrl("file:///android_asset/plugins/weather_view.html");
        } else if(appNameSpace.equals("newsfeed")) {
        	webView.loadUrl("file:///android_asset/plugins/newsfeed_view.html");
        } else if(appNameSpace.equals("twitter")) {
        	webView.loadUrl("file:///android_asset/plugins/twitter_view.html");
        }  else if(appNameSpace.equals("instagram")) {
        	webView.loadUrl("file:///android_asset/plugins/instagram_view.html");
        }
    }
	
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; go home
	            Intent intent = new Intent(this, MainActivity.class);
	            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	            intent.addCategory("back");
	            startActivity(intent);
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	
	 private class Callback extends WebViewClient{  //HERE IS THE MAIN CHANGE. 

	        @Override
	        public boolean shouldOverrideUrlLoading(WebView view, String url) {
	            return (false);
	        }

	    }
	
}
