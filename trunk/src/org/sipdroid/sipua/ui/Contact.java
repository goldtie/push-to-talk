package org.sipdroid.sipua.ui;

import java.io.Serializable;

public class Contact implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String mUsername;
	
	public int mPresence;
	public String mStatus;
	public String mImagePath = "";
	public String mEmail  = "";
	public String mPhoneNumber = "";
	
	public Contact(String username, String status, String imagePath) {
		this.mUsername = username;
		this.mStatus = status;
		this.mImagePath = imagePath;
		mPresence = Presence.OFFLINE_STATUS;
//		this.mEmail = email;
//		this.mPhoneNumber = phoneNumber;
	}
	
	public Contact(String username, String status) {
		this.mUsername = username;
		this.mStatus = status;
		mPresence = Presence.OFFLINE_STATUS;
	}
}
