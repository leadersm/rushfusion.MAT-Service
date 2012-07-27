package com.rushfusion.matservice;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.rushfusion.vhttpproxy.VHttpProxyDaemon;
import com.rushfusion.vhttpproxy.VHttpProxyDaemon.OnBufferingStatus;

public class Service_MediaPlayer extends Activity implements
		OnBufferingUpdateListener, OnVideoSizeChangedListener,
		OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPreparedListener, OnSeekCompleteListener, Callback{
	public static final String ACTION = "com.rushfusion.matshow";

	private SurfaceView surfaceView;
	private SurfaceHolder surfaceHolder;
	private MediaPlayer mediaPlayer;
	private Display currentDisplay;
	private int videoWidth = 0;
	private int videoHeight = 0;
	private String title;
	private int currentPosition = 0;
	
	boolean isPrepared = false;
	String IP;// who started this page;
	
	
	boolean useHttpProxy = true;
	// -------------------------------------------
	MATController controller;
	// -------------------------------------------
	VHttpProxyDaemon _httpProxy;
	int _port = 9001;

	boolean _isDebug = false;
	boolean _enableProxyAutoBuffering = false;
	boolean _enableProxyBuffering = true;
	boolean _setSource = false;

	boolean _mediaplayerIsPrepared;
	boolean _mediaplayerIsBuffering;
	boolean _proxyIsBuffering;

	private static final int HANDLE_BROADCAST_MSG = 1;
	private static final int SHOW_PROGRESS = 2;
	private static final int UPDATE_PROGRESS = 3;
	private static final int HIDE_PROGRESS = 4;
	private static final int SHOW_NETWORK_SPEED = 5;
	private static final int SHOW_BUFFER_PERCENT = 6;
	private static final int HIDE_CONTROLLER = 7;
	private static final int UPDATE_CONTROLLER_INFO = 8;
	

	private static final int REFRESH_NETWORK_INTERVAL = 1000;
	private static final int REFRESH_BUFFER_INTERVAL = 1000;

	private static final int NETWORK_SPEED_SAMPLE_COUNT = 10;

	private boolean _isProxyRunning = false;
	private boolean _isPlaying = false;
	private int _downloadSpeedSampleCount = 1;
	private long _lastTime = 0;
	private long _startTime = 0;
	private long _currentBytes = 0;
	private LinkedList<Long> _downloadSpeed = new LinkedList<Long>();
	
	
	private MATDialog dialog;
	private int _progress;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO:onCreate
		setContentView(R.layout.playershow);
		registeBR();
		initView();
		initMediaPlayer();
		Intent i = getIntent();
		String cmd = i.getStringExtra("cmd");
		title = i.getStringExtra("title");
		controller.setFilmName(title);
		IP = i.getStringExtra("IP");
		Log.d(MATService.TAG, "cause page IP-->" + IP);

		String url = i.getStringExtra("url");
//		String url = "http://122.72.0.150/11/35/98/2111612248.0.letv?" +
//				"crypt=530c9f49aa7f2e158&b=388&qos=4&level=20&nc=1&bf=33&video_type" +
//				"=flv&check=1&tm=1343331000&key=292ab5721923baaf87c704c082ebb8f1&proxy" +
//				"=2006184720&s=9&df=11/35/98/2111612248.0.flv&br=388";

		Log.d(MATService.TAG, "use HttpProxy---------->"+useHttpProxy);
		
		dialog.setMessage("视频准备中,即将为您播放:"+title);
		dialog.show();
		
		try {
			if (cmd.equals("play")) {
				if(useHttpProxy){
					initHttpProxy(url, "");
				}else{
					mediaPlayer.setDataSource(url);
					mediaPlayer.prepareAsync();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("mediaplayer 设置数据中出错，错误信息：" + e.toString());
			finish();
		}
		currentDisplay = getWindowManager().getDefaultDisplay();
	}

	OnBufferingStatus _bufferStatuslistener = new OnBufferingStatus() {
		public void onStartOfBuffering() {
			Log.i(MATService.TAG, "proxy start of buffering");
			_proxyIsBuffering = true;
			_handler.sendEmptyMessage(SHOW_PROGRESS);
		}

		public void onStopOfBuffering() {
			Log.i(MATService.TAG, "proxy stop of buffering");
			_proxyIsBuffering = false;
			_handler.sendEmptyMessage(HIDE_PROGRESS);
		}

		public void onUpdateBuffering(int percent) {
			Log.i(MATService.TAG, "proxy buffering percent " + percent);
			_progress = percent;
			_handler.sendEmptyMessage(UPDATE_PROGRESS);
		}
	};

	void setDebugFlag(boolean flag) {
		VHttpProxyDaemon.enableSessionLog(flag);
		VHttpProxyDaemon.enableDataSourceLog(flag);
		VHttpProxyDaemon.enableDataSourceBufferLog(flag);
		VHttpProxyDaemon.enableDataSourceM3u8Log(flag);
	}

	private void showBufferInfo() {
		int p = _httpProxy.getBufferPercent();
		controller._bufferPercentTextView.setText(String.valueOf(p));
		int b = _httpProxy.getBufferAvaiableBytes();
		b /= 1024;
		controller._bufferBytesTextView.setText(String.valueOf(b));
	}

	private void startBufferInfo() {
		_handler.sendEmptyMessageDelayed(SHOW_BUFFER_PERCENT,REFRESH_BUFFER_INTERVAL);
	}

	private void stopBufferInfo() {
		_handler.removeMessages(SHOW_BUFFER_PERCENT);
	}

	private long showNetworkInfo() {
		if(_httpProxy==null)return 0;
		long bytes = _httpProxy.getBufferReadInBytes();
		long time = System.currentTimeMillis();
		long avgSpeed = bytes / 1024 / ((time - _startTime) / 1000);
		controller._avgSpeedTextView.setText(String.valueOf(avgSpeed));

		long dt = time - _lastTime;
		long db = bytes - _currentBytes;

		if (db < 0)
			return 0;
		if(dt<=0)return 0;

		_currentBytes = bytes;
		_lastTime = time;

		long thisSpeed = db * 1000 / dt;
		_downloadSpeed.addLast(thisSpeed);
		if (_downloadSpeedSampleCount < NETWORK_SPEED_SAMPLE_COUNT) {
			++_downloadSpeedSampleCount;
			return 0;
		}

		long total = 0;
		for (long each : _downloadSpeed) {
			total += each;
		}

		long lastestSpeed = total / 1024 / _downloadSpeed.size();
		_downloadSpeed.removeFirst();
		controller._instantSpeedTextView.setText(String.valueOf(lastestSpeed));
		return lastestSpeed;
	}

	private void startNetworkInfo() {
		_handler.sendEmptyMessageDelayed(SHOW_NETWORK_SPEED,
				REFRESH_NETWORK_INTERVAL);
	}

	private void stopNetworkInfo() {
		_handler.removeMessages(SHOW_NETWORK_SPEED);
	}

	public void doPlay(Uri uri) {
		if (uri == null) {
			Log.e(MATService.TAG, "uri is null");
			return;
		}

		if (_isPlaying) {
			Log.i(MATService.TAG, "is playing, stop it first");
			return;
		}
		_lastTime = System.currentTimeMillis();
		_startTime = _lastTime;
		_currentBytes = 0;
		_downloadSpeed.clear();
		_downloadSpeedSampleCount = 1;

		_isPlaying = true;
		startNetworkInfo();
		startBufferInfo();

		Log.i(MATService.TAG, "uri is " + uri.toString());

//		mediaPlayer.reset();
		mediaPlayer.setDisplay(surfaceHolder);
		try {
			mediaPlayer.setDataSource(uri.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
		mediaPlayer.prepareAsync();
	}

	// -------------------------------------------

	Handler _handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			//TODO:handleMsg
			super.handleMessage(msg);
			switch (msg.what) {
			case HANDLE_BROADCAST_MSG:
				handleBroadcastMsg(msg);
				break;
			case SHOW_PROGRESS:
				dialog.show();
				return;
			case UPDATE_PROGRESS:
				dialog.setMessage("正在努力加载中...");
				if(useHttpProxy)dialog.setProgress(_progress);
				return;
			case HIDE_PROGRESS:
				if(dialog.isShowing())
				dialog.hide();
				return;
			case SHOW_NETWORK_SPEED:
				showNetworkInfo();
				startNetworkInfo();
				return;
			case SHOW_BUFFER_PERCENT:
				showBufferInfo();
				startBufferInfo();
				return;
			case HIDE_CONTROLLER:
				controller.hide();
				break;	
			case UPDATE_CONTROLLER_INFO:
				updateControllerInfo();
				break;	
			default:
				break;
			}

		}

		private void handleBroadcastMsg(Message msg) {
			Intent intent = (Intent) msg.obj;
			String cmd = intent.getStringExtra("cmd");
			Log.d(MATService.TAG, "onReceive cmd--->" + cmd);
			if (cmd.equals("pause")) {
				if (controller != null){
					controller.pause();
				}
			} else if (cmd.equals("stop")) {
				finish();
			} else if (cmd.equals("resume")) {
				if (!mediaPlayer.isPlaying()) {
					controller.play();
				} else
					Log.d(MATService.TAG, "该视频已经在播放了。。。");
				Intent i = new Intent(ACTION);
				i.putExtra("cmd", "state");
				i.putExtra("isPlaying", true);
				sendBroadcast(i);
			} else if (cmd.equals("seek")) {
				int pos = intent.getIntExtra("pos", 0);
				try {
					if (mediaPlayer != null && isPrepared) {
						if(!dialog.isShowing()){
							dialog.setMessage("正在努力加载中...");
							dialog.show();
						
						}
						controller.seekTo(mediaPlayer.getDuration() * pos / 100);
						Intent i = new Intent(ACTION);
						i.putExtra("cmd", "state-duration");
						i.putExtra("pos", mediaPlayer.getCurrentPosition());
						sendBroadcast(i);
					}
				} catch (Exception e) {
					finish();
				}
			}
		}
	};

	BroadcastReceiver br = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Message msg = new Message();
			msg.what = HANDLE_BROADCAST_MSG;
			msg.obj = intent;
			_handler.sendMessageDelayed(msg, 100);
		}
	};

	
	private void initHttpProxy(String url, String type) {
		// TODO:doPlay
		try {
			_httpProxy = new VHttpProxyDaemon(_port);
			_httpProxy.setBufferSize(4*1024*1024);
			_isProxyRunning = true;
			_httpProxy.setOnBufferingStatusListener(_bufferStatuslistener);
			_httpProxy.enableAutoBuffering(_enableProxyAutoBuffering);
			setDebugFlag(_isDebug);
			_httpProxy.start();

			Uri uri = _httpProxy.buildUri(url, "normal");// type
			doPlay(uri);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 1、更新进度条
	 * 2、更新时间
	 * 3、更新下载速度
	 */
	protected void updateControllerInfo() {
		// TODO Auto-generated method stub
		try {
			if (mediaPlayer != null && mediaPlayer.isPlaying()) {
				int position = mediaPlayer.getCurrentPosition();
				int duration = mediaPlayer.getDuration();
				if (duration > 0) {
					String txt = stringForTime(position) + "/" + stringForTime(duration);
					controller.time.setText(txt);
					controller.seekBar.setProgress(100 * position / duration);
//				controller.seekBar.setSecondaryProgress(???+100*position/duration);//???=100*blocksize*_progress/totalsize;
					if(useHttpProxy)controller.bps.setText(showNetworkInfo()+"KBps");
				}
				return;
			}
		} catch (Exception e) {
			finish();
		}
	}
	
	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;
		return hours + ":" + minutes + ":" + seconds;
	}
	
	void initView() {

		controller = new MATController(this);
		dialog = createDialog();

		surfaceView = (SurfaceView) findViewById(R.id.page_playershow_surfaceview);
		surfaceView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if(controller.isShowing()){
					controller.hide();
				}else{
					controller.show();
				}
			}
		}) ;
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.setFixedSize(500, 500);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	
	boolean isBrRegisted = false;
	private void registeBR() {
		isBrRegisted = true;
		registerReceiver(br, new IntentFilter("com.rushfusion.matservice"));
		br.setOrderedHint(true);
		Log.d("MATService", "onCreate registerReceiver");
	}

	private void initMediaPlayer() {
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
	public void surfaceChanged(SurfaceHolder holder, int format, int width,int height) {
		Log.d(MATService.TAG, "==============>surfaceChanged<===============");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(MATService.TAG, "==============>surfaceCreated<===============");
		try {
			mediaPlayer.setDisplay(holder);
		} catch (IllegalStateException e) {
			e.printStackTrace();
			Log.d(MATService.TAG, "surface准备中出错 ，错误信息 ：" + e.toString());
			finish();
		}
	}

	private MATDialog createDialog() {
		MATDialog dialog = new MATDialog(this,R.style.dialog);
		dialog.setMessage("视频加载中，请稍候...");
		dialog.setCancelable(false);
		return dialog;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(MATService.TAG, "==============>surfaceDestroyed<===============");
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
	}

	@Override
	public void onVideoSizeChanged(MediaPlayer mp, int arg1, int arg2) {
		Log.d(MATService.TAG, "==============>onVideoSizeChanged<===============");
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
			
			ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
			params.width = videoWidth;
			params.height = videoHeight;
			surfaceView.setLayoutParams(params);
			
		} else {
			surfaceHolder.setFixedSize(videoWidth, videoHeight);
		}
	}

	@Override
	public void onSeekComplete(MediaPlayer mp) {
		Log.d(MATService.TAG, "==============>onSeekComplete<===============");
		if(dialog.isShowing())dialog.dismiss();
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		Log.d(MATService.TAG, "==============>onPrepared<===============");
		_handler.sendEmptyMessage(HIDE_PROGRESS);
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
			ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
			params.width = videoWidth;
			params.height = videoHeight;
			surfaceView.setLayoutParams(params);
			
		} else {
			surfaceHolder.setFixedSize(videoWidth, videoHeight);
		}
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		controller.play();
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", true);
		sendBroadcast(i);
		isPrepared = true;
	}

	@Override
	protected void onPause() {
		Log.d(MATService.TAG, "==============>onPause<===============");
		if (_isPlaying) {
			mediaPlayer.stop();
			_isPlaying = false;
		}
		if (_isProxyRunning) {
			_httpProxy.stop();
			_isProxyRunning = false;
		}

		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		if (controller != null && controller.isShowing())
			controller.hide();
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", false);
		sendBroadcast(i);
		super.onPause();
	}

	@Override
	public void onBufferingUpdate(MediaPlayer mp, int percent) {
		Log.i(MATService.TAG, "==========>onBufferingUpdate  percent:"+percent+" <==========");
		if(!useHttpProxy){
			_progress = percent;
			_handler.sendEmptyMessage(UPDATE_PROGRESS);
		}
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		switch (what) {
		case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
			Log.d(MATService.TAG, "onInfo 音视频交叉错误");
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			Log.d(MATService.TAG, "onInfo 音视频交叉错误");
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			Log.i(MATService.TAG, "onInfo MEDIA_INFO_BUFFERING_START");
			if(useHttpProxy){
				_mediaplayerIsBuffering = true;
				if (_enableProxyBuffering) {
					Log.i(MATService.TAG, "onInfo start proxy buffering");
					_httpProxy.startBuffering();
				}
			}else{
				dialog.setMessage("正在努力加载中...");
				_handler.sendEmptyMessage(SHOW_PROGRESS);
			}
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			Log.i(MATService.TAG, "onInfo MEDIA_INFO_BUFFERING_END");
			if(useHttpProxy){
				_mediaplayerIsBuffering = false;
			}else{
				_handler.sendEmptyMessage(HIDE_PROGRESS);
			}
			break;
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			Log.d(MATService.TAG, "onInfo 原资料更新");
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			Log.d(MATService.TAG, "onInfo 该视频类型，无法定位");
			break;
		}
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d(MATService.TAG, "==============>onError<===============");
		if (dialog != null && dialog.isShowing())
			dialog.dismiss();
		Intent intent = new Intent(ACTION);
		intent.putExtra("cmd", "error");
		intent.putExtra("errorCode", what);
		intent.putExtra("IP", IP);

		sendBroadcast(intent);

		Toast.makeText(this, "错误代码：" + what, 500).show();
		Log.d(MATService.TAG, "onError error -->" + what);
		finish();
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		Log.d(MATService.TAG, "==============>onCompletion<===============");
		Toast.makeText(this, "播放完毕", 500).show();
		Intent i = new Intent(ACTION);
		i.putExtra("cmd", "state-complete");
		i.putExtra("IP", IP);
		sendBroadcast(i);
		finish();
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
		Log.d(MATService.TAG, "==============>onStop<===============");
		if(dialog.isShowing())dialog.dismiss();
		if(useHttpProxy){
			stopBufferInfo();
			stopNetworkInfo();
			_httpProxy.stop();
			_isProxyRunning = false;
		}
		surfaceView.destroyDrawingCache();
		if(isBrRegisted)unregisterReceiver(br);
		Log.d(MATService.TAG, "onStop  unregisterReceiver");
		super.onStop();
	}

	class MATController implements MediaPlayerControl {
		private static final int HIDE_CONTROLLER_INTERVAL = 15000;
		//------------------------------
		private TextView _instantSpeedTextView ;
		private TextView _avgSpeedTextView ;
		private TextView _bufferPercentTextView ;
		private TextView _bufferBytesTextView ;
		//------------------------------
		
		
		private Activity context;
		private ViewGroup controller;
		private Button play_pause, next, prev;
		private TextView filmName, bps, time;
		private SeekBar seekBar;

		private boolean isShowing = false;

		public MATController(Activity context) {
			super();
			this.context = context;
			initControllerView();
		}

		private void initControllerView() {
			// TODO Auto-generated method stub
			controller = (ViewGroup) context.findViewById(R.id.controller);
			play_pause = (Button) controller.findViewById(R.id.controller_play_pause);
			next = (Button) controller.findViewById(R.id.controller_next);
			prev = (Button) controller.findViewById(R.id.controller_prev);
			filmName = (TextView) controller.findViewById(R.id.controller_filename);
			bps = (TextView) controller.findViewById(R.id.controller_bps);
			time = (TextView) controller.findViewById(R.id.controller_process);
			seekBar = (SeekBar) controller.findViewById(R.id.controller_seekbar);
			//------------------------------------------------------------------------			
			_instantSpeedTextView = (TextView) findViewById(R.id.speed);
			_avgSpeedTextView = (TextView) findViewById(R.id.avgspeed);
			_bufferPercentTextView = (TextView) findViewById(R.id.percent);
			_bufferBytesTextView = (TextView) findViewById(R.id.bufferbytes);
			//------------------------------------------------------------------------
			
			seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
				
				int pos = 0;
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
					Log.d(MATService.TAG, "seekbar pos--->"+pos);
					mediaPlayer.seekTo(mediaPlayer.getDuration() * pos / 100);
					lock = true;
					updateInfo(true);
				}
				
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
					
				}
				
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if(fromUser){
						lock = false;
						if(timer!=null)timer.cancel();
						if(tt!=null)tt.cancel();
						timer = null;
						tt = null;
						pos = seekBar.getProgress();
					}
				}
			});
			
			play_pause.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if(mediaPlayer.isPlaying()){
						pause();
					}else {
						play();
					}
				}
			});
			next.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

				}
			});
			prev.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {

				}
			});

		}
		
		public void setFilmName(String title) {
			filmName.setText("正在播放："+title);
		}

		boolean isShowing() {
			if (controller.getVisibility() == View.VISIBLE) {
				isShowing = true;
			} else
				isShowing = false;
			return isShowing;
		}

		void show() {
			controller.setVisibility(View.VISIBLE);
			_handler.sendEmptyMessageDelayed(HIDE_CONTROLLER, HIDE_CONTROLLER_INTERVAL);
			updateInfo(true);
		}

		void hide() {
			controller.setVisibility(View.INVISIBLE);
			updateInfo(false);
		}


		boolean lock = true;
		TimerTask tt ;
		Timer timer;
		
		void updateInfo(boolean flag){
			if(timer!=null){
				tt.cancel();
				timer.cancel();
				tt=null;
				timer = null;
			}
			if(flag){
				timer = new Timer();
				tt = new TimerTask() {
					
					@Override
					public void run() {
						if(lock) {
							_handler.sendEmptyMessage(UPDATE_CONTROLLER_INFO);
						}
					}
				};
				timer.schedule(tt, 1000, 1000);
			}
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
						currentPosition = mediaPlayer.getCurrentPosition();
						return currentPosition;
					} else {
						return currentPosition;
					}
				}
			} catch (Exception e) {
				Log.d(MATService.TAG, "mediaPlayer.getCurrentPosition() 出错了！");
				finish();
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
				Log.d(MATService.TAG, "mediaPlayer.getDuration() 出错了！");
				finish();
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
				Log.d(MATService.TAG, "mediaPlayer.isPlaying() 出错了！");
				finish();
			}
			return false;
		}

		@Override
		public void pause() {
			play_pause.setBackgroundResource(R.drawable.player_play_selector);
			show();
			try {
				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
				}
			} catch (Exception e) {
				finish();
			}
		}

		void play(){
			play_pause.setBackgroundResource(R.drawable.player_pause_selector);
			start();
		}
		
		@Override
		public void seekTo(int pos) {
			show();
			try {
				if (mediaPlayer != null) {
					mediaPlayer.seekTo(pos);
				}
			} catch (Exception e) {
				Log.d(MATService.TAG, "mediaplayer.seekto 出错了！");
				finish();
			}
		}

		
		
		@Override
		public void start() {
			show();
			try {
				if (mediaPlayer != null) {
					mediaPlayer.start();
				}
			} catch (Exception e) {
				Log.d(MATService.TAG, "mediaplayer.start 出错了！");
				finish();
			}
		}
		
		@Override
		public int getBufferPercentage() {
			return 0;
		}
		
	}
	
	
	
	
	class MATDialog extends Dialog{

		
		
		TextView msgText,progressText;
		ImageView logo;
		
		public MATDialog(Context context, int theme) {
			super(context, theme);
			initContentView();
		}
		
		private View initContentView() {
			View v = LayoutInflater.from(Service_MediaPlayer.this).inflate(R.layout.dialog, null);
			msgText = (TextView) v.findViewById(R.id.msg);
			progressText = (TextView) v.findViewById(R.id.progress);
			logo = (ImageView) v.findViewById(R.id.dialoglogo);
			setContentView(v);
			return v;
		}

		void setMessage(String msg){
			if(msg==null)return;
			msgText.setText(msg);
		}
		
		void setProgress(int progress){
			if(progress<0)return;
			progressText.setText(progress+"%");
		}
		
		void setLogo(int resId){
			logo.setImageResource(resId);
		}

		void setLogo(Bitmap bm){
			if(bm==null)return;
			logo.setImageBitmap(bm);
		}
		
		void setLogo(Drawable drawable){
			if(drawable==null)return;
			logo.setImageDrawable(drawable);
		}

	}
	
	
	
	

}
