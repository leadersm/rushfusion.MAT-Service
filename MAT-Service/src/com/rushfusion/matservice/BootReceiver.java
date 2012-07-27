package com.rushfusion.matservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.WifiManager;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		String Action = arg1.getAction();
		Log.d("MAT-Service", "------------Receiver--------ACTION--->"+Action);
		NetworkInfo info =arg1.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
		if(info!=null){
			Log.i("MAT-Service",info.getDetailedState().toString());
			if(info.getDetailedState()==DetailedState.CONNECTED){
				Log.i("MAT-Service", "------->start service<------");
				Intent i = new Intent(arg0,MATService.class);
				arg0.startService(i);
			}else{
				Log.i("MAT-Service", "------>stop service<-----");
				Intent intent = new Intent(Service_MediaPlayer.ACTION);
				intent.putExtra("cmd", "resetnetwork");
				arg0.sendBroadcast(intent);
			}
		}
	}

	
}
