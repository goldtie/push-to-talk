package org.sipdroid.sipua.ui;

import java.util.ArrayList;

import org.sipdroid.sipua.MessageStruct;


public class ChatArchiveStruct{

	public String mUserName;		//who I talk with
	public String mImagePath;
	public ArrayList<MessageStruct> mChatArchiveContent;

	public ChatArchiveStruct(String username, ArrayList<MessageStruct> chatArchiveContent) {
		mUserName = username;
		mChatArchiveContent = chatArchiveContent;
		this.mImagePath = new ContactManagement().getContact(mUserName).mImagePath;
	}

}
