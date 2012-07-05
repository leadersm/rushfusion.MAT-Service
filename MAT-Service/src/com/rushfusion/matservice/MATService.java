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
import java.util.HashSet;
import java.util.Set;

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

	public static String ACTION = "com.rushfusion.matservice";
	private static final int PORT = 6806;
	private static final String TAG = "MATService";
	private DatagramSocket s = null;
	private String mIp;
	private String preUrl = "";
	Set<String> ips;
	
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
			}
		}
	};
	
	public String getLocalIpAddress() {
//		try {
//			for (Enumeration<NetworkInterface> en = NetworkInterface
//					.getNetworkInterfaces(); en.hasMoreElements();) {
//				NetworkInterface intf = en.nextElement();
//				for (Enumeration<InetAddress> enumIpAddr = intf
//						.getInetAddresses(); enumIpAddr.hasMoreElements();) {
//					InetAddress inetAddress = enumIpAddr.nextElement();
//					if (!inetAddress.isLoopbackAddress()) {
//						return inetAddress.getHostAddress().toString();
//					}
//				}
//			}
//		} catch (SocketException ex) {
//			ex.printStackTrace();
//		}
//		return null;
		
		ips = new HashSet<String>();
		Enumeration en;
		try {
			en = NetworkInterface.getNetworkInterfaces();
			while(en.hasMoreElements()){
				NetworkInterface intf = (NetworkInterface)en.nextElement();
				Enumeration enadd = intf.getInetAddresses();
				while(enadd.hasMoreElements()){
					InetAddress add = (InetAddress)enadd.nextElement();
					ips.add(add.getHostAddress());
				}
			}
			for(String s : ips){
	        	if(s.indexOf(":")==-1 && !(s.equals("127.0.0.1"))){
	        		return s;
	        		}
	        	}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
		
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
										String IP = map.get("IP");
										if(req.equals("searchreq")){
											Log.d(TAG, "response searchreq-->");
											responseTo(IP,ConstructResponseData.SearchResponse(0, mIp));
										}else if(req.equals("playreq")){
											Log.d(TAG, "response playreq  url-->"+map.get("url"));
											String url = map.get("url");
											Log.d(TAG, "isPlaying---->"+isPlaying);
											if(isPlaying){
												if(preUrl.equals(url)){
													Intent i = new Intent("com.rushfusion.matservice");
													i.putExtra("cmd", "resume");
													sendBroadcast(i);
												}else{
													preUrl = url;
													Intent i = new Intent("com.rushfusion.matservice");
													i.putExtra("cmd", "reset");
													i.putExtra("url", url);
													sendBroadcast(i);
												}
											}else{
												doPlay(map, url);
											}
										}else if(req.equals("pausereq")){
											Log.d(TAG, "response pausereq-->");
											Intent i = new Intent("com.rushfusion.matservice");
											i.putExtra("cmd", "pause");
											sendBroadcast(i);
										}else if(req.equals("stopreq")){
											Log.d(TAG, "response stopreq-->");
											Intent i = new Intent("com.rushfusion.matservice");
											i.putExtra("cmd", "stop");
											sendBroadcast(i);
										}else if(req.equals("")){
											
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
