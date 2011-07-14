package org.sipdroid.sipua;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MessageStruct implements Serializable {
	public String mMessageContent;
	public String mMessageSender;
	public String mMessageIncomingTime;
	
	public MessageStruct() {
		Calendar calendar = new GregorianCalendar();
		mMessageIncomingTime = calendar.get(Calendar.HOUR) 
							+ ":" + calendar.get(Calendar.MINUTE) 
							+ (calendar.get(Calendar.AM_PM) == 0 ? "AM" : "PM");
	}
}