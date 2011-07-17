package org.sipdroid.sipua.ui.screen;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.sipdroid.sipua.PresenceAgent;
import org.sipdroid.sipua.PresenceAgentListener;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.XMPPEngine;

import android.content.Intent;
import org.sipdroid.sipua.UserAgentProfile;
import org.sipdroid.sipua.component.Contact;
import org.sipdroid.sipua.component.ContactManagement;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.RegisterService;
import org.sipdroid.sipua.ui.Settings;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

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
	public static final int SIGNOUT_MENU_ITEM = CONFERENCE_MENU_ITEM + 1;
	
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
	public static PresenceAgent pa;

	SipProvider sip_provider;
	UserAgentProfile user_profile;
	
	Contact mMyProfile = null;
	
	public static Context mContext;
	
	public static Contact mTarget = null;
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
		private Bitmap mIconFireOnLine;
		private Bitmap mIconCameraOnLine;
		private Bitmap mIconFireOn;
		private Bitmap mIconFireOff;
		private Bitmap mIconCameraOn;
		private Bitmap mIconCameraOff;

		public EfficientAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			mIconOffLine = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_offline);
			mIconBusy = BitmapFactory.decodeResource(context.getResources(), 	R.drawable.pre_busy);
			mIconFireOnLine = BitmapFactory.decodeResource(context.getResources(), 	R.drawable.pre_fire_online);
			mIconFireOn = BitmapFactory.decodeResource(context.getResources(), 	R.drawable.pre_fireon);
			mIconFireOff = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_fireoff);
			mIconCameraOnLine = BitmapFactory.decodeResource(context.getResources(),R.drawable.pre_camera_online);
			mIconCameraOn = BitmapFactory.decodeResource(context.getResources(),R.drawable.pre_cameraon);
			mIconCameraOff = BitmapFactory.decodeResource(context.getResources(), R.drawable.pre_cameraoff);
		}

		public int getCount() {
			return mContactManagement.mContactList.size();
		}

		public Object getItem(int position) {
			return mContactManagement.mContactList.get(position);
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
			holder.userName.setText(mContactManagement.mContactList.get(position).mDisplayName);
			Contact contact = mContactManagement.mContactList.get(position);
			holder.userStatus.setBackgroundResource(contact.mStatus.length() == 0 ? 0 : R.drawable.status_border);
			holder.userStatus.setText(mContactManagement.mContactList.get(position).mStatus);
			
			switch (contact.mPresence) {
			case ONLINE_STATUS:
				
				if(contact.mAvatar == null) {
					contact.mAvatar = BitmapFactory.decodeFile(contact.mImagePath);
				}
				if(contact.mUserName.equals(FIRE)) {
					holder.avatar.setImageBitmap(mIconFireOnLine);
				} else if(contact.mUserName.equals(CAMERA)) {
					holder.avatar.setImageBitmap(mIconCameraOnLine);
				} else {
					holder.avatar.setImageBitmap(contact.mAvatar);
				}
				
				break;
			case OFFLINE_STATUS: 
				if(contact.mUserName.equals(FIRE)) {
					holder.avatar.setImageBitmap(mIconFireOff);
				} else if(contact.mUserName.equals(CAMERA)) {
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

		MenuItem m = menu.add(0, CONFERENCE_MENU_ITEM, 0, R.string.menu_conference);
		m.setIcon(R.drawable.icon_menu_conference);
		
		m = menu.add(0, SIGNOUT_MENU_ITEM, 0, R.string.menu_sign_out);
		m.setIcon(R.drawable.icon_sign_out);
		return result;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean result = super.onPrepareOptionsMenu(menu);

		menu.findItem(CONFERENCE_MENU_ITEM).setVisible(true);
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
	
	public void changeUserStatus(int index, int status) {
		final int tmp1 = index, tmp2 = status;
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (mContactManagement.mContactList.get(tmp1).mPresence != tmp2){
					mContactManagement.mContactList.get(tmp1).mPresence = tmp2;
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
			mTarget = null;
			Presence.mIsPttService = true;
			
			String target = Settings.getPTT_Username(getBaseContext())+"@"+ Settings.getPTT_Server(getBaseContext());
			Receiver.engine(getBaseContext()).call(target, true);	//automatically start PTTCallScreen activity

			//Intent intent = new Intent(getBaseContext(), org.sipdroid.sipua.ui.PTTCallScreen.class);
			//startActivity(intent);
			Receiver.xmppEngine().startConversation("ptt@conference." + Settings.getXMPP_Service(getBaseContext()), XMPPEngine.CONFERENCE);

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

		}

		return result;
	}
	
	void reSubscribe(int period) {
		pa_presence.resubscribe(user_profile.username + "@"
				+ user_profile.realm, period);
		for (int i = 0, j = 0; i < mContactManagement.mContactList.size(); i++) {
			if (mContactManagement.mContactList.get(i).mUserName == null)
				break;
			lst_PA.get(j).resubscribe(
					mContactManagement.mContactList.get(i).mUserName  + "@" + user_profile.realm, period);
			j++;
		}
	}
	
	public void presenceFinish() {
//		ObjectOutput out = null;
//		try {
//			out = new ObjectOutputStream(new FileOutputStream(STORAGE_CONTACT_LIST));
//			//mContactList.add(mMyProfile);
//			for(Contact c : mContactList)
//				c.mPresence = OFFLINE_STATUS;
//            out.writeObject(mContactList);
//            out.close();
//		} catch (Exception e) {
//			Log.d("SIPDROID", "[Presence]  - onCreate - Exception: " + e.getMessage());
//		} finally {
//			try {
//				out.close();
//			} catch (IOException e) {
//				Log.d("SIPDROID", "[Presence]  - presenceFinish - Exception: " + e.getMessage());
//			}
//		}
		pa.publish(user_profile.username + "@" + user_profile.realm, 0, "Off Line", "Off-Line");
		
		if (isSubscribed)
			reSubscribe(0);

		isSubscribed = false;
		lst_PA.clear();
	}
	
	public void changeFireEvent(boolean b) {
		for (int i = 0; i < mContactManagement.mContactList.size(); i++) {
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

		for (int i = 0, j = 0; i < mContactManagement.mContactList.size(); i++) {
			if (mContactManagement.mContactList.get(i).mUserName == null)
				break;
			
			PresenceAgent pa_temp = new PresenceAgent(sip_provider, user_profile, this, this);
			pa_temp.subscribe(mContactManagement.mContactList.get(i).mUserName + "@" + user_profile.realm,SipStack.default_expires * 2);
			
			lst_PA.add(j, pa_temp);
			j++;
		}
		pa.publish(user_profile.username + "@" + user_profile.realm,
				SipStack.default_expires * 2, "On Line", "log-in");

		isSubscribed = true;
	}

	public static ContactManagement mContactManagement = null;
	
	@Override
	public void onCreate(Bundle icicle) {
		
		Log.d("SIPDROID", "[Presence]  - onCreate");
		super.onCreate(icicle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.contacts_list);
		getListView().setEmptyView(findViewById(R.id.empty));
		
		mContext = this;
		sip_provider = Receiver.mSipdroidEngine.sip_providers[0];
		user_profile = Receiver.mSipdroidEngine.user_profiles[0];
		
		mContactManagement = new ContactManagement();
		for(Contact c : mContactManagement.mContactList) {
			if(c.mUserName.equals(user_profile.username)) {
				mMyProfile = c;
				mContactManagement.mContactList.remove(c);
				break;
			}
		}
		checkFireEvent = new boolean[mContactManagement.mContactList.size()];
		
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
        
        mySpinner.setPrompt(mMyProfile.mDisplayName + " - Change my status");
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
				
				mTarget = (Contact)((EfficientAdapter)paramAdapterView.getAdapter()).getItem(paramInt);
				if(mTarget.mDisplayName.equals(FIRE) || mTarget.mDisplayName.equals(CAMERA))
					return;
				((ListActivity)mContext).showDialog(CUSTOM_DIALOG);
				
			}
		});
		
		PresenceInitialize();
		Receiver.xmppEngine().setContext(this);
		
		//need this code to be after initializing presence lst_PA 
		
		
	}
	private static final int CUSTOM_DIALOG 	= 1;
	
	public void onPrepareDialog(int id, Dialog dialog) {
		if(id == CUSTOM_DIALOG) {
			 Bitmap iconOnLine = BitmapFactory.decodeFile(Presence.mTarget.mImagePath);
	         
			 ImageView avatar = (ImageView)dialog.findViewById(R.id.avatar);
	         if(avatar != null)
	        	 avatar.setImageBitmap(iconOnLine);
	         
	         TextView status = (TextView)dialog.findViewById(R.id.summary);
	         if(status != null)
	        	 status.setText(Presence.mTarget.mStatus);
	         
	         TextView message = (TextView)dialog.findViewById(R.id.message);
	         if(message != null) {
	        	 if(Presence.mTarget.mStatus.equals(EMPTY)) {
	        		 status.setVisibility(ViewGroup.GONE);
	        		 message.setGravity(Gravity.CENTER_VERTICAL);
	        	 } else {
	        		 status.setVisibility(ViewGroup.VISIBLE);
	        		 message.setGravity(Gravity.TOP);
	        	 }
	        	 message.setText(Presence.mTarget.mDisplayName);
	         }
	        	 
		}
	}
	
	private Dialog mChatDialog = null;
	public Dialog onCreateDialog(int dialogId) {
    	
    	if(dialogId == CUSTOM_DIALOG) {
    		CustomDialog.Builder customBuilder = new CustomDialog.Builder(this);
			customBuilder.setTitle(mMyProfile.mDisplayName)
				.setNegativeButton("Call", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Presence.mIsPttService = false;
						Receiver.engine(getBaseContext()).call(mTarget.mUserName + "@" + PreferenceManager.getDefaultSharedPreferences(Presence.this)
								.getString(Settings.PREF_SERVER, EMPTY), true);	
						dialog.cancel();
					}
				})
				.setPositiveButton("Chat", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						Presence.mIsPttService = true;
						String target = Settings.getPTT_Username(getBaseContext())+"@"+ Settings.getPTT_Server(getBaseContext());
						Receiver.engine(getBaseContext()).call(target, true);
						
						Receiver.xmppEngine().startConversation(mTarget.mUserName + "@" + Settings.getXMPP_Service(getBaseContext()), 
								XMPPEngine.PERSONAL);
						dialog.cancel();
					}
				});
			mChatDialog = customBuilder.create();
			mChatDialog.getWindow().getAttributes().width = LayoutParams.FILL_PARENT;
			mChatDialog.getWindow().getAttributes().height = LayoutParams.WRAP_CONTENT;
			mChatDialog.getWindow().getAttributes().gravity = Gravity.BOTTOM;

    	}
    	return mChatDialog;
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
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK && mChatDialog != null) {
			if(mChatDialog.isShowing())
				dismissDialog(CUSTOM_DIALOG);
		}
			

		return super.onKeyDown(keyCode, event);
	}
}