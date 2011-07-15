package org.sipdroid.sipua.component;

import java.util.ArrayList;

import org.sipdroid.sipua.ui.screen.Presence;

public class ContactManagement {
	public static final String EMPTY = "";
	public ArrayList<Contact> mContactList = new ArrayList<Contact>();
	
	public ContactManagement() {
		mContactList.clear();
		mContactList.add(new Contact(Presence.FIRE, Presence.FIRE, EMPTY));
		mContactList.add(new Contact(Presence.CAMERA, Presence.CAMERA, EMPTY));
//		mContactList.add(new Contact("1000", "NKThinh", "잘가요 안녕 내사랑 ", "mnt/sdcard/download/icon_1000.png"));
		mContactList.add(new Contact("1001", "YoungHwa", "날 찾아오신 내님 어서 오세요", "mnt/sdcard/download/icon_1001.png"));
		mContactList.add(new Contact("1002", "TieuTuanHao", EMPTY, "mnt/sdcard/download/icon_1002.png"));
		mContactList.add(new Contact("1003", "KimCheKon", EMPTY, "mnt/sdcard/download/icon_1003.png"));
		mContactList.add(new Contact("1004", "JangJiWon", "어서 오세요 당신의 꽃이 될래요", "mnt/sdcard/download/icon_1004.png"));
		mContactList.add(new Contact("1005", "NgocThanhDinh", EMPTY, "mnt/sdcard/download/icon_1005.png"));
//		mContactList.add(new Contact("1006", "NamYuNoo", "Hyungnam Engineering Build 424", "mnt/sdcard/download/icon_1006.png"));
//		mContactList.add(new Contact("1007", "TuanND", EMPTY, "mnt/sdcard/download/icon_1007.png"));
	}
	
	public String getDisplayName(String username) {
		for(Contact c: mContactList) {
			if(c.mUserName.equals(username)) {
				return c.mDisplayName;
			}
		}
		return EMPTY;
	}
	
	public Contact getContact(String username) {
		for(Contact c : mContactList) {
			if(c.mUserName.equals(username))
				return c;
		}
		return null;
	}
}
