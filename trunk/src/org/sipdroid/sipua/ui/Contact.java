package org.sipdroid.sipua.ui;

public class Contact {
	public String mUsername;
	public String mStatus = "";
	public String mImagePath = "";
	public String mEmail  = "";
	public String mPhoneNumber = "";
	
	public Contact(String username, String status, String image, String email, String phoneNumber) {
		this.mUsername = username;
		this.mStatus = status;
		this.mImagePath = image;
		this.mEmail = email;
		this.mPhoneNumber = phoneNumber;
	}
}
