package com.rushfusion.matservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Service_WebViewPlayer extends Activity {

	private WebView wv;
	private static final String ACTION = "com.rushfusion.matshow";
	
	BroadcastReceiver r = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String cmd = intent.getStringExtra("cmd");
			Log.d("MATService", "webplayer onReceive cmd-->"+cmd);
			if(cmd.equals("pause")){
				if(wv!=null)wv.pauseTimers();
			}else if(cmd.equals("resume")){
				if(wv!=null)wv.resumeTimers();
				sendPlayingMessage(true);
			}else if(cmd.equals("stop")){
				finish();
			}else if(cmd.equals("seek")){
				
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		registerReceiver(r, new IntentFilter("com.rushfusion.matservice"));
		setContentView(R.layout.webviewplayer);
		Intent i = getIntent();
		String url = i.getStringExtra("url");
		Log.d("MATService", "webview url--->"+url);
		wv = (WebView) findViewById(R.id.webView1);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setPluginsEnabled(true);
		wv.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		wv.loadUrl(url);
		sendPlayingMessage(true);
		wv.setWebViewClient(new WebViewClient(){

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
	}
	
	private void sendPlayingMessage(boolean isPlaying) {
		// TODO Auto-generated method stub
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", isPlaying);
		sendBroadcast(i);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(wv!=null)
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
	        wv.goBack();
	        return true;
	    }
	    return super.onKeyDown(keyCode, event);
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		if(wv!=null)wv.destroy();
		super.onPause();
		
	}
	
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		try {
			unregisterReceiver(r);
			sendPlayingMessage(false);
			if(wv!=null){
				wv.pauseTimers();
				wv.stopLoading();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		super.onStop();
	}
	
}
