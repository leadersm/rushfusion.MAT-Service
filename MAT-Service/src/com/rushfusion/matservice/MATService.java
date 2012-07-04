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
import android.content.Intent;
import android.media.MediaPlayer;
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
	
	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		super.onStart(intent, startId);
		Log.d(TAG, "------------onStart----------");
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
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
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
					System.out.println("receive-->" + str);
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
											Intent i = new Intent(getApplicationContext(),PlayActivity.class);
											i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
											i.putExtra("url", url);
											startActivity(i);
										}else if(req.equals("pausereq")){
											Log.d(TAG, "response pausereq-->");
											
										}else if(req.equals("stopreq")){
											Log.d(TAG, "response stopreq-->");
											
										}else if(req.equals("")){
											
										}
										
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
