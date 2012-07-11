package com.rushfusion.matservice;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

public class Service_MediaPlayer extends Activity implements
		OnBufferingUpdateListener, OnVideoSizeChangedListener,
		OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPreparedListener, OnSeekCompleteListener, Callback,
		MediaPlayerControl {
	public static final String ACTION = "com.rushfusion.matshow";
	
	
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	MediaPlayer mediaPlayer;
	MediaController controller;
	Display currentDisplay;
	int videoWidth = 0;
	int videoHeight = 0;
	int contiuePosition = 0;
	String title;
	int saveTime = 0;
	
	Handler h = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 0:
				Intent intent = (Intent) msg.obj;
				String cmd = intent.getStringExtra("cmd");
				Log.d(MATService.TAG,"onReceive cmd--->" + cmd);
				if (cmd.equals("pause")) {
					if(controller!=null)controller.show();
					pause();
				} else if (cmd.equals("stop")) {
					finish();
				} else if(cmd.equals("resume")){
					if(controller!=null)controller.show();
					if(!mediaPlayer.isPlaying()){
						mediaPlayer.start();
					}else Log.d(MATService.TAG, "该视频已经在播放了。。。");
					Intent i = new Intent(ACTION);
					i.putExtra("cmd", "state");
					i.putExtra("isPlaying", true);
					sendBroadcast(i);
				}else if(cmd.equals("seek")){
					if(controller!=null)controller.show();
					int pos = intent.getIntExtra("pos", 0);
					try {
						if(mediaPlayer!=null&&isPrepared){
							mediaPlayer.seekTo(mediaPlayer.getDuration()*pos/100);
							Intent i = new Intent(ACTION);
							i.putExtra("cmd", "state-duration");
							i.putExtra("pos", mediaPlayer.getCurrentPosition());
							sendBroadcast(i);
						}
					} catch (Exception e) {
						// TODO: handle exception
						finish();
					}
				}
				
				break;

			default:
				break;
			}
			
		}
	};
	
	BroadcastReceiver br = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			Message msg = new Message();
			msg.what = 0;
			msg.obj = intent;
			h.sendMessageDelayed(msg, 100);
		}
	};

	boolean isPrepared = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playershow);
		surfaceView = (SurfaceView) findViewById(R.id.page_playershow_surfaceview);
		registeBR();

        Intent i = getIntent();
        String cmd = i.getStringExtra("cmd");
        
        initMediaPlayer();
		
		title = i.getStringExtra("title");
		String url = i.getStringExtra("url");
		try {
			if(cmd.equals("play")){
	        	mediaPlayer.setDataSource(url);
	        }
		} catch (Exception e) {
			System.out.println("mediaplayer 设置数据中出错，错误信息：" + e.toString());
		}
		currentDisplay = getWindowManager().getDefaultDisplay();
		controller = new MediaController(this, false);
	}

	
	private void registeBR() {
		registerReceiver(br, new IntentFilter("com.rushfusion.matservice"));
		br.setOrderedHint(true);
		Log.d("MATService", "onCreate registerReceiver");
	}

	private void initMediaPlayer() {
		showDialog(0);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mediaPlayer = new MediaPlayer();
		mediaPlayer.setOnCompletionListener(this);
		mediaPlayer.setOnErrorListener(this);
		mediaPlayer.setOnInfoListener(this);
		mediaPlayer.setOnPreparedListener(this);
		mediaPlayer.setOnSeekCompleteListener(this);
		mediaPlayer.setOnVideoSizeChangedListener(this);
		mediaPlayer.setOnBufferingUpdateListener(this);
	}

	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getCurrentPosition() {
		try {
			if (mediaPlayer != null) {
				if (mediaPlayer.isPlaying()) {
					saveTime = mediaPlayer.getCurrentPosition();
					return saveTime;
				} else {
					return saveTime;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	}

	@Override
	public int getDuration() {
		try {
			if (mediaPlayer != null) {
				if (mediaPlayer.isPlaying()) {
					return mediaPlayer.getDuration();
				} else {
					return 0;
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	}

	@Override
	public boolean isPlaying() {
		try {
			if (mediaPlayer != null) {
				return mediaPlayer.isPlaying();
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return false;
	}

	@Override
	public void pause() {
		try{
			if (mediaPlayer.isPlaying()) {
				mediaPlayer.pause();
			}
		}catch (Exception e) {
			// TODO: handle exception
			finish();
		}
	}

	
	@Override
	public void seekTo(int pos) {
		if (mediaPlayer != null) {
			// mediaPlayer.pause();
			showDialog(0);
			mediaPlayer.seekTo(pos);
		} else {
			Log.d(MATService.TAG,"mediaplayer.seekto 出错了！");
			finish();
		}

	}

	@Override
	public void start() {
		if (mediaPlayer != null) {
			mediaPlayer.start();
		} else {
			Log.d(MATService.TAG,"mediaplayer.start 出错了！");
			finish();
		}
	}

	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			mediaPlayer.setDisplay(holder);
			mediaPlayer.prepareAsync();
		} catch (IllegalStateException e) {
			Log.d(MATService.TAG,"surface准备中出错 ，错误信息 ：" + e.toString());
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case 0:
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setMessage("视频加载中，请稍后...");
			dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			return dialog;
		default:
			break;
		}
		return null;
	}
	

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int arg1, int arg2) {
		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();
		if (videoWidth > currentDisplay.getWidth()
				|| videoHeight > currentDisplay.getHeight()) {
			float heightRatio = (float) videoHeight
					/ (float) currentDisplay.getHeight();
			float widthRatio = (float) videoWidth
					/ (float) currentDisplay.getWidth();
			if (heightRatio > 1 || widthRatio > 1) {
				if (heightRatio > widthRatio) {
					videoHeight = (int) Math.ceil((float) videoHeight
							/ (float) heightRatio);
					videoWidth = (int) Math.ceil((float) videoWidth
							/ (float) heightRatio);
				} else {
					videoHeight = (int) Math.ceil((float) videoHeight
							/ (float) widthRatio);
					videoWidth = (int) Math.ceil((float) videoWidth
							/ (float) widthRatio);
				}
			}
			surfaceView.setLayoutParams(new LinearLayout.LayoutParams(
					videoWidth, videoHeight));
		} else {
			surfaceHolder.setFixedSize(videoWidth, videoHeight);
		}
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		if (mediaPlayer != null) {
			dismissDialog(0);
		} else {
			Log.d(MATService.TAG,"mediaplayer.onseekcomplete出错了");
			dismissDialog(0);
		}

	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		videoWidth = mp.getVideoWidth();
		videoHeight = mp.getVideoHeight();
		if (videoWidth > currentDisplay.getWidth()|| videoHeight > currentDisplay.getHeight()) {
			float heightRatio = (float) videoHeight/ (float) currentDisplay.getHeight();
			float widthRatio = (float) videoWidth/ (float) currentDisplay.getWidth();
			if (heightRatio > 1 || widthRatio > 1) {
				if (heightRatio > widthRatio) {
					videoHeight = (int) Math.ceil((float) videoHeight/ (float) heightRatio);
					videoWidth = (int) Math.ceil((float) videoWidth/ (float) heightRatio);
				} else {
					videoHeight = (int) Math.ceil((float) videoHeight/ (float) widthRatio);
					videoWidth = (int) Math.ceil((float) videoWidth/ (float) widthRatio);
				}
			}
			surfaceView.setLayoutParams(new LinearLayout.LayoutParams(videoWidth, videoHeight));
		} else {
			surfaceHolder.setFixedSize(videoWidth, videoHeight);
		}
		controller.setMediaPlayer(this);
		controller.setAnchorView(this.findViewById(R.id.page_playershow_mainview));
		controller.setEnabled(true);
		controller.show();
		dismissDialog(0);
		mediaPlayer.start();
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", true);
		sendBroadcast(i);
		isPrepared = true;
	}

	@Override
	protected void onPause() {
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", false);
		sendBroadcast(i);
		super.onPause();
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
			Log.d(MATService.TAG,"音视频交叉错误");
			break;
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			Log.d(MATService.TAG,"原资料更新");
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			Log.d(MATService.TAG,"该视频类型，无法定位");
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			Log.d(MATService.TAG,"音视频交叉错误");
			break;
		}
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Toast.makeText(this, "错误代码："+what , 500).show();
		Log.d(MATService.TAG,"onError error -->"+what);
		finish();
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		Toast.makeText(this, "播放完毕", 500).show();
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state-complete");
		sendBroadcast(i);
		finish();
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
			if (!controller.isShowing()) {
				controller.show();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (!controller.isShowing()) {
				controller.show();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			if (!controller.isShowing()) {
				controller.show();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (!controller.isShowing()) {
				controller.show();
				return true;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (!controller.isShowing()) {
				controller.show();
				return true;
			}
			break;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		try{
			unregisterReceiver(br);
			Log.d(MATService.TAG,"onStop  unregisterReceiver");
		}catch (Exception e) {
			// TODO: handle exception
			Log.d(MATService.TAG,e.getMessage());
		}
		super.onStop();
	}


	
	
}
