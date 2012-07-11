package com.rushfusion.matservice;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.widget.VideoView;

public class Service_VideoView extends Activity {
	VideoView v;
	BroadcastReceiver br = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String cmd = intent.getStringExtra("cmd");
			System.out.println("onReceive cmd--->" + cmd);
			if (cmd.equals("pause")) {
				v.pause();
			} else if (cmd.equals("stop")) {
				v.stopPlayback();
				Intent i = new Intent("com.rushfusion.matshow");
				i.putExtra("cmd","release");
				sendBroadcast(i);
				finish();
			} else if(cmd.equals("resume")){
				v.start();
				Intent i = new Intent("com.rushfusion.matshow");
				i.putExtra("cmd", "state");
				i.putExtra("isPlaying", true);
				sendBroadcast(i);
			} else if(cmd.equals("reset")){
				v.stopPlayback();
				String url = intent.getStringExtra("url");
				v.setVideoPath(url);
				showDialog(0);
				v.start();
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
		v.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				dismissDialog(0);
				Intent i = new Intent("com.rushfusion.matshow");
				i.putExtra("cmd", "state");
				i.putExtra("isPlaying", mp.isPlaying());
				sendBroadcast(i);
			}
		});
        Intent i = getIntent();
        String cmd = i.getStringExtra("cmd");
        if(cmd.equals("play")){
        	String url = i.getStringExtra("url");
        	v.setVideoPath(url);
        	showDialog(0);
        	v.start();
        }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		// TODO Auto-generated method stub
		switch (id) {
		case 0:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle("提示");
			dialog.setMessage("视频正在加载中...");
			return dialog;
		default:
			break;
		}
		return null;
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		unregisterReceiver(br);
	}
	
	
}
