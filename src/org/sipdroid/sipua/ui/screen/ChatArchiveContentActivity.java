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

import java.util.ArrayList;

import org.sipdroid.sipua.MessageStruct;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.component.Contact;
import org.sipdroid.sipua.component.ContactManagement;
import org.sipdroid.sipua.ui.MainUIActivity;
import org.sipdroid.sipua.ui.Settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

public class ChatArchiveContentActivity extends Activity{

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
				holder.mFriendName = (TextView) convertView.findViewById(R.id.friendName);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			MessageStruct mess = mMessagesList.get(position);
			holder.mIncomingTime.setText(mess.mMessageIncomingTime);
			holder.mMessage.setText(mess.mMessageContent);
			
			if(mess.mMessageSender.equals(Settings.getAccountUserName(ChatArchiveContentActivity.this))) {
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
				holder.mFriendName.setVisibility(View.GONE);
			} else {
				
				RelativeLayout.LayoutParams lp5 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp5.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				lp5.topMargin = 10; 
				holder.mAvatar.setLayoutParams(lp5);
				
				RelativeLayout.LayoutParams lp6 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp6.topMargin = 10;
				lp6.addRule(RelativeLayout.RIGHT_OF, holder.mAvatar.getId());
				holder.mFriendName.setLayoutParams(lp6);
				
				holder.mAvatar.setImageBitmap(MainUIActivity.mContactManagement.getAvatar(mess.mMessageSender));
				holder.mAvatar.setVisibility(View.VISIBLE);
				holder.mFriendName.setText(MainUIActivity.mContactManagement.getDisplayName(mess.mMessageSender));
				holder.mFriendName.setVisibility(View.VISIBLE);
				
				RelativeLayout.LayoutParams lp3 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp3.topMargin = 44; 
				lp3.addRule(RelativeLayout.RIGHT_OF, holder.mAvatar.getId());
				holder.mMessage.setLayoutParams(lp3);
				holder.mMessage.setBackgroundResource(R.drawable.message_receiving_border);
				
				RelativeLayout.LayoutParams lp4 = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				lp4.topMargin = 47; 
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
			TextView mMessage;
			TextView mFriendName;
			TextView mIncomingTime;
			ImageView mAvatar;
		}
	}
	
	private static ListView mChatContentList;
	
	public static MessageListAdapter mMessageListAdapter;
	
	/** Called with the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		Log.d("SIPDROID", "[PTTCallScreen] - OnCreate");
		super.onCreate(icicle);

		// setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
		 requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		 setContentView(R.layout.chat_archive_content_screen);
	        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_window_title);
		
		

		// don't set mSurfaceHolder here. We have it set ONLY within
		// surfaceCreated / surfaceDestroyed, other parts of the code
		// assume that when it is set, the surface is also set.
		mMessagesList = HistoryListActivity.mTarget.mChatArchiveContent;
		
		mChatContentList = (ListView) this.findViewById(R.id.listArchiveMessages);
		mChatContentList.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
		mChatContentList.setStackFromBottom(true);
		mMessageListAdapter = new MessageListAdapter(this);
		mChatContentList.setAdapter(mMessageListAdapter);
//		LayoutInflater inflater = getLayoutInflater();
//		ViewGroup header = (ViewGroup)inflater.inflate(R.layout.chat_screen_list_header, mChatContentList, false);
//		mChatContentList.addHeaderView(header, null, false);
		
		TextView datetimeHeader = (TextView) this.findViewById(R.id.datetime_header);
		datetimeHeader.setText(HistoryListActivity.mDateTime);
		
		mTargetAvatar = BitmapFactory.decodeFile(HistoryListActivity.mTarget.mImagePath);
		TextView tvTarget = (TextView) findViewById(R.id.window_title);
		
		tvTarget.setText(MainUIActivity.mContactManagement.getDisplayName(HistoryListActivity.mTarget.mUserName));
		
		ImageView ivTarget = (ImageView) findViewById(R.id.window_icon);
		ivTarget.setImageBitmap(mTargetAvatar == null ? 
				BitmapFactory.decodeResource(this.getResources(), R.drawable.icon_conference) : mTargetAvatar);
        TextView appTarget = (TextView) findViewById(R.id.appName);
		appTarget.setText("DCNTalk - " + MainUIActivity.mContactManagement.getDisplayName(Settings.getAccountUserName(getBaseContext())));
		
	}

}