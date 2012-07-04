package com.rushfusion.matservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.VideoView;

public class PlayActivity extends Activity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play);
        Intent i = getIntent();
        String url = i.getStringExtra("url");
        VideoView v = (VideoView) findViewById(R.id.videoView1);
        v.setVideoPath(url);
        v.start();
		
	}
}
