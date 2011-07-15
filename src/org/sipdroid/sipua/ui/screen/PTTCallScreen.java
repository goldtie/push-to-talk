package org.sipdroid.sipua.ui.screen;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.sipua.MessageStruct;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.XMPPEngine;
import org.sipdroid.sipua.component.ContactManagement;
import org.sipdroid.sipua.ui.AudioMBCPProcess;
import org.sipdroid.sipua.ui.CallScreen;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;
import org.sipdroid.sipua.ui.Sipdroid;
import org.sipdroid.sipua.ui.SipdroidListener;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class PTTCallScreen extends CallScreen implements SipdroidListener {

	public static final String EMPTY = "";

	ArrayList<MessageStruct> mMessagesList = new ArrayList<MessageStruct>();
	
	public final static String STORAGE_CHAT_HISTORY = "mnt/sdcard/download/chatlog/";

	public Bitmap mTargetAvatar = null;
	
	public class MessageListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		
		
		
		public MessageListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
		}

		public int getCount() {
			return mMessagesList.size();
		}

		public Object getItem(int position) {
			return mMessagesList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public void addMessage(MessageStruct newMessage) {
			mMessagesList.add(newMessage);
            notifyDataSetChanged();
        }
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.chat_message_list_item, null);

				holder = new ViewHolder();
				holder.mMessage = (TextView) convertView.findViewById(R.id.messageContent);
				holder.mIncomingTime = (TextView) convertView.findViewById(R.id.messageComingTime);
				holder.mAvatar = (ImageView) convertView.findViewById(R.id.avatar);
				
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			MessageStruct mess = mMessagesList.get(position);
			holder.mIncomingTime.setText(mess.mMessageIncomingTime);
			holder.mMessage.setText(mess.mMessageContent);
			
			if(mess.mMessageSender.equals(Settings.getAccountUserName(PTTCallScreen.this))) {
				RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				lp2.topMargin = 14; 
				holder.mMessage.setLayoutParams(lp2);
				holder.mMessage.setBackgroundResource(R.drawable.message_sending_border);
				
				RelativeLayout.LayoutParams lp1 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp1.topMargin = 17; 
				lp1.addRule(RelativeLayout.LEFT_OF, holder.mMessage.getId());
				holder.mIncomingTime.setLayoutParams(lp1);
				holder.mIncomingTime.setBackgroundResource(R.drawable.message_sending_time_border);
				
				holder.mAvatar.setVisibility(View.INVISIBLE);
				
			} else {
				RelativeLayout.LayoutParams lp5 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp5.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				holder.mAvatar.setLayoutParams(lp5);
				
				holder.mAvatar.setImageBitmap(BitmapFactory.decodeFile(new ContactManagement().getContact(mess.mMessageSender).mImagePath));
				holder.mAvatar.setVisibility(View.VISIBLE);
				
				RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp3.topMargin = 14; 
				lp3.addRule(RelativeLayout.RIGHT_OF, holder.mAvatar.getId());
				holder.mMessage.setLayoutParams(lp3);
				holder.mMessage.setBackgroundResource(R.drawable.message_receiving_border);
				
				RelativeLayout.LayoutParams lp4 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp4.topMargin = 17; 
				lp4.addRule(RelativeLayout.RIGHT_OF, holder.mMessage.getId());
				holder.mIncomingTime.setLayoutParams(lp4);
				holder.mIncomingTime.setBackgroundResource(R.drawable.message_receiving_time_border);
				
			}
			
			return convertView;
		}

		public boolean areAllItemsEnabled() {
			return false;
		}
		
		public boolean isEnabled(int position) {
			return false;
		}
		
		
		class ViewHolder {
			ImageView mAvatar;
			TextView mMessage;
			TextView mIncomingTime;
		}
	}
	
	private static int UPDATE_RECORD_TIME = 1;

	private static ListView mChatContentList;
	
	private EditText mSendText;

	LocationManager mLocationManager = null;
	
	public static MessageListAdapter mMessageListAdapter;
	
	private Handler mHandler = new MainHandler();

	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int AUDIO_REQUEST_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int AUDIO_RELEASE_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int SPEAKER_MENU_ITEM = FIRST_MENU_ID + 5;
	public static final int HANG_UP_MENU_ITEM = FIRST_MENU_ID + 6;

	public static boolean isAudioSending = false;

	public AudioMBCPProcess audio_mbcp_process = null;

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

			//mRecordingTimeView.setText(text);

			

			// for Audio status and video status 
			//mAudioStatus.setText(strAudio+AudioMBCPProcess.Status);
			
			mHandler.sendEmptyMessageDelayed(UPDATE_RECORD_TIME, 1000);
		}
	};

	private String mDateTime;
	
	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Log.d("SIPDROID", "[PTTCallScreen] - OnCreate");
		super.onCreate(icicle);
		
		Receiver.xmppEngine().setContext(this);
		
		audio_mbcp_process = new AudioMBCPProcess(this);

		android.util.Log.d("HAO", "Sipdroid.audio_mbcp_process.HelloMSG(user_profile.username)");
		audio_mbcp_process.HelloMSG(Receiver.mSipdroidEngine.user_profiles[0].username);

		mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

		// setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setScreenOnFlag();
		setContentView(R.layout.chat_screen);

		mChatContentList = (ListView) this.findViewById(R.id.listMessages);
		mChatContentList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		mChatContentList.setStackFromBottom(true);
		mMessageListAdapter = new MessageListAdapter(mContext);
		//adapter.addMessage(mXMPPEngine.getLastMessage());
		mChatContentList.setAdapter(mMessageListAdapter);
		
		String[] strDays = new String[] { "Sunday", "Monday", "Tuesday", "Wednesday", "Thusday",
		        "Friday", "Saturday" };
		String[] strMonths = new String[] { "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

		Calendar now = new GregorianCalendar();
		mDateTime = strDays[now.get(Calendar.DAY_OF_WEEK) - 1] + ", " + strMonths[now.get(Calendar.MONTH)] + now.get(Calendar.DAY_OF_MONTH) + ", " + now.get(Calendar.YEAR);

		TextView datetimeHeader = (TextView) this.findViewById(R.id.datetime_header);
		datetimeHeader.setText(mDateTime);
		
		mSendText = (EditText) this.findViewById(R.id.txtInputChat);
		Button send = (Button) this.findViewById(R.id.btnSend);

		send.setOnClickListener(new View.OnClickListener() {
			public void onClick(View view) {

				String text = mSendText.getText().toString();
				if(text.equals(EMPTY))
					return;
				mSendText.setText(EMPTY);
				Receiver.onMsgStatus(XMPPEngine.XMPP_STATE_OUTCOMING_MSG, text);
			}
		});

		mTargetAvatar = (Presence.mTarget == null) ? BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_conference) : BitmapFactory.decodeFile(Presence.mTarget.mImagePath);
		
	}

	public static void updateListContent(MessageListAdapter adapter){
		if(mChatContentList != null)
			mChatContentList.setAdapter(adapter);
	}

	@Override
	public void onStart() {
		Log.d("SIPDROID", "[PTTCallScreen] - onStart");
		super.onStart();
		
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d("SIPDROID", "[PTTCallScreen] - onResume");
		if (!Sipdroid.release)
			Log.i("SipUA:", "on resume");
		
		mHandler.sendEmptyMessage(UPDATE_RECORD_TIME);
	}

	@Override
	public void onPause() {
		Log.d("SIPDROID", "[PTTCallScreen] - onPause");
		super.onPause();
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

	boolean isAvailableSprintFFC, useFront = true;

	public void onError(MediaRecorder mr, int what, int extra) {
		Log.d("SIPDROID", "[PTTCallScreen] - void onError");
		if (what == MediaRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
			finish();
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
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuItem m = menu.add(0, AUDIO_REQUEST_MENU_ITEM, 0, R.string.menu_audiosend);
		m.setIcon(android.R.drawable.ic_menu_call);

		m = menu.add(0, AUDIO_RELEASE_MENU_ITEM, 0, R.string.menu_audiorelease);
		m.setIcon(android.R.drawable.ic_menu_call);

		m = menu.add(0, SPEAKER_MENU_ITEM, 0, R.string.menu_speaker);
		m.setIcon(android.R.drawable.stat_sys_speakerphone);

		m = menu.add(0, HANG_UP_MENU_ITEM, 0, R.string.menu_endCall);
		m.setIcon(R.drawable.ic_menu_end_call);

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.d("HAO", "PTTCallScreen_onPrepareOptionsMenu");

		if (Receiver.mSipdroidEngine != null
				&& Receiver.mSipdroidEngine.ua != null
				&& UserAgent.audio_app != null) {
			menu.findItem(AUDIO_REQUEST_MENU_ITEM).setVisible(!isAudioSending && !AudioMBCPProcess.Status.equals("Receiving"));
			menu.findItem(HANG_UP_MENU_ITEM).setVisible(true);
		} 

		menu.findItem(SPEAKER_MENU_ITEM).setVisible(!isAudioSending &&!(Receiver.headset > 0 || Receiver.docked > 0 || Receiver.bluetooth > 0));
		menu.findItem(AUDIO_RELEASE_MENU_ITEM).setVisible(isAudioSending && AudioMBCPProcess.Status.equals("Sending"));
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

		case HANG_UP_MENU_ITEM:
			hangup();
			break;
		}
		return true;
	}
	
	private void hangup() {
		if(mMessagesList.size() > 0) {
			
			try {
				String archiveFileName = STORAGE_CHAT_HISTORY + (Presence.mTarget == null ? "conference.car" : (Presence.mTarget.mUserName + ".car")); 
				File file = new File(archiveFileName);
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
				out.writeObject(mDateTime);
				out.writeObject(mMessagesList);
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		Log.d("SIPDROID", "[PTTCallScreen] - onOptionsItemSelected - HANG_UP_MENU_ITEM");
		Receiver.stopRingtone();
		Receiver.engine(this).rejectcall();
		Receiver.xmppEngine().stopConversation();
		finish();
	}
}