package com.rushfusion.matservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		String Action = arg1.getAction();
		Log.d("MAT-Service", "------------Receiver--------ACTION--->"+Action);
//		NetworkInfo info =arg1.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
//		Log.i("MAT-Service",info.toString());
		Intent intent = new Intent(arg0,MATService.class);
		arg0.startService(intent);
	}

	
}
