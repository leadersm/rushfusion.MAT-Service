package com.rushfusion.matservice;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.VideoView;

public class PlayActivity extends Activity {
	VideoView v;
	BroadcastReceiver br = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String cmd = intent.getStringExtra("cmd");
			System.out.println("onReceive cmd--->"+cmd);
			if(cmd.equals("pause")){
				v.pause();
			}else if(cmd.equals("stop")){
				v.stopPlayback();
				finish();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play);
		registerReceiver(br, new IntentFilter("com.rushfusion.matservice"));
		v = (VideoView) findViewById(R.id.videoView1);
        Intent i = getIntent();
        String cmd = i.getStringExtra("cmd");
        if(cmd.equals("play")){
        	String url = i.getStringExtra("url");
        	v.setVideoPath(url);
        	v.start();
        }
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(br);
	}
}
