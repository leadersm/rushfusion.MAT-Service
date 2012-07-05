package com.rushfusion.matservice;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

public class MediaPlayerShow extends Activity implements
		OnBufferingUpdateListener, OnVideoSizeChangedListener,
		OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPreparedListener, OnSeekCompleteListener, Callback,
		MediaPlayerControl {
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	MediaPlayer mediaPlayer;
	MediaController controller;
	Display currentDisplay;
	int videoWidth = 0;
	int videoHeight = 0;
	int contiuePosition = 0;
	String title;
	boolean isContinue = false;
	ProgressDialog pDialog;
	int saveTime = 0;

	BroadcastReceiver br = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String cmd = intent.getStringExtra("cmd");
			System.out.println("onReceive cmd--->" + cmd);
			if (cmd.equals("pause")) {
				pause();
			} else if (cmd.equals("stop")) {
				if(pDialog.isShowing()){
					pDialog.dismiss();
				}
				mediaPlayer.stop();
				Intent i = new Intent("com.rushfusion.matshow");
				i.putExtra("cmd","release");
				sendBroadcast(i);
				finish();
			} else if(cmd.equals("resume")){
				mediaPlayer.start();
				Intent i = new Intent("com.rushfusion.matshow");
				i.putExtra("cmd", "state");
				i.putExtra("isPlaying", true);
				sendBroadcast(i);
			} else if(cmd.equals("reset")){
				pause();
				mediaPlayer.reset();
				String url = intent.getStringExtra("url");
				try {
					mediaPlayer.setDataSource(url);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				mediaPlayer.start();
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.playershow);
		surfaceView = (SurfaceView) findViewById(R.id.page_playershow_surfaceview);

		registerReceiver(br, new IntentFilter("com.rushfusion.matservice"));
        Intent i = getIntent();
        String cmd = i.getStringExtra("cmd");
        
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
		
		title = i.getStringExtra("title");
		try {
			if(cmd.equals("play")){
	        	String url = i.getStringExtra("url");
	        	mediaPlayer.setDataSource(url);
	        }
		} catch (IllegalArgumentException e) {
			System.out.println("mediaplayer 设置数据中出错，错误信息：" + e.toString());
		} catch (IllegalStateException e) {
			System.out.println("mediaplayer 设置数据中出错，错误信息：" + e.toString());
		} catch (IOException e) {
			System.out.println("mediaplayer 设置数据中出错，错误信息：" + e.toString());
		}
		currentDisplay = getWindowManager().getDefaultDisplay();
		controller = new MediaController(this, false);
		surfaceView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (controller.isShowing()) {
					controller.hide();
				} else {
					controller.show();
				}
			}
		});
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
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				saveTime = mediaPlayer.getCurrentPosition();
				return saveTime;
			} else {
				return saveTime;
			}
		}
		return 0;
	}

	@Override
	public int getDuration() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying()) {
				return mediaPlayer.getDuration();
			} else {
				return 0;
			}
		}
		return 0;
	}

	@Override
	public boolean isPlaying() {
		if (mediaPlayer != null) {
			return mediaPlayer.isPlaying();
		}
		return false;
	}

	@Override
	public void pause() {
		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		}
	}

	
	@Override
	public void seekTo(int pos) {
		if (mediaPlayer != null) {
			// mediaPlayer.pause();
			pDialog.show();
			mediaPlayer.seekTo(pos);
		} else {
			System.out.println("mediaplayer.seekto 出错了！");
			finish();
		}

	}

	@Override
	public void start() {
		if (mediaPlayer != null) {
			mediaPlayer.start();
		} else {
			System.out.println("mediaplayer.start 出错了！");
			finish();
		}
	}

	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mediaPlayer.setDisplay(holder);
		try {
			mediaPlayer.prepareAsync();
		} catch (IllegalStateException e) {
			System.out.println("surface准备中出错 ，错误信息 ：" + e.toString());
		}
		pDialog = new ProgressDialog(this);
		pDialog.setMessage("视频加载中，请稍后");
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.show();
	}

	
	

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (mediaPlayer != null) {
			// saveTime = mediaPlayer.getCurrentPosition();
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
			pDialog.cancel();
		} else {
			System.out.println("mediaplayer.onseekcomplete出错了");
			pDialog.cancel();
		}

	}

	@Override
	public void onPrepared(MediaPlayer mp) {
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
		controller.setMediaPlayer(this);
		controller.setAnchorView(this
				.findViewById(R.id.page_playershow_mainview));
		controller.setEnabled(true);
		controller.show();
		pDialog.cancel();
		if (isContinue) {
			AlertDialog dialog = new AlertDialog.Builder(this).create();
			dialog.setMessage("是否从上次中断处继续播放？");
			dialog.setButton(Dialog.BUTTON_POSITIVE, "继续上次播放",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							mediaPlayer.seekTo(contiuePosition);
						}
					});
			dialog.setButton(Dialog.BUTTON_NEGATIVE, "从头播放",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							mediaPlayer.seekTo(0);
						}
					});
			dialog.show();
		}
		mediaPlayer.start();
		Intent i = new Intent("com.rushfusion.matshow");
		i.putExtra("cmd", "state");
		i.putExtra("isPlaying", true);
		sendBroadcast(i);
	}

	@Override
	protected void onPause() {
		if (mediaPlayer != null) {
			mediaPlayer.release();
		}
		Intent i = new Intent("com.rushfusion.matshow");
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
			System.out.println("音视频交叉错误");
			break;
		case MediaPlayer.MEDIA_INFO_METADATA_UPDATE:
			System.out.println("原资料更新");
			break;
		case MediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
			System.out.println("该视频类型，无法定位");
			break;
		case MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
			System.out.println("音视频交叉错误");
			break;
		}
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// Toast.makeText(this, "错误代码："+what , 500).show();
		switch (what) {
		case MediaPlayer.MEDIA_ERROR_UNKNOWN:
			Toast.makeText(this, "网络连接出现错误，请稍后再试！", 1000).show();
			finish();
			break;
		case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
			Toast.makeText(this, "网络连接出现错误，请稍后再试！", 1000).show();
			finish();
			break;
		case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
			Toast.makeText(this, "网络连接出现错误，请稍后再试！", 1000).show();
			finish();
			break;
		}
		return true;
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {

		Toast.makeText(this, "播放完毕", 500).show();
		// finish();
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
		unregisterReceiver(br);
		super.onStop();
	}
	
}
