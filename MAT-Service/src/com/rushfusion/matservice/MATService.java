package com.rushfusion.matservice;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;

import com.rushfusion.matservice.util.ConstructResponseData;
import com.rushfusion.matservice.util.MscpDataParser;

public class MATService extends Service {

	public static final String ACTION = "com.rushfusion.matservice";
	
	private static final int PORT = 6806;
	private static final String TAG = "MATService";
	private DatagramSocket s = null;
	private String mIp;
	private String preUrl = "";
	private String IP;
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "------------onCreate----------");
		try {
			s = new DatagramSocket(PORT);
			mIp = getLocalIpAddress();
			Thread mReceiveThread = new Thread(receiveRunnable);
			mReceiveThread.start();
			registerReceiver(r, new IntentFilter("com.rushfusion.matshow"));
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
	}
	
	private boolean isPlaying = false;
	
	BroadcastReceiver r = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String cmd = intent.getStringExtra("cmd");
			Log.d(TAG, "back to service cmd--->"+cmd);
			if(cmd.equals("release")){
				preUrl = "";
				isPlaying = false;
			}else if(cmd.equals("state")){
				isPlaying = intent.getBooleanExtra("isPlaying", false);
			}else if(cmd.equals("state-duration")){
				int pos = intent.getIntExtra("pos", 0);
				responseTo(IP,ConstructResponseData.SeekResp(mIp,pos));
			}
		}
	};
	
	public String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface
					.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf
						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	
	Runnable receiveRunnable = new Runnable() {
		public void run() {
			Log.d(TAG,"the MATService receive-thread is running");
			startReceive();
		}
	};
	
	protected void startReceive() {
		try {
			byte[] buffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			while (true) {
				s.receive(packet);
				if (packet.getLength() > 0) {
					String str = new String(buffer, 0, packet.getLength());
					MscpDataParser.getInstance().init(this);
					MscpDataParser.getInstance().parse(packet,
							new MscpDataParser.CallBack() {
								@Override
								public void onParseCompleted(
										HashMap<String, String> map) {
									if (map != null) {
										String req = map.get("cmd");
										IP = map.get("IP");
										if(req.equals("searchreq")){
											Log.d(TAG, "receive searchreq-->");
											responseTo(IP,ConstructResponseData.SearchResponse(0, mIp));
										}else if(req.equals("playreq")){
											Log.d(TAG, "receive playreq url-->"+map.get("url"));
											String url = map.get("url");
											Log.d(TAG, "isPlaying---->"+isPlaying);
											if(isPlaying){
												if(preUrl.equals(url)){
													Intent i = new Intent(ACTION);
													i.putExtra("cmd", "resume");
													sendBroadcast(i);
												}else{
													preUrl = url;
													Intent i = new Intent(ACTION);
													i.putExtra("cmd", "reset");
													i.putExtra("url", url);
													sendBroadcast(i);
												}
											}else{
												doPlay(map, url);
											}
										}else if(req.equals("pausereq")){
											Log.d(TAG, "receive pausereq-->");
											Intent i = new Intent(ACTION);
											i.putExtra("cmd", "pause");
											sendBroadcast(i);
										}else if(req.equals("stopreq")){
											Log.d(TAG, "receive stopreq-->");
											Intent i = new Intent(ACTION);
											i.putExtra("cmd", "stop");
											sendBroadcast(i);
										}else if(req.equals("seekreq")){
											Log.d(TAG, "receive seekreq-->"+map.get("pos"));
											Intent i = new Intent(ACTION);
											i.putExtra("cmd", "seek");
											i.putExtra("pos", Integer.parseInt(map.get("pos")));
											sendBroadcast(i);
										}
										
									}
								}
								private void doPlay(HashMap<String, String> map, String url) {
									preUrl = url;
									if(url.indexOf("html")==(url.length()-4)){
										Intent it = new Intent(Intent.ACTION_VIEW , Uri.parse(url));
										startActivity(it);
									}else {
										Intent i = new Intent(getApplicationContext(),MediaPlayerShow.class);
										i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
										i.putExtra("cmd", "play");
										i.putExtra("url", url);
										i.putExtra("title", map.get("title"));
										startActivity(i);
									}
								}
								@Override
								public void onError(int code, String desc) {

								}
							});
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void responseTo(String destip,byte[] data) {
		Log.d(TAG, "responseTo-->"+destip);
		InetAddress IP;
		try {
			IP = InetAddress.getByName(destip);
			DatagramPacket p = new DatagramPacket(data, data.length, IP, PORT);
			s.send(p);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
}
