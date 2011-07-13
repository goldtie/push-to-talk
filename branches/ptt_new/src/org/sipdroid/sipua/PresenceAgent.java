/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package org.sipdroid.sipua;

import org.sipdroid.sipua.ui.Presence;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;
import org.sipdroid.sipua.ui.Sipdroid;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.dialog.NotifierDialog;
import org.zoolu.sip.dialog.NotifierDialogListener;
import org.zoolu.sip.dialog.PublisherDialog;
import org.zoolu.sip.dialog.PublisherDialogListener;
import org.zoolu.sip.dialog.SubscriberDialog;
import org.zoolu.sip.dialog.SubscriberDialogListener;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import android.content.Intent;
import android.os.Handler;

/**
 * Simple Presence Agent (PA). <br/>
 * It allows a user to subscribe for a presentity acting as presence watcher, or
 * respond to subscription requests (accepting or refusiong the incoming
 * watcher's requests) acting as presentity that watchers can subscribe for. <br/>
 * In the latter case, it simply acts as authorization entity for subscription
 * events.
 */
public class PresenceAgent implements
/* SipInterfaceListener, */SubscriberDialogListener, NotifierDialogListener,
		PublisherDialogListener, Runnable {
	/** Event logger. */
	protected Log log;

	/** UserProfile */
	protected UserAgentProfile user_profile;

	/** SipProvider */
	protected SipProvider sip_provider;

	/** SipInterface to message MESSAGE. */
	// protected SipInterface sip_interface;
	/** SubscriberDialog. */
	protected SubscriberDialog subscriber_dialog;

	public PublisherDialog publisher_dialog;

	/** NotifierDialog. */
	protected NotifierDialog notifier_dialog;

	/** Presence listener */
	protected PresenceAgentListener listener;

	String sender_state = "";
	static Presence gua;
	static String old_icon_string = "log-out";

	/** Expiration time. */
	int expire_time;

	/** Renew time. */
	int renew_time;

	/** Whether keep on registering. */
	boolean loop;

	/** Whether the thread is running. */
	boolean is_running;

	/** Whether the thread is a watcher. */
	boolean is_watcher = false;

	String presentity;

	Thread th1 = new Thread(this);
	//Thread th2 = new Thread(this);

	// /////////////////////////////////////////////////////////////////

	/** Constructs a new PresenceAgent. */
	public PresenceAgent(SipProvider sip_provider,
			UserAgentProfile user_profile, PresenceAgentListener listener,
			Presence Gua) {
		if (Gua != null) {
			gua = Gua;

		}
		this.sip_provider = sip_provider;
		this.log = sip_provider.getLog();
		this.listener = listener;
		this.user_profile = user_profile;
		// if no contact_url and/or from_url has been set, create it now
		user_profile.initContactAddress(sip_provider);
		notifier_dialog = new NotifierDialog(sip_provider, this);
		notifier_dialog.listen();
	}

	public PresenceAgent(SipProvider sip_provider,
			UserAgentProfile user_profile, PresenceAgentListener listener) {
		this.sip_provider = sip_provider;
		this.log = sip_provider.getLog();
		this.listener = listener;
		this.user_profile = user_profile;
		// if no contact_url and/or from_url has been set, create it now
		user_profile.initContactAddress(sip_provider);

		notifier_dialog = new NotifierDialog(sip_provider, this);
		notifier_dialog.listen();
	}

	public PresenceAgent() {
	}

	/** Subscribes for a presentity. */
	public void subscribe(String presentity, int expires) { // if
		subscriber_dialog = new SubscriberDialog(sip_provider, "presence",
				null, this);
		if (expires < 0)
			expires = SipStack.default_expires;
		subscriber_dialog.subscribe(presentity, user_profile.from_url,
				user_profile.contact_url, expires);
	}

	// HAO EDIT -->
	public void resubscribe(String presentity, int expires) {
		subscriber_dialog.reSubscribe(presentity, user_profile.from_url,
				user_profile.contact_url, expires);
	}

	// <---

	// ///////////////////09.08.20 add by LHK /////////////////////
	public void subscribe_w(String presentity, int expires) {
		if (subscriber_dialog == null)
			subscriber_dialog = new SubscriberDialog(sip_provider,
					"presence.winfo", null, this);
		if (expires < 0)
			expires = SipStack.default_expires;
		subscriber_dialog.subscribe(presentity, user_profile.from_url,
				user_profile.contact_url, expires);
	}

	public void publish(String presentity, int expires, String state,
			String state_img) {
		if (publisher_dialog == null)
			publisher_dialog = new PublisherDialog(sip_provider, "presence",
					null, this);
		if (expires < 0)
			expires = SipStack.default_expires;
		publisher_dialog.publish(presentity, user_profile.from_url,
				user_profile.contact_url, expires, state, state_img);
	}

	public void publish(Message msg) {
		publisher_dialog.publish(msg);
	}

	// public void republish(String presentity, int expires) {
	// publisher_dialog.publish(presentity, publisher, contact, expires, state,
	// state_img)

	// presentity, user_profile.from_url,user_profile.contact_url, expires,
	// state, state_img);
	// }

	public synchronized void loop_subscribe(String presentity, int expire_time,
			int renew_time) {
		is_watcher = false;
		this.presentity = presentity;
		this.expire_time = expire_time;
		this.renew_time = renew_time;
		loop = true;
		// if (!is_running) (new Thread(this)).start();
		th1.start();
	}

	public void loop_subscribe_w(String presentity, int expire_time,
			int renew_time) {
		is_watcher = true;
		this.presentity = presentity;
		this.expire_time = expire_time;
		this.renew_time = renew_time;
		loop = true;
		// if (!is_running) (new Thread(this)).start();
		th1.start();
	}

	// //////////////////////////////////////////////////////////////////

	/** Notify a watcher of "pending" state. */
	/*
	 * public void pending() { notifier_dialog.pending(); }
	 */

	/** Notify a watcher of "active" state. */
	public void accept() {
		notifier_dialog.accept(user_profile.expires, user_profile.contact_url);
	}

	/** Notify a watcher of "active" state. */
	public void activate() {
		notifier_dialog.activate();
	}

	/** Notify a watcher of "terminate" state. */
	public void terminate() {
		notifier_dialog.terminate();
	}

	/** Notify a watcher. */
	public void notify(String state, int expires, String content_type,
			String body) {
		notifier_dialog.notify(state, expires, content_type, body);
	}

	public void run() {
		// printLog(is_watcher);
		is_running = true;
		try {
			while (loop) {
				if (!is_watcher)
					subscribe(presentity, expire_time);
				else
					subscribe_w(presentity, expire_time);

				Thread.sleep(renew_time * 1000);
			}
		} catch (Exception e) {
			printException(e, LogLevel.HIGH);
		}
		is_running = false;
	}

	/**
	 * When a 2xx successful final response is received for an SUBSCRIBE
	 * transaction.
	 */
	public void onDlgPublicationSuccess(PublisherDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgPublicationSuccess()", LogLevel.MEDIUM);
		listener.onPaPublicationSuccess(this, dialog.getRemoteName());
	}

	/** When a 300-699 response is received for an SUBSCRIBE transaction. */
	public void onDlgPublicationFailure(PublisherDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgPublicationFailure()", LogLevel.MEDIUM);
		listener
				.onPaPublicationTerminated(this, dialog.getRemoteName(), reason);
	}

	/** When SUBSCRIBE transaction expires without a final response. */
	public void onDlgPublishTimeout(PublisherDialog dialog) {
		printLog("onDlgPublishTimeout()", LogLevel.MEDIUM);
		listener.onPaPublicationTerminated(this, dialog.getRemoteName(),
				"Request Timeout");
	}

	/** When the dialog is terminated. */
	public void onDlgPublicationTerminated(PublisherDialog dialog)
	{
		printLog("onDlgPublicationTerminated()", LogLevel.MEDIUM);
		listener.onPaPublicationTerminated(this, dialog.getRemoteName(),
				"Terminated");
	}

	// //////////////////////////////////////////////////////////////

	// ********* Callback functions implementation ****************

	/**
	 * When a 2xx successfull final response is received for an SUBSCRIBE
	 * transaction.
	 */
	public void onDlgSubscriptionSuccess(SubscriberDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgSubscriptionSuccess()", LogLevel.MEDIUM);
		listener.onPaSubscriptionSuccess(this, dialog.getRemoteName());
	}

	/** When a 300-699 response is received for an SUBSCRIBE transaction. */
	public void onDlgSubscriptionFailure(SubscriberDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgSubscriptionFailure()", LogLevel.MEDIUM);
		listener.onPaSubscriptionTerminated(this, dialog.getRemoteName(),
				reason);
	}

	/** When SUBSCRIBE transaction expires without a final response. */
	public void onDlgSubscribeTimeout(SubscriberDialog dialog) {
		printLog("onDlgSubscribeTimeout()", LogLevel.MEDIUM);
		listener.onPaSubscriptionTerminated(this, dialog.getRemoteName(),
				"Request Timeout");
	}

	/** When the dialog is terminated. */
	public void onDlgSubscriptionTerminated(SubscriberDialog dialog) {
		printLog("onDlgSubscriptionTerminated()", LogLevel.MEDIUM);
		listener.onPaSubscriptionTerminated(this, dialog.getRemoteName(),
				"Terminated");
	}
	protected Handler mHandler = new Handler();
	/** When an incoming NOTIFY is received. */
	public void onDlgNotify(SubscriberDialog dialog, NameAddress target,
			NameAddress notifier, NameAddress contact, String state,
			String content_type, String body, Message msg) {
		printLog("onDlgNotify()", LogLevel.MEDIUM);

		listener.onPaNotificationRequest(this, target, notifier, state,
				content_type, body);
		
			int i = 0;
			// JLabel change_icon_label = new JLabel();

			if (content_type != null) {
				if (content_type.equals("application/pidf+xml")) {
					if (body.contains("<dm:note>")) {
						int s = body.lastIndexOf("<dm:note>");
						int e = body.lastIndexOf("</dm:note>");
						sender_state = body.substring(s + 9, e);
					} else {
						sender_state = "";
						System.out.println("***Notify without xml***");
						return;
					}
					String notifier_URI = notifier.toString();
					int s2 = notifier_URI.indexOf(":");
					int e2 = notifier_URI.indexOf("@");
					String get_notifier_URI = notifier_URI
							.substring(s2 + 1, e2);

					for (i = 0; i < gua.mContactList.size(); i++) {
						if (gua.mContactList.get(i).mUsername
								.equals(get_notifier_URI)) {
							if (sender_state.equals("Busy")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i, Presence.BUSY_STATUS);
							} else if (sender_state.equals("On Line")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i, Presence.ONLINE_STATUS);
							} else if (sender_state.equals("Off Line")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i, Presence.OFFLINE_STATUS);
							} else if (sender_state.equals("Fire On")) {
								android.util.Log.d("SIPDROID", "[PresenceAgent] - onDlgNotify - fire event occurs");
								gua.checkFireEvent[i] = true;
								gua.changeUserStatus(i,Presence.FIREON_STATUS);
								if (!Receiver.mSipdroidEngine.isFire()) {
									mHandler.post(new Runnable() {
										public void run() {
											Presence.mIsPttService = true;
											String ptt = Settings.getPTT_Username(Receiver.mContext)+"@"+ Settings.getPTT_Server(Receiver.mContext);
											Receiver.mSipdroidEngine.call(ptt, true);
											Receiver.xmppEngine().startConversation("ptt@conference." + Settings.getXMPP_Service(Receiver.mContext), XMPPEngine.CONFERENCE);
										}
									});
									
								}
							} else if (sender_state.equals("Fire Off")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i, Presence.FIREOFF_STATUS);
							} else if (sender_state.equals("Camera On")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i,Presence.CAMERAON_STATUS);
//								if (Receiver.mSipdroidEngine.isFire()) {
//									gua.presence_panel
//											.changejComboBox_stateList(3);
//								}
							} else if (sender_state.equals("Camera Off")) {
								gua.checkFireEvent[i] = false;
								gua.changeUserStatus(i, Presence.CAMERAOFF_STATUS);
							} else {
								System.out.println("�´°� ����");
							}
							break;
						}
					}
				}
			}
		
	}

	/** When an incoming SUBSCRIBE is received. */
	public void onDlgSubscribe(NotifierDialog dialog, NameAddress target,
			NameAddress subscriber, String event, String id, Message msg) {
		printLog("onDlgSubscribe()", LogLevel.MEDIUM);
		notifier_dialog.pending();
		listener.onPaSubscriptionRequest(this, target, subscriber);
	}

	/** When NOTIFY transaction expires without a final response. */
	public void onDlgNotifyTimeout(NotifierDialog dialog) {
		printLog("onDlgNotifyTimeout()", LogLevel.MEDIUM);
		listener.onPaNotificationFailure(this, dialog.getRemoteName(),
				"Request Timeout");
	}

	/** When a 300-699 response is received for a NOTIFY transaction. */
	public void onDlgNotificationFailure(NotifierDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgNotificationFailure()", LogLevel.MEDIUM);
		listener.onPaNotificationFailure(this, dialog.getRemoteName(), reason);
	}

	/**
	 * When a 2xx successful final response is received for a NOTIFY
	 * transaction.
	 */
	public void onDlgNotificationSuccess(NotifierDialog dialog, int code,
			String reason, Message msg) {
		printLog("onDlgNotificationSuccess()", LogLevel.MEDIUM);
		// do nothing
	}

	// **************************** Logs ****************************/

	/** Adds a new string to the default Log */
	private void printLog(String str) { // printLog(str,LogLevel.HIGH);
		System.out.println("notify: " + str);
	}

	public void printLog(int str) { // printLog(str,LogLevel.HIGH);
		System.out.println("notify: " + str);
	}

	public void printLog(boolean str) { // printLog(str,LogLevel.HIGH);
		System.out.println("notify: " + str);
	}

	/** Adds a new string to the default Log */
	private void printLog(String str, int level) {
		if (log != null)
			log.println("PresenceAgent: " + str, level + SipStack.LOG_LEVEL_UA);
		// System.out.println("PA: "+str);
	}

	/** Adds the Exception message to the default Log */
	void printException(Exception e, int level) {
		if (log != null)
			log.printException(e, level + SipStack.LOG_LEVEL_UA);
	}

}
