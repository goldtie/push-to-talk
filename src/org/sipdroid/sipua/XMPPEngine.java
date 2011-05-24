package org.sipdroid.sipua;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.muc.DiscussionHistory;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

public class XMPPEngine {
	
	public static final int IDLE = -1;
	public static final int CONFERENCE = 0;
	public static final int PERSONAL = 1;
	
	protected XMPPConnection xmppConn = null;
	
	private MultiUserChat mMUC = null;
	private Chat mChat = null;
	
	protected List<String> messages = new ArrayList<String>();
	
	protected Handler mHandler = new Handler();
	
	private String mUsername, mPassword;
	
	private int mCurrentConversation = IDLE;	//0:conference, 1: personal
	
	private Context mContext;
	
	private MessageListener mChatListener = new MessageListener() {
		
		@Override
		public void processMessage(Chat arg0, Message message) {
			// TODO Auto-generated method stub
			//just make sure that the further process is not happened to increase the performance
			if(mCurrentConversation == CONFERENCE)		
				return;
			if (message.getBody() != null) {
				String fromName = StringUtils.parseBareAddress(message.getFrom());
				if(fromName.equals(Settings.getAccountUserName(mContext) + "@" + Settings.getXMPP_Service(mContext))) {
					return;
				}
				Log.i("XMPPEngine", "Got text [" + message.getBody() + "] from [" + fromName + "]");
				messages.add(fromName.substring(0, fromName.indexOf("@")) + ":");
				messages.add(message.getBody());
				// Add the incoming message to the list view
				mHandler.post(new Runnable() {
					public void run() {
						Receiver.onMsgStatus(XMPP_STATE_INCOMING_MSG, null);
					}
				});
			}
		}
	};
	
