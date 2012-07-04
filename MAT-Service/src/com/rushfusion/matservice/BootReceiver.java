package com.rushfusion.matservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.d("MAT-Service", "------------BootReceiver-----------");
		Intent intent = new Intent(arg0,MATService.class);
		arg0.startService(intent);
	}

	
}
