package org.sipdroid.sipua.ui;

import java.io.Serializable;

import android.graphics.Bitmap;

public class Contact implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String mUserName;
	public String mDisplayName;
	public int mPresence;
	public String mStatus;
	public String mImagePath = "";
	public String mEmail  = "";
	public String mPhoneNumber = "";
	public transient Bitmap mAvatar;
	
	public Contact(String username, String displayname, String status, String imagePath) {
		this.mUserName = username;
		this.mDisplayName = displayname;
		this.mStatus = status;
		this.mImagePath = imagePath;
		mPresence = Presence.OFFLINE_STATUS;
//		this.mEmail = email;
//		this.mPhoneNumber = phoneNumber;
	}
	
	public Contact(String username, String displayname, String status) {
		this.mUserName = username;
		this.mDisplayName = displayname;
		this.mStatus = status;
		mPresence = Presence.OFFLINE_STATUS;
	}
}