	private PacketListener mConferenceListener = new PacketListener() {
		
		@Override
		public void processPacket(Packet arg0) {
			try {
			// TODO Auto-generated method stub
			if(mCurrentConversation == PERSONAL)
				return;
			Message message = (Message) arg0;
			if (message.getBody() != null) {
				String fromName = StringUtils.parseResource(message.getFrom());		//get the sender address
				if(fromName.equals(Settings.getAccountUserName(mContext) + "@" + Settings.getXMPP_Service(mContext))) {
					return;
				}
				Log.i("XMPPEngine", "Got text [" + message.getBody() + "] from [" + fromName + "]");
				messages.add(fromName + ":");
				messages.add(message.getBody());
				// Add the incoming message to the list view
				mHandler.post(new Runnable() {
					public void run() {
						Receiver.onMsgStatus(XMPP_STATE_INCOMING_MSG, null);
					}
				});
			}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	};
	
	//Just temporarily, not good solution
	public static final int XMPP_STATE_INCOMING_MSG = -1;
	public static final int XMPP_STATE_OUTCOMING_MSG = -2;
	
	public XMPPEngine(String host, int port, String service) {
		ConnectionConfiguration connConfig = new ConnectionConfiguration(host, 
				port, service);
		if(xmppConn == null)
			xmppConn = new XMPPConnection(connConfig);
	}
	
	public void connect() {
		try {
			xmppConn.connect();
            Log.i("ptt", "[XMPPEngine] Connected to " + xmppConn.getHost());
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            xmppConn.addPacketListener(new PacketListener() {
            	public void processPacket(Packet packet) {
            		final Message message = (Message) packet;
            		if(mCurrentConversation == IDLE) {

            			
            			if (message.getBody() != null) {
            				mHandler.post(new Runnable() {

            					@Override
            					public void run() {
            						String fromName = StringUtils.parseBareAddress(message.getFrom());
            						// TODO Auto-generated method stub
            						Toast.makeText(mContext, fromName + " wants to talk something with you!", Toast.LENGTH_LONG).show();
            					}
            				});

            			}
            		} else if(mCurrentConversation == CONFERENCE) {
            			try {
            				// TODO Auto-generated method stub
            				if(mCurrentConversation == PERSONAL)
            					return;
            				if (message.getBody() != null) {
            					String fromName = StringUtils.parseResource(message.getFrom());		//get the sender address
            					if(fromName.equals(Settings.getAccountUserName(mContext) + "@" + Settings.getXMPP_Service(mContext))) {
            						return;
            					}
            					Log.i("XMPPEngine", "Got text [" + message.getBody() + "] from [" + fromName + "]");
            					messages.add(fromName.substring(0, fromName.indexOf("@")) + ":");
            					messages.add(message.getBody());
            					// Add the incoming message to the list view
            					mHandler.post(new Runnable() {
            						public void run() {
            							Receiver.onMsgStatus(XMPP_STATE_INCOMING_MSG, null);
            						}
            					});
            				}
            				} catch (Exception ex) {
            					ex.printStackTrace();
            				}
            		} else if(mCurrentConversation == PERSONAL) {
            			
            			if (message.getBody() != null) {
            				String fromName = StringUtils.parseBareAddress(message.getFrom());
            				if(fromName.equals(Settings.getAccountUserName(mContext) + "@" + Settings.getXMPP_Service(mContext))) {
            					return;
            				}
            				Log.i("XMPPEngine", "Got text [" + message.getBody() + "] from [" + fromName + "]");
            				messages.add(fromName.substring(0, fromName.indexOf("@")) + ":");
            				messages.add(message.getBody());
            				// Add the incoming message to the list view
            				mHandler.post(new Runnable() {
            					public void run() {
            						Receiver.onMsgStatus(XMPP_STATE_INCOMING_MSG, null);
            					}
            				});
            			}
            		}
            	}
            }, filter);
		} catch (XMPPException ex) {
			Log.e("ptt", "[XMPPEngine] Failed to connect to " + xmppConn.getHost());
			Log.e("ptt", ex.toString());
		}
	}

	public void disconect(){
		xmppConn.disconnect();
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public List<String> getChatLog() {
		return messages;
	}
	
	public XMPPConnection getXMPPConnection() {
		return xmppConn;
	}

	public void login(String username, String password) {
		try {

			Roster roster = xmppConn.getRoster();
			if(roster != null)	//available => no need to login again
				return;
			
			mUsername = username;
			mPassword = password;
			
			xmppConn.login(username, password);
			Log.i("XMPPEngine", "Logged in as " + xmppConn.getUser());
			// Set the status to available
			Presence presence = new Presence(Presence.Type.available);
			xmppConn.sendPacket(presence);
		} catch (XMPPException ex) {
			Log.e("XMPPEngine", ex.toString());
		}

	}
	
	public void sendMessage(String message) {
		try {
			if (message.length() > 0) {
				messages.add(Settings.getAccountUserName(mContext)+ ":");
				messages.add(message);
			}
			
			if(mCurrentConversation == CONFERENCE) {
				mMUC.sendMessage(message);
			} else {
				mChat.sendMessage(message);
			}
		} catch(XMPPException ex) {
			Log.d("XMPPEngine", ex.getMessage());
		}
		
	}
	
	public void stopConversation() {
		if(mCurrentConversation == CONFERENCE) {
			mMUC.leave();
			mMUC.removeMessageListener(mConferenceListener);
			mMUC = null;
		}
		if(mCurrentConversation == PERSONAL)
		{
			mChat.removeMessageListener(mChatListener);
			mChat = null;
		}
		mCurrentConversation = IDLE;
	}

	public void startConversation(String targetName, int type){
		mCurrentConversation = type;
		if(type == CONFERENCE) {	//conference
			if(mMUC == null) {
				mMUC = new MultiUserChat(xmppConn, targetName);
				try {
					mMUC.addMessageListener(mConferenceListener);
					DiscussionHistory history = new DiscussionHistory();
					history.setMaxStanzas(0);
					
					mMUC.join(mUsername, "", history, SmackConfiguration.getPacketReplyTimeout());
					
				} catch (XMPPException e) {
					// TODO Auto-generated catch block
					Log.d("XMPPEngine", e.getMessage());
				}
			}
		} else if(type == PERSONAL) {
			mChat = xmppConn.getChatManager().createChat(targetName, mChatListener);
		}
		
		
	}
	
	
}