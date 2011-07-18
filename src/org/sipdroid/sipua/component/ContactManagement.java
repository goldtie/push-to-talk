package org.sipdroid.sipua.component;

import java.util.ArrayList;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.screen.Presence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ContactManagement {
	public static final String EMPTY = "";
	public ArrayList<Contact> mContactList = new ArrayList<Contact>();
	
	public Context mContext;
	
	public ContactManagement(Context context) {
		mContactList.clear();
		mContactList.add(new Contact(Presence.FIRE, "Fire Center", EMPTY));
		mContactList.add(new Contact(Presence.CAMERA, "Camera", EMPTY));
		mContactList.add(new Contact("1000", "NKThinh", "잘가요 안녕 내사랑 ", "mnt/sdcard/download/icon_1000.png"));
		mContactList.add(new Contact("1001", "Younghwa", "날 찾아오신 내님 어서 오세요", "mnt/sdcard/download/icon_1001.png"));
		mContactList.add(new Contact("1002", "JangJiWon", EMPTY, "mnt/sdcard/download/icon_1002.png"));
		mContactList.add(new Contact("1003", "KimCheKon", EMPTY, "mnt/sdcard/download/icon_1003.png"));
		mContactList.add(new Contact("1004", "TieuTuanHao", "어서 오세요 당신의 꽃이 될래요", "mnt/sdcard/download/icon_1004.png"));
		mContactList.add(new Contact("1005", "DinhNgocThanh", EMPTY, "mnt/sdcard/download/icon_1005.png"));
		mContext = context;
//		mContactList.add(new Contact("1006", "NamYuNoo", "Hyungnam Engineering Build 424", "mnt/sdcard/download/icon_1006.png"));
//		mContactList.add(new Contact("1007", "TuanND", EMPTY, "mnt/sdcard/download/icon_1007.png"));
	}
	
	public String getDisplayName(String username) {
		for(Contact c: mContactList) {
			if(c.mUserName.equals(username)) {
				return c.mDisplayName;
			}
		}
		return "Conference";
	}
	
	public Contact getContact(String username) {
		for(Contact c : mContactList) {
			if(c.mUserName.equals(username))
				return c;
		}
		return new Contact(Presence.CONFERENCE, "Conference", EMPTY);
	}
	
	public Contact getContact(int index) {
		return mContactList.get(index);
	}
	
	public Bitmap getAvatar(String username) {
		if(username.equals(Presence.FIRE)) {
			return BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pre_fire_online);
		} else if(username.equals(Presence.CAMERA)) {
			return BitmapFactory.decodeResource(mContext.getResources(), R.drawable.pre_camera_online);
		} else if(username.equals(Presence.CONFERENCE)) {
			return BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_conference);
		}
		return BitmapFactory.decodeFile(getContact(username).mImagePath);
	}
}
