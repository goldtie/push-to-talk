package org.sipdroid.sipua.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.sipdroid.sipua.PresenceAgent;
import org.sipdroid.sipua.PresenceAgentListener;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.XMPPEngine;

import android.content.Intent;
import org.sipdroid.sipua.UserAgentProfile;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class Presence extends ListActivity implements PresenceAgentListener, DialogInterface.OnClickListener{

	String[] mMyStatus = {"Available", "Invisible", "Busy"};
	
	public class MyCustomAdapter extends ArrayAdapter<String>{

		public MyCustomAdapter(Context context, int textViewResourceId,
				String[] objects) {
			super(context, textViewResourceId, objects);
			// TODO Auto-generated constructor stub
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomView(position, convertView, parent);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			return getCustomView(position, convertView, parent);
		}

		public View getCustomView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			//return super.getView(position, convertView, parent);

			LayoutInflater inflater=getLayoutInflater();
			View row=inflater.inflate(R.layout.contacts_my_status_spinner_item_style, parent, false);
			TextView label=(TextView)row.findViewById(R.id.mystatus);
			label.setText(mMyStatus[position]);
			
			ImageView icon=(ImageView)row.findViewById(R.id.icon);

			switch(position)
			{
			case ONLINE_STATUS:
				icon.setImageResource(R.drawable.icon_available);
				break;
			case OFFLINE_STATUS:
				icon.setImageResource(R.drawable.icon_offline);
				break;
			case BUSY_STATUS:
				icon.setImageResource(R.drawable.icon_busy);
				break;
			}
			return row;
		}	
    }
	
	public static final String EMPTY = "";
	public static final String STORAGE_CONTACT_LIST = "mnt/sdcard/download/contact_list.ctl";
	public static final String CAMERA = "camera";
	public static final String FIRE = "fire";

	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFERENCE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int MY_STATUS_MENU_ITEM = CONFERENCE_MENU_ITEM + 1;
	public static final int PREFERENCE_MENU_ITEM = MY_STATUS_MENU_ITEM + 1;
	public static final int SIGNOUT_MENU_ITEM = PREFERENCE_MENU_ITEM + 1;
	
	public static final int ONLINE_STATUS = 0;
	public static final int OFFLINE_STATUS = 1;
	public static final int BUSY_STATUS = 2;
	public static final int FIREON_STATUS = 3;
	public static final int FIREOFF_STATUS = 4;
	public static final int CAMERAON_STATUS = 5;
	public static final int CAMERAOFF_STATUS = 6;
	
	public static final int CALL_ACTION = 0;
	public static final int CHAT_ACTION = 1;
	
	EfficientAdapter contactAdapter;
	Vector<PresenceAgent> lst_PA;

	// pa_presence is used for presence.info ~ winfo.
	PresenceAgent pa_presence;
	PresenceAgent pa;

	SipProvider sip_provider;
	UserAgentProfile user_profile;
	
	Contact mMyProfile;
	
	public static boolean mIsPttService;	//replace Sipdroid.pttService due to Sipdroid activity is gone be removed
	
	boolean isSubscribed = false;
	public static Message lastPublishMsg = null;

	public boolean[] checkFireEvent;

	private String[] statusList;
	
	final Handler mHandler = new Handler();
	
	private static class EfficientAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private Bitmap mIconOffLine;
		private Bitmap mIconBusy;
		private Bitmap mIconFireOn;
		private Bitmap mIconFireOff;
		private Bitmap mIconCameraOn;
		private Bitmap mIconCameraOff;

		public EfficientAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			mIconOffLine = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_offline);
			mIconBusy = BitmapFactory.decodeResource(context.getResources(), 	R.drawable.pre_busy);
			mIconFireOn = BitmapFactory.decodeResource(context.getResources(), 	R.drawable.pre_fireon);
			mIconFireOff = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_fireoff);
			mIconCameraOn = BitmapFactory.decodeResource(context.getResources(),R.drawable.pre_cameraon);
			mIconCameraOff = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_cameraoff);
		}

		public int getCount() {
			return mContactList.size();
		}

		public Object getItem(int position) {
			return mContactList.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.contacts_list_item, null);

				holder = new ViewHolder();
				holder.userName = (TextView) convertView.findViewById(R.id.username);
				holder.userStatus = (TextView) convertView.findViewById(R.id.status);
				holder.avatar = (ImageView) convertView.findViewById(R.id.avatar);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.userName.setText(mContactList.get(position).mUsername);
			Contact contact = mContactList.get(position);
			holder.userStatus.setBackgroundResource(contact.mStatus.length() == 0 ? 0 : R.drawable.status_border);
			holder.userStatus.setText(mContactList.get(position).mStatus);
			
			switch (contact.mPresence) {
			case ONLINE_STATUS:
				Bitmap iconOnLine = BitmapFactory.decodeFile(contact.mImagePath);
				holder.avatar.setImageBitmap(iconOnLine);
				break;
			case OFFLINE_STATUS: 
				if(contact.mUsername.equals(FIRE)) {
					holder.avatar.setImageBitmap(mIconFireOff);
				} else if(contact.mUsername.equals(CAMERA)) {
					holder.avatar.setImageBitmap(mIconCameraOff);
				} else {
					holder.avatar.setImageBitmap(mIconOffLine);
				}
				break;
			case BUSY_STATUS:
				holder.avatar.setImageBitmap(mIconBusy);
				break;
			case FIREON_STATUS:
				holder.avatar.setImageBitmap(mIconFireOn);
				break;
			case CAMERAON_STATUS:
				holder.avatar.setImageBitmap(mIconCameraOn);
				break;
			default:
				break;
			}

			return convertView;
		}

		static class ViewHolder {
			TextView userName;
			TextView userStatus;
			ImageView avatar;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		menu.add(0, CONFERENCE_MENU_ITEM, 0, R.string.menu_conference);
		menu.add(0, MY_STATUS_MENU_ITEM, 0, R.string.menu_mystatus);
		menu.add(0, PREFERENCE_MENU_ITEM, 0, R.string.menu_preference);
		menu.add(0, SIGNOUT_MENU_ITEM, 0, R.string.menu_sign_out);
		return result;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		menu.findItem(CONFERENCE_MENU_ITEM).setVisible(true);
		menu.findItem(MY_STATUS_MENU_ITEM).setVisible(true);
		menu.findItem(PREFERENCE_MENU_ITEM).setVisible(true);
		menu.findItem(SIGNOUT_MENU_ITEM).setVisible(true);

		return result;
	}

	public class time {
		Timer timer;

		public time(int period) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new TimerTaskTest(), 0, period * 10);
		}

		class TimerTaskTest extends TimerTask {
			public void run() {

				if (isSubscribed) {
					// republish here
					rePublish(SipStack.default_expires);
				}
			}
		}
	}
	
	void rePublish(int period) {
		pa.publisher_dialog.rePublish(lastPublishMsg);
	}
	
	private void changeMyStatus() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.change_status)
		.setItems(statusList, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface paramDialogInterface, int whichButton) {
				switch (whichButton) {
				case 0:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "On-Line");
					//mContactList.get(myIndex).mPresence = ONLINE_STATUS;
					break;
				case 1:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Off-Line");
					//mContactList.get(myIndex).mPresence = OFFLINE_STATUS;
					break;
				case 2:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Busy");
					//mContactList.get(myIndex).mPresence = BUSY_STATUS;
					break;
				case 3:
					if (user_profile.username.equals(FIRE)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-On");
						//mContactList.get(myIndex).mPresence = FIREON_STATUS;
					}else if (user_profile.username.equals(CAMERA)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-On");
						//mContactList.get(myIndex).mPresence = CAMERAON_STATUS;
					}
					break;
				case 4:
					if (user_profile.username.equals(FIRE)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-Off");
						//mContactList.get(myIndex).mPresence = FIREOFF_STATUS;
					}else if (user_profile.username.equals(CAMERA)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-Off");
						//mContactList.get(myIndex).mPresence = CAMERAOFF_STATUS;
					}
					break;
				}
				contactAdapter.notifyDataSetChanged();	
			}
		}).show();
	}
	
	public void changeUserStatus(int index, int status) {
		final int tmp1 = index, tmp2 = status;
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mContactList.get(tmp1).mPresence != tmp2){
					mContactList.get(tmp1).mPresence = tmp2;
					contactAdapter.notifyDataSetChanged();
				}
			}
		});
		
	}		

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
//		Intent intent = null;

		switch (item.getItemId()) {
		case CONFERENCE_MENU_ITEM:
			//if (SipDroid.pttService){
			
			Presence.mIsPttService = true;
			
			String target = Settings.getPTT_Username(getBaseContext())+"@"+ Settings.getPTT_Server(getBaseContext());
			Receiver.engine(getBaseContext()).call(target, true);	//automatically start PTTCallScreen activity

			//Intent intent = new Intent(getBaseContext(), org.sipdroid.sipua.ui.PTTCallScreen.class);
			//startActivity(intent);
			Receiver.xmppEngine().startConversation("ptt@conference." + Settings.getXMPP_Service(getBaseContext()), XMPPEngine.CONFERENCE);

			break;
		case PREFERENCE_MENU_ITEM:
			break;

		case SIGNOUT_MENU_ITEM:
			// Exit or Stop the presence service function
			//mContactList.get(myIndex).mPresence = OFFLINE_STATUS;
			presenceFinish();
			
			Receiver.xmppEngine().disconect();
			Receiver.mXMPPEngine = null;
			
			//Sipdroid.on(this,false);
			Receiver.pos(true);
			Receiver.engine(this).halt();
			
			Receiver.mSipdroidEngine = null;
			Receiver.reRegister(0);
			stopService(new Intent(this,RegisterService.class));
			finish();
			
			break;

		case MY_STATUS_MENU_ITEM:
			// Do change status
			changeMyStatus();
			break;
		}

		return result;
	}
	
	void reSubscribe(int period) {
		pa_presence.resubscribe(user_profile.username + "@"
				+ user_profile.realm, period);
		for (int i = 0, j = 0; i < mContactList.size(); i++) {
			if (mContactList.get(i).mUsername == null)
				break;
			lst_PA.get(j).resubscribe(
					mContactList.get(i).mUsername  + "@" + user_profile.realm, period);
			j++;
		}
	}
	
	public void presenceFinish() {
		ObjectOutput out = null;
		try {
			out = new ObjectOutputStream(new FileOutputStream(STORAGE_CONTACT_LIST));
			mContactList.add(mMyProfile);
			for(Contact c : mContactList)
				c.mPresence = OFFLINE_STATUS;
            out.writeObject(mContactList);
            out.close();
		} catch (Exception e) {
			Log.d("SIPDROID", "[Presence]  - onCreate - Exception: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				Log.d("SIPDROID", "[Presence]  - presenceFinish - Exception: " + e.getMessage());
			}
		}
		pa.publish(user_profile.username + "@" + user_profile.realm, 0, "Off Line", "Off-Line");
		
		if (isSubscribed)
			reSubscribe(0);

		isSubscribed = false;
		lst_PA.clear();
	}
	
	public void changeFireEvent(boolean b) {
		for (int i = 0; i < mContactList.size(); i++) {
			checkFireEvent[i] = b;
		}
	}

	public void PresenceInitialize() {
		
		// set timer for republish later !!
		new time(SipStack.default_expires);

		pa_presence.subscribe_w(user_profile.username + "@"
				+ user_profile.realm, SipStack.default_expires * 2); // presence_winfo

		lst_PA = new Vector<PresenceAgent>();
		changeFireEvent(false);

		for (int i = 0, j = 0; i < mContactList.size(); i++) {
			if (mContactList.get(i).mUsername == null)
				break;
			
			PresenceAgent pa_temp = new PresenceAgent(sip_provider, user_profile, this, this);
			pa_temp.subscribe(mContactList.get(i).mUsername + "@" + user_profile.realm,SipStack.default_expires * 2);
			
			lst_PA.add(j, pa_temp);
			j++;
		}
		pa.publish(user_profile.username + "@" + user_profile.realm,
				SipStack.default_expires * 2, "On Line", "log-in");

		isSubscribed = true;
	}

	public static ArrayList<Contact> mContactList = new ArrayList<Contact>();
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle icicle) {
		
		Log.d("SIPDROID", "[Presence]  - onCreate");
		super.onCreate(icicle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.contacts_list);
		getListView().setEmptyView(findViewById(R.id.empty));
		
		sip_provider = Receiver.mSipdroidEngine.sip_providers[0];
		user_profile = Receiver.mSipdroidEngine.user_profiles[0];
		
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(STORAGE_CONTACT_LIST));
			mContactList = (ArrayList<Contact>)in.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				mContactList.clear();
				mContactList.add(new Contact(FIRE, EMPTY));
				mContactList.add(new Contact(CAMERA, EMPTY));
				mContactList.add(new Contact("1001", "I love you", "mnt/sdcard/download/icon_1001.png"));
				mContactList.add(new Contact("1002", "Demo Sipdroid", "mnt/sdcard/download/icon_1002.png"));
				mContactList.add(new Contact("1003", "Soongsil University", "mnt/sdcard/download/icon_1003.png"));
				mContactList.add(new Contact("1004", EMPTY, "mnt/sdcard/download/icon_1004.png"));
				mContactList.add(new Contact("1005", "Ec Ec", "mnt/sdcard/download/icon_1005.png"));
				
				
			    File file = new File(STORAGE_CONTACT_LIST);
			    // Create file if it does not exist
			    file.createNewFile();
			    
			} catch (IOException ex) {
				Log.d("SIPDROID", "[Presence]  - onCreate - Exception: " + ex.getMessage());
			}
		}
		
		
		
		checkFireEvent = new boolean[mContactList.size()];
		
		lst_PA = new Vector<PresenceAgent>();
		pa_presence = new PresenceAgent(sip_provider, user_profile, this, this);
		pa = new PresenceAgent(sip_provider, user_profile, this);
				
		initStatusList();
		
		contactAdapter = new EfficientAdapter(this);
		setListAdapter(contactAdapter);
		
		contactAdapter.notifyDataSetChanged();

		
		Spinner mySpinner = (Spinner)findViewById(R.id.spinner_mystatus);
		MyCustomAdapter adapter = new MyCustomAdapter(this, R.layout.contacts_my_status_spinner_item_style, mMyStatus);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mySpinner.setAdapter(adapter);
        
        mySpinner.setPrompt(user_profile.username + " - Change my status");
        mySpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				switch(pos) {
				case ONLINE_STATUS:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[pos], "On-Line");
					break;
				case OFFLINE_STATUS:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[pos], "Off-Line");
					break;
				case BUSY_STATUS:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[pos], "Busy");
					break;
				}
				
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
		});
		ListView lv = getListView();
		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				// TODO Auto-generated method stub
				
				final Contact con = (Contact)((EfficientAdapter)paramAdapterView.getAdapter()).getItem(paramInt);
				
				new AlertDialog.Builder(Presence.this)
				.setTitle(Settings.getAccountUserName(Presence.this))
				.setItems(new String[] {"Call", "Chat"}, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface paramDialogInterface, int whichButton) {
						// TODO Auto-generated method stub
						
						switch (whichButton) {
						case CALL_ACTION:
							Presence.mIsPttService = false;
							Receiver.engine(getBaseContext()).call(con.mUsername + "@" + PreferenceManager.getDefaultSharedPreferences(Presence.this)
									.getString(Settings.PREF_SERVER, EMPTY), true);							
							break;
						case CHAT_ACTION:
							Presence.mIsPttService = true;
							String target = Settings.getPTT_Username(getBaseContext())+"@"+ Settings.getPTT_Server(getBaseContext());
							Receiver.engine(getBaseContext()).call(target, true);
							
							Receiver.xmppEngine().startConversation(
									con.mUsername +
									"@" + Settings.getXMPP_Service(getBaseContext()), 
									XMPPEngine.PERSONAL);
							break;
						}
						
					}
				})
				.show();
			}
		});
		
		PresenceInitialize();
		Receiver.xmppEngine().setContext(this);
		//need this code to be after initializing presence lst_PA 
		for(Contact contact : mContactList) {
			if(contact.mUsername.equals(user_profile.username)) {
				mMyProfile = contact;
				mMyProfile.mPresence = ONLINE_STATUS;
				mContactList.remove(contact);		//will add later before finishing presence
				break;
			}
		}
		
	}
	
	private void initStatusList() {
		if (Receiver.mSipdroidEngine.user_profiles[0].username.equals(FIRE)){
			statusList = new String[] {"On Line", "Off Line", "Busy", "Fire On","Fire Off"};
		}else if (Receiver.mSipdroidEngine.user_profiles[0].username.equals(CAMERA)){
			statusList = new String[] {"On Line", "Off Line", "Busy", "Camera On", "Canera Off"};
		}else 
			statusList = new String[] {"On Line", "Off Line", "Busy"};
	}

	@Override
	public void onPaNotificationFailure(PresenceAgent pa,
			NameAddress recipient, String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaNotificationRequest(PresenceAgent pa,
			NameAddress recipient, NameAddress notifier, String event,
			String contentType, String body) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaPublicationRequest(PresenceAgent pa,
			NameAddress presentity, NameAddress watcher) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaPublicationSuccess(PresenceAgent pa, NameAddress presentity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaPublicationTerminated(PresenceAgent pa,
			NameAddress presentity, String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaSubscriptionRequest(PresenceAgent pa,
			NameAddress presentity, NameAddress watcher) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaSubscriptionSuccess(PresenceAgent pa, NameAddress presentity) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPaSubscriptionTerminated(PresenceAgent pa,
			NameAddress presentity, String reason) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onClick(DialogInterface dialog, int whichButton) {
		// publish new status here !!
		// -->Then receive notify then status will be change automatic
		
		
	}
}