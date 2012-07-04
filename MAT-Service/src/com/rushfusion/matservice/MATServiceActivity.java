package com.rushfusion.matservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MATServiceActivity extends Activity {
    /** Called when the activity is first created. */
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Intent i = new Intent(this,MATService.class);
        startService(i);
    }
}