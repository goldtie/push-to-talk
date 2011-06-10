package org.sipdroid.sipua.ui;

/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2007 The Android Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.ArrayList;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.net.RtpPacket;
import org.sipdroid.net.RtpSocket;
import org.sipdroid.net.SipdroidSocket;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.XMPPEngine;

import android.content.Context;
import android.hardware.Camera;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

public class PTTCallScreen extends CallScreen implements SipdroidListener,
SurfaceHolder.Callback, MediaRecorder.OnErrorListener,
MediaPlayer.OnErrorListener, OnClickListener, OnLongClickListener {

	Thread t;
	Context mContext = this;

	private static final String TAG = "PTTCallScreen";

	private static int UPDATE_RECORD_TIME = 1;

	private static final float VIDEO_ASPECT_RATIO = 176.0f / 144.0f;
	VideoPreview mVideoPreview;
	SurfaceHolder mSurfaceHolder = null;
	VideoView mVideoFrame;
	MediaController mMediaController;

	private MediaRecorder mMediaRecorder;
	private boolean mMediaRecorderRecording = false;

	private TextView mRecordingTimeView;

	private static ListView mChatContentList;
	private EditText mSendText;

	private TextView mAudioStatus, mVideoStatus;

	ArrayList<MenuItem> mGalleryItems = new ArrayList<MenuItem>();

	View mPostPictureAlert;
	LocationManager mLocationManager = null;

	private Handler mHandler = new MainHandler();
	LocalSocket receiver, sender;
	LocalServerSocket lss;
	int obuffering;

	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int AUDIO_REQUEST_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int AUDIO_RELEASE_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int VIDEO_REQUEST_MENU_ITEM = FIRST_MENU_ID + 3;
	public static final int VIDEO_RELEASE_MENU_ITEM = FIRST_MENU_ID + 4;
	public static final int SPEAKER_MENU_ITEM = FIRST_MENU_ID + 5;
	public static final int HANG_UP_MENU_ITEM = FIRST_MENU_ID + 6;

	public static boolean isAudioSending = false;
	public static boolean isVideoSending = false;

	final String strAudio = "Audio: ";
	final String strVideo = "Video: ";

	/** MBCPProcess */
	public AudioMBCPProcess audio_mbcp_process = null;
	public VideoMBCPProcess video_mbcp_process = null;


	/**
	 * This Handler is used to post message back onto the main thread of the
	 * application
	 */
	private class MainHandler extends Handler {
		@Override
		public void handleMessage(android.os.Message msg) {
			long now = SystemClock.elapsedRealtime();
			long delta = now - Receiver.ccCall.base;

			long seconds = (delta + 500) / 1000; // round to nearest
			long minutes = seconds / 60;
			long hours = minutes / 60;
			long remainderMinutes = minutes - (hours * 60);
			long remainderSeconds = seconds - (minutes * 60);

			String secondsString = Long.toString(remainderSeconds);
			if (secondsString.length() < 2) {
				secondsString = "0" + secondsString;
			}
			String minutesString = Long.toString(remainderMinutes);
			if (minutesString.length() < 2) {
				minutesString = "0" + minutesString;
			}
			String text = minutesString + ":" + secondsString;
			if (hours > 0) {
				String hoursString = Long.toString(hours);
				if (hoursString.length() < 2) {
					hoursString = "0" + hoursString;
				}
				text = hoursString + ":" + text;
			}

			mRecordingTimeView.setText(text);

			if (mVideoFrame != null) {
				int buffering = mVideoFrame.getBufferPercentage();
				if (buffering != 100 && buffering != 0) {
					mMediaController.show();
				}
				if (buffering != 0 && !mMediaRecorderRecording)
					mVideoPreview.setVisibility(View.INVISIBLE);
				if (obuffering != buffering && buffering == 100
						&& rtp_socket != null) {
					RtpPacket keepalive = new RtpPacket(new byte[12], 0);
					keepalive.setPayloadType(125);
					try {
						rtp_socket.send(keepalive);
					} catch (IOException e) {
					}
				}
				obuffering = buffering;
			}

			// for Audio status and video status 
			mAudioStatus.setText(strAudio+AudioMBCPProcess.Status);
			mVideoStatus.setText(strVideo+VideoMBCPProcess.Status);
			//mVideoStatus.setText(AudioMBCPProcess.audioStatus);
			// Work around a limitation of the T-Mobile G1: The T-Mobile
			// hardware blitter can't pixel-accurately scale and clip at the
			// same time,
			// and the SurfaceFlinger doesn't attempt to work around this
			// limitation.
			// In order to avoid visual corruption we must manually refresh the
			// entire
			// surface view when changing any overlapping view's contents.
			mVideoPreview.invalidate();
			mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
		}
	};

	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Log.d("HAO", "PTTCallScreen_OnCreate");
		super.onCreate(icicle);

		audio_mbcp_process = new AudioMBCPProcess(this);
		video_mbcp_process = new VideoMBCPProcess(this);

		android.util.Log.d("HAO", "Sipdroid.audio_mbcp_process.HelloMSG(user_profile.username)");
		audio_mbcp_process.HelloMSG(Receiver.mSipdroidEngine.user_profiles[0].username);

		android.util.Log.d("HAO", "Sipdroid.audio_mbcp_process.HelloMSG(user_profile.username)");
		video_mbcp_process.HelloMSG(Receiver.mSipdroidEngine.user_profiles[0].username);

		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		// setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setScreenOnFlag();
		setContentView(R.layout.ptt_call);

		mVideoPreview = (VideoPreview) findViewById(R.id.camera_preview1);
		mVideoPreview.setAspectRatio(VIDEO_ASPECT_RATIO);

		// don't set mSurfaceHolder here. We have it set ONLY within
		// surfaceCreated / surfaceDestroyed, other parts of the code
		// assume that when it is set, the surface is also set.
		SurfaceHolder holder = mVideoPreview.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mAudioStatus = (TextView) findViewById(R.id.audio_status);
		mVideoStatus = (TextView) findViewById(R.id.video_status);

		mRecordingTimeView = (TextView) findViewById(R.id.recording_time1);
		mVideoFrame = (VideoView) findViewById(R.id.video_frame1);

		mChatContentList = (ListView) this.findViewById(R.id.listMessages);
		mChatContentList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		mChatContentList.setStackFromBottom(true);
		
		mSendText = (EditText) this.findViewById(R.id.txtInputChat);
		Button send = (Button) this.findViewById(R.id.btnSend);

		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				String text = mSendText.getText().toString();
				mSendText.setText("");
				Receiver.onMsgStatus(XMPPEngine.XMPP_STATE_OUTCOMING_MSG, text);
			}
		});
		
	}
	
	public static void updateListContent(ArrayAdapter<String> adapter){
		if(mChatContentList != null)
			mChatContentList.setAdapter(adapter);
	}

	int speakermode;
	boolean justplay;

	@Override
	public void onStart() {
		Log.d("SIPDROID", "[PTTCallScreen] - onStart");
		super.onStart();
		// SUA TAM
		// -> speakermode =
		// Receiver.engine(this).speaker(AudioManager.MODE_NORMAL);
		videoQualityHigh = PreferenceManager.getDefaultSharedPreferences(
				mContext).getString(
						org.sipdroid.sipua.ui.Settings.PREF_VQUALITY,
						org.sipdroid.sipua.ui.Settings.DEFAULT_VQUALITY).equals("high");
		if ((intent = getIntent()).hasExtra(MediaStore.EXTRA_VIDEO_QUALITY)) {
			int extraVideoQuality = intent.getIntExtra(
					MediaStore.EXTRA_VIDEO_QUALITY, 0);
			videoQualityHigh = (extraVideoQuality > 0);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("HAO", "PTTCallScreen_onResume");
		if (!Sipdroid.release)
			Log.i("SipUA:", "on resume");
		justplay = intent.hasExtra("justplay");
		if (!justplay) {
			receiver = new LocalSocket();
			try {
				lss = new LocalServerSocket("Sipdroid");
				receiver.connect(new LocalSocketAddress("Sipdroid"));
				receiver.setReceiveBufferSize(500000);
				receiver.setSendBufferSize(500000);
				sender = lss.accept();
				sender.setReceiveBufferSize(500000);
				sender.setSendBufferSize(500000);
			} catch (IOException e1) {
				if (!Sipdroid.release)
					e1.printStackTrace();
				Log.d("SIPDROID", "[PTTCallScreen] - onResume - Exepction: " + e1.getMessage());
				//super.onResume();
				finish();
				return;
			}
			checkForCamera();
			mVideoPreview.setVisibility(View.VISIBLE);
			if (!mMediaRecorderRecording)
				initializeVideo();
			startVideoRecording();
			//} else if (Receiver.engine(mContext).getRemoteVideo() != 0) {
			Log.d("SIPDROID", "[PTTCallScreen] - onResume - start video recording successfully");
			mVideoFrame.setVideoURI(Uri.parse("rtsp://220.149.84.222/livetv.sdp"));
			//			mVideoFrame.setVideoURI(Uri.parse("rtsp://"+Receiver.engine(mContext).getRemoteAddr()+"/"+
			//	        		Receiver.engine(mContext).getRemoteVideo()+"/sipdroid"));
			mVideoFrame.setMediaController(mMediaController = new MediaController(this));
			mVideoFrame.setOnErrorListener(this);
			mVideoFrame.requestFocus();
			mVideoFrame.start();
		}

		mRecordingTimeView.setText("");
		mRecordingTimeView.setVisibility(View.VISIBLE);
		mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
		
	}

	@Override
	public void onPause() {
		Log.d("SIPDROID", "[PTTCallScreen] - onPause");
		super.onPause();

		// This is similar to what mShutterButton.performClick() does,
		// but not quite the same.
		if (mMediaRecorderRecording) {
			stopVideoRecording();

			try {
				lss.close();
				receiver.close();
				sender.close();
			} catch (IOException e) {
				if (!Sipdroid.release)
					e.printStackTrace();
			}
		}

		Receiver.engine(this).speaker(speakermode);
		finish();
	}

	/*
	 * catch the back and call buttons to return to the in call activity.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d("HAO", "PTTCallScreen_onKeyDown");
		switch (keyCode) {
		// finish for these events
		case KeyEvent.KEYCODE_CALL:
			Receiver.engine(this).togglehold();
		case KeyEvent.KEYCODE_BACK:
			//finish();
			return true;

		case KeyEvent.KEYCODE_CAMERA:
			// Disable the CAMERA button while in-call since it's too
			// easy to press accidentally.
			return true;

		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			RtpStreamReceiver.adjust(keyCode, true);
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d("HAO", "PTTCallScreen_surfaceChanged");
		if (!justplay && !mMediaRecorderRecording)
			initializeVideo();
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("HAO", "PTTCallScreen_surfaceCreated");
		mSurfaceHolder = holder;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("HAO", "PTTCallScreen_surfaceDestroyed");
		mSurfaceHolder = null;
	}

	boolean isAvailableSprintFFC, useFront = true;

	private void checkForCamera() {
		Log.d("HAO", "PTTCallScreen_checkForCamera");
		try {
			Class.forName("android.hardware.HtcFrontFacingCamera");
			isAvailableSprintFFC = true;
		} catch (Exception ex) {
			isAvailableSprintFFC = false;
		}
	}

	boolean videoQualityHigh;
	Camera mCamera;

	// initializeVideo() starts preview and prepare media recorder.
	// Returns false if initializeVideo fails
	private boolean initializeVideo() {
		Log.d("HAO", "PTTCallScreen_initializeVideo");
		Log.v(TAG, "initializeVideo");

		if (mSurfaceHolder == null) {
			Log.v(TAG, "SurfaceHolder is null");
			return false;
		}

		mMediaRecorderRecording = true;

		if (mMediaRecorder == null)
			mMediaRecorder = new MediaRecorder();
		else
			mMediaRecorder.reset();
		if (mCamera != null) {
			if (Integer.parseInt(Build.VERSION.SDK) >= 8)
				VideoCameraNew2.reconnect(mCamera);
			mCamera.release();
			mCamera = null;
		}

		if (useFront && Integer.parseInt(Build.VERSION.SDK) >= 5) {
			if (isAvailableSprintFFC) {
				try {
					Method method = Class.forName(
					"android.hardware.HtcFrontFacingCamera")
					.getDeclaredMethod("getCamera", null);
					mCamera = (Camera) method.invoke(null, null);
				} catch (Exception ex) {
					Log.d(TAG, ex.toString());
				}
			} else {
				mCamera = Camera.open();
				Camera.Parameters parameters = mCamera.getParameters();
				parameters.set("camera-id", 2);
				mCamera.setParameters(parameters);
			}
			VideoCameraNew.unlock(mCamera);
			mMediaRecorder.setCamera(mCamera);
			mVideoPreview.setOnClickListener(this);
		}
		mVideoPreview.setOnLongClickListener(this);
		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		mMediaRecorder.setOutputFile(sender.getFileDescriptor());

		// Use the same frame rate for both, since internally
		// if the frame rate is too large, it can cause camera to become
		// unstable. We need to fix the MediaRecorder to disable the support
		// of setting frame rate for now.
		mMediaRecorder.setVideoFrameRate(20);
		if (videoQualityHigh) {
			mMediaRecorder.setVideoSize(352, 288);
		} else {
			mMediaRecorder.setVideoSize(176, 144);
		}
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H263);
		mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());

		try {
			mMediaRecorder.prepare();
			mMediaRecorder.setOnErrorListener(this);
			mMediaRecorder.start();
		} catch (IOException exception) {
			releaseMediaRecorder();
			finish();
			return false;
		}
		return true;
	}

	private void releaseMediaRecorder() {
		Log.d("HAO", "PTTCallScreen_releaseMediaRecorder");

		Log.v(TAG, "Releasing media recorder.");
		if (mMediaRecorder != null) {
			mMediaRecorder.reset();
			if (mCamera != null) {
				if (Integer.parseInt(Build.VERSION.SDK) >= 8)
					VideoCameraNew2.reconnect(mCamera);
				mCamera.release();
				mCamera = null;
			}
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
	}

	public void onError(MediaRecorder mr, int what, int extra) {
		Log.d("SIPDROID", "[PTTCallScreen] - void onError");
		if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
			finish();
		}
	}

	boolean change;

	protected void startVideoRecording() {
		Log.d("HAO", "PTTCallScreen_startVideoRecording");
		Log.v(TAG, "startVideoRecording");

		if (Receiver.listener_video == null) {
			Receiver.listener_video = this;
			RtpStreamSender.delay = 1;

			try {
				if (rtp_socket == null)
					rtp_socket = new RtpSocket(new SipdroidSocket(Receiver
							.engine(mContext).getLocalVideo()), InetAddress
							.getByName(Receiver.engine(mContext)
									.getRemoteAddr()), Receiver
									.engine(mContext).getRemoteVideo());

				//					rtp_socket = new RtpSocket(new SipdroidSocket(Receiver
				//							.engine(mContext).getLocalVideo()), InetAddress
				//							.getByName("220.149.84.222"), 1234);

			} catch (Exception e) {
				if (!Sipdroid.release)
					Log.d("SIPDROID", "[PTTCallScreen] - startVideoRecording - Exception: " + e.getMessage());
				return;
			}

			(t = new Thread() {
				public void run() {
					int frame_size = 1400;
					byte[] buffer = new byte[frame_size + 14];
					buffer[12] = 4;
					RtpPacket rtp_packet = new RtpPacket(buffer, 0);
					int seqn = 0;
					int num, number = 0, src, dest, len = 0, head = 0, lasthead = 0, cnt = 0, stable = 0;
					long now, lasttime = 0;
					double avgrate = videoQualityHigh ? 45000 : 24000;
					double avglen = avgrate / 20;

					InputStream fis = null;
					try {
						fis = receiver.getInputStream();
					} catch (IOException e1) {
						if (!Sipdroid.release)
							e1.printStackTrace();
						rtp_socket.getDatagramSocket().close();
						return;
					}

					rtp_packet.setPayloadType(103);
					while (Receiver.listener_video != null && videoValid()) {
						num = -1;
						try {
							num = fis.read(buffer, 14 + number, frame_size
									- number);
						} catch (IOException e) {
							if (!Sipdroid.release)
								e.printStackTrace();
							break;
						}
						if (num < 0) {
							try {
								sleep(20);
							} catch (InterruptedException e) {
								break;
							}
							continue;
						}
						number += num;
						head += num;
						try {
							if (lasthead != head + fis.available()
									&& ++stable >= 5) {
								now = SystemClock.elapsedRealtime();
								if (lasttime != 0) {

									avgrate = (double) fis.available() * 1000
									/ (now - lasttime);
								}
								if (cnt != 0 && len != 0)
									avglen = len / cnt;
								lasttime = now;
								lasthead = head + fis.available();
								len = cnt = stable = 0;
							}
						} catch (IOException e1) {
							if (!Sipdroid.release)
								e1.printStackTrace();
							break;
						}

						for (num = 14; num <= 14 + number - 2; num++)
							if (buffer[num] == 0 && buffer[num + 1] == 0)
								break;
						if (num > 14 + number - 2) {
							num = 0;
							rtp_packet.setMarker(false);
						} else {
							num = 14 + number - num;
							rtp_packet.setMarker(true);
						}

						rtp_packet.setSequenceNumber(seqn++);
						rtp_packet.setPayloadLength(number - num + 2);
						if (seqn > 10)
							try {
								if (isVideoSending)
									rtp_socket.send(rtp_packet);
								len += number - num;
							} catch (IOException e) {
								if (!Sipdroid.release)
									e.printStackTrace();
								break;
							}

							if (num > 0) {
								num -= 2;
								dest = 14;
								src = 14 + number - num;
								if (num > 0 && buffer[src] == 0) {
									src++;
									num--;
								}
								number = num;
								while (num-- > 0)
									buffer[dest++] = buffer[src++];
								buffer[12] = 4;

								cnt++;
								try {
									if (avgrate != 0)
										Thread
										.sleep((int) (avglen / avgrate * 1000));
								} catch (Exception e) {
									break;
								}
								rtp_packet.setTimestamp(SystemClock
										.elapsedRealtime() * 90);
							} else {
								number = 0;
								buffer[12] = 0;
							}
							if (change) {
								change = false;
								long time = SystemClock.elapsedRealtime();

								try {
									while (fis.read(buffer, 14, frame_size) > 0
											&& SystemClock.elapsedRealtime() - time < 3000)
										;
								} catch (Exception e) {
								}
								number = 0;
								buffer[12] = 0;
							}
					}
					rtp_socket.getDatagramSocket().close();
					try {
						while (fis.read(buffer, 0, frame_size) > 0)
							;
					} catch (IOException e) {
					}
				}
			}).start();
		}
	}

	private void stopVideoRecording() {
		Log.d("HAO", "PTTCallScreen_stopVideoRecording");

		Log.v(TAG, "stopVideoRecording");
		if (mMediaRecorderRecording || mMediaRecorder != null) {
			Receiver.listener_video = null;
			t.interrupt();
			RtpStreamSender.delay = 0;

			if (mMediaRecorderRecording && mMediaRecorder != null) {
				try {
					mMediaRecorder.setOnErrorListener(null);
					mMediaRecorder.setOnInfoListener(null);
					mMediaRecorder.stop();
				} catch (RuntimeException e) {
					Log.e(TAG, "stop fail: " + e.getMessage());
				}

				mMediaRecorderRecording = false;
			}
			releaseMediaRecorder();
		}
	}

	private void setScreenOnFlag() {
		Log.d("HAO", "PTTCallScreen_setScreenOnFlag");

		Window w = getWindow();
		final int keepScreenOnFlag = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
		if ((w.getAttributes().flags & keepScreenOnFlag) == 0) {
			w.addFlags(keepScreenOnFlag);
		}
	}

	public void onHangup() {
		Log.d("SIPDROID", "[PttCallScreen] - onHangup");

		finish();
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d("HAO", "PTTCallScreen_onKeyUp");

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
		case KeyEvent.KEYCODE_VOLUME_UP:
			RtpStreamReceiver.adjust(keyCode, false);
			return true;
		case KeyEvent.KEYCODE_ENDCALL:
			if (Receiver.pstn_state == null
					|| (Receiver.pstn_state.equals("IDLE") && (SystemClock
							.elapsedRealtime() - Receiver.pstn_time) > 3000)) {
				Receiver.engine(mContext).rejectcall();
				return true;
			}
			break;
		}
		return false;
	}

	static TelephonyManager tm;

	static boolean videoValid() {
		if (Receiver.on_wlan)
			return true;
		if (tm == null)
			tm = (TelephonyManager) Receiver.mContext
			.getSystemService(Context.TELEPHONY_SERVICE);
		if (tm.getNetworkType() < TelephonyManager.NETWORK_TYPE_UMTS)
			return false;
		return true;
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		Log.d("HAO", "PTTCallScreen_onError");

		return true;
	}

	@Override
	public void onClick(View v) {
		// Log.d("HAO", "PTTCallScreen_onClick");

		useFront = !useFront;
		initializeVideo();
		change = true;

	}

	@Override
	public boolean onLongClick(View v) {
		Log.d("SIPDROID", "[PTTCallScreen] - onLongClick");

		videoQualityHigh = !videoQualityHigh;
		initializeVideo();
		change = true;
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuItem m = menu.add(0, AUDIO_REQUEST_MENU_ITEM, 0, R.string.menu_audiosend);
		m.setIcon(android.R.drawable.ic_menu_call);

		m = menu.add(0, AUDIO_RELEASE_MENU_ITEM, 0, R.string.menu_audiorelease);
		m.setIcon(android.R.drawable.ic_menu_call);

		m = menu.add(0, VIDEO_REQUEST_MENU_ITEM, 0, R.string.menu_videosend);
		m.setIcon(android.R.drawable.ic_menu_camera);

		m = menu.add(0, VIDEO_RELEASE_MENU_ITEM, 0, R.string.menu_videorelease);
		m.setIcon(android.R.drawable.ic_menu_camera);

		m = menu.add(0, SPEAKER_MENU_ITEM, 0, R.string.menu_speaker);
		m.setIcon(android.R.drawable.stat_sys_speakerphone);

		m = menu.add(0, HANG_UP_MENU_ITEM, 0, R.string.menu_endCall);
		m.setIcon(R.drawable.ic_menu_end_call);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d("HAO", "PTTCallScreen_onPrepareOptionsMenu");
		if (mMediaRecorderRecording)
			menu.findItem(VIDEO_REQUEST_MENU_ITEM).setVisible(false);

		if (Receiver.mSipdroidEngine != null
				&& Receiver.mSipdroidEngine.ua != null
				&& Receiver.mSipdroidEngine.ua.audio_app != null) {
			menu.findItem(VIDEO_REQUEST_MENU_ITEM).setVisible(
					VideoCamera.videoValid()
					&& Receiver.call_state == UserAgent.UA_STATE_INCALL
					&& Receiver.engine(this).getRemoteVideo() != 0 && !isVideoSending && !VideoMBCPProcess.Status.equals("Receiving"));
			menu.findItem(AUDIO_REQUEST_MENU_ITEM).setVisible(!isAudioSending && !AudioMBCPProcess.Status.equals("Receiving"));
			menu.findItem(HANG_UP_MENU_ITEM).setVisible(true);
		} 

		menu.findItem(SPEAKER_MENU_ITEM).setVisible(!isAudioSending &&!(Receiver.headset > 0 || Receiver.docked > 0 || Receiver.bluetooth > 0));
		menu.findItem(AUDIO_RELEASE_MENU_ITEM).setVisible(isAudioSending && AudioMBCPProcess.Status.equals("Sending"));
		menu.findItem(VIDEO_RELEASE_MENU_ITEM).setVisible(isVideoSending && VideoMBCPProcess.Status.equals("Sending"));
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected");

		switch (item.getItemId()) {

		case SPEAKER_MENU_ITEM:
			Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - SPEAKER_MENU_ITEM");
			Receiver.engine(this).speaker(AudioManager.MODE_IN_CALL);
			break;

		case AUDIO_REQUEST_MENU_ITEM:
			Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - AUDIO_REQUEST_MENU_ITEM");
			audio_mbcp_process.RequestMSG();
			break;

		case AUDIO_RELEASE_MENU_ITEM:
			Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - AUDIO_RELEASE_MENU_ITEM");
			audio_mbcp_process.ReleaseMSG();
			break;


		case VIDEO_REQUEST_MENU_ITEM:
			Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - VIDEO_REQUEST_MENU_ITEM");
			video_mbcp_process.RequestMSG();
			//startVideoRecording();
			//onResume();
			break;

		case VIDEO_RELEASE_MENU_ITEM:
			//intent.removeExtra("justplay");
			video_mbcp_process.ReleaseMSG();

			//			intent.removeExtra("justplay");
			//			onResume();
			break;

		case HANG_UP_MENU_ITEM:
			Receiver.stopRingtone();
			Receiver.engine(this).rejectcall();
			Receiver.xmppEngine().stopConversation();
			Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - HANG_UP_MENU_ITEM");
			finish();
			break;

		}
		return true;
	}
}