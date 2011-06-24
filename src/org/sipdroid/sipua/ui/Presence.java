package org.sipdroid.sipua.ui;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.jivesoftware.smackx.muc.MultiUserChat;
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
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Presence extends ListActivity implements PresenceAgentListener, DialogInterface.OnClickListener{

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
	
	public static class contact {
		public String mDisplayName;
		public String mName;
		public int mStatus;
		public contact(String name, String display, int status){
			mDisplayName=display;
			mName = name;
			mStatus = status;
		}
		
		public contact(String name, String display){
			mDisplayName=display;
			mName = name;
			mStatus = OFFLINE_STATUS;
		}
	}
	/** Called with the activity is first created. */

	EfficientAdapter contactAdapter;
	Vector<PresenceAgent> lst_PA;

	// pa_presence is used for presence.info ~ winfo.
	PresenceAgent pa_presence;
	PresenceAgent pa;

	SipProvider sip_provider;
	UserAgentProfile user_profile;
	int myIndex = -1;
	
	public static boolean mIsPttService;	//replace Sipdroid.pttService due to Sipdroid activity is gone be removed
	
	// keep current status
	int mCurrentStatus = ONLINE_STATUS;

	boolean isSubscribed = false;
	public static Message lastPublishMsg = null;

	@SuppressWarnings("serial")
	public static ArrayList<contact> contactList = new ArrayList<contact>() {{
		add(new contact("fire","Fire Center"));
		add(new contact("camera","Camera"));
		add(new contact("1000","UA : 1000"));
		add(new contact("1001","UA : 1001"));
		add(new contact("1002","UA : 1002"));
		add(new contact("1003","UA : 1003"));
		add(new contact("1004","UA : 1004"));
	}};
	
	public boolean[] checkFireEvent;

	private String[] statusList;
		
	private static class EfficientAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		private Bitmap mIconOnLine;
		private Bitmap mIconOffLine;
		private Bitmap mIconBusy;
		private Bitmap mIconFireOn;
		private Bitmap mIconFireOff;
		private Bitmap mIconCameraOn;
		private Bitmap mIconCameraOff;

		public EfficientAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);

			// Icons bound to the rows.
			mIconOnLine = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.pre_online);
			mIconOffLine = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.pre_offline);
			mIconBusy = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.pre_busy);
			mIconFireOn = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.pre_fireon);
			mIconFireOff = BitmapFactory.decodeResource(context.getResources(),
					R.drawable.pre_fireoff);
			mIconCameraOn = BitmapFactory.decodeResource(
					context.getResources(), R.drawable.pre_cameraon);
			mIconCameraOff = BitmapFactory.decodeResource(context
					.getResources(), R.drawable.pre_cameraoff);
		}

		/**
		 * The number of items in the list is determined by the number of
		 * speeches in our array.
		 * 
		 * @see android.widget.ListAdapter#getCount()
		 */
		public int getCount() {
			return contactList.size();
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * Sufficient to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 * 
		 * @see android.widget.ListAdapter#getItem(int)
		 */
		public Object getItem(int position) {
			return contactList.get(position);
		}

		/**
		 * Use the array index as a unique id.
		 * 
		 * @see android.widget.ListAdapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 * 
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid
			// unnecessary calls to findViewById() on each row.
			ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is
			// no need
			// to reinflate it. We only inflate a new View when the convertView
			// supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item_icon_text,
						null);

				// Creates a ViewHolder and store references to the two children
				// views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.text.setText(contactList.get(position).mName);
			
			switch (contactList.get(position).mStatus) {
			case ONLINE_STATUS:
				holder.icon.setImageBitmap(mIconOnLine);
				break;
			case OFFLINE_STATUS:
				holder.icon.setImageBitmap(mIconOffLine);
				break;
			case BUSY_STATUS:
				holder.icon.setImageBitmap(mIconBusy);
				break;
			case FIREON_STATUS:
				holder.icon.setImageBitmap(mIconFireOn);
				break;
			case FIREOFF_STATUS:
				holder.icon.setImageBitmap(mIconFireOff);
				break;
			case CAMERAON_STATUS:
				holder.icon.setImageBitmap(mIconCameraOn);
				break;
			case CAMERAOFF_STATUS:
				holder.icon.setImageBitmap(mIconCameraOff);
				break;
			default:
				break;
			}

			return convertView;
		}

		static class ViewHolder {
			TextView text;
			ImageView icon;
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
				// TODO Auto-generated method stub
				switch (whichButton) {
				case 0:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "On-Line");
					contactList.get(myIndex).mStatus = ONLINE_STATUS;
					break;
				case 1:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Off-Line");
					contactList.get(myIndex).mStatus = OFFLINE_STATUS;
					break;
				case 2:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Busy");
					contactList.get(myIndex).mStatus = BUSY_STATUS;
					break;
				case 3:
					if (user_profile.username.equals("fire")){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-On");
						contactList.get(myIndex).mStatus = FIREON_STATUS;
					}else if (user_profile.username.equals("camera")){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-On");
						contactList.get(myIndex).mStatus = CAMERAON_STATUS;
					}
					break;
				case 4:
					if (user_profile.username.equals("fire")){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-Off");
						contactList.get(myIndex).mStatus = FIREOFF_STATUS;
					}else if (user_profile.username.equals("camera")){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-Off");
						contactList.get(myIndex).mStatus = CAMERAOFF_STATUS;
					}
					break;
				}
				contactAdapter.notifyDataSetChanged();	
			}
		}).show();
	}
	
	final Handler mHandler = new Handler();
	public void changeUserStatus(int index, int status) {
		final int tmp1 = index, tmp2 = status;
		mHandler.post(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				if (contactList.get(tmp1).mStatus != tmp2){
					contactList.get(tmp1).mStatus = tmp2;
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
			contactList.get(myIndex).mStatus = OFFLINE_STATUS;
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
		for (int i = 0, j = 0; i < contactList.size(); i++) {
			if (contactList.get(i).mName == null)
				break;
			lst_PA.get(j).resubscribe(
					contactList.get(i).mName  + "@" + user_profile.realm, period);
			j++;
		}
	}
	
	public void presenceFinish() {
		
		pa.publish(user_profile.username + "@" + user_profile.realm, 0,
				"Off Line", "Off-Line");
		
		if (isSubscribed)
			reSubscribe(0);

		isSubscribed = false;
		lst_PA.clear();
	}
	
	public void changeFireEvent(boolean b) {
		for (int i = 0; i < contactList.size(); i++) {
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

		for (int i = 0, j = 0; i < contactList.size(); i++) {
			if (contactList.get(i).mName == null)
				break;
			
			PresenceAgent pa_temp = new PresenceAgent(sip_provider, user_profile, this, this);
			pa_temp.subscribe(contactList.get(i).mName + "@" + user_profile.realm,SipStack.default_expires * 2);
			
			lst_PA.add(j, pa_temp);
			j++;
		}
		pa.publish(user_profile.username + "@" + user_profile.realm,
				SipStack.default_expires * 2, "On Line", "log-in");

		isSubscribed = true;
	}

	@Override
	public void onCreate(Bundle icicle) {
		
		Log.d("SIPDROID", "[Presence]  - onCreate");
		super.onCreate(icicle);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.presence);

		
		sip_provider = Receiver.mSipdroidEngine.sip_providers[0];
		user_profile = Receiver.mSipdroidEngine.user_profiles[0];
		
		checkFireEvent = new boolean[contactList.size()];
		
		lst_PA = new Vector<PresenceAgent>();
		// pa_presence is used for presence.info ~ winfo.
		pa_presence = new PresenceAgent(sip_provider, user_profile, this, this);
		pa = new PresenceAgent(sip_provider, user_profile, this);
		
		for (int i =0; i<contactList.size();i++) {
			if (contactList.get(i).mName.equals(user_profile.username)){
				myIndex = i;
			}
		}
		
		contactList.get(myIndex).mStatus = ONLINE_STATUS;
				
		initStatusList();
		
		contactAdapter = new EfficientAdapter(this);
		setListAdapter(contactAdapter);
		
		contactAdapter.notifyDataSetChanged();
		ListView lv = getListView();
		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				// TODO Auto-generated method stub
				
				final Presence.contact con = (Presence.contact)((EfficientAdapter)paramAdapterView.getAdapter()).getItem(paramInt);
				
				new AlertDialog.Builder(Presence.this)
				.setTitle(Settings.getAccountUserName(Presence.this))
				.setItems(new String[] {"Call", "Chat"}, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface paramDialogInterface, int whichButton) {
						// TODO Auto-generated method stub
						
						switch (whichButton) {
						case CALL_ACTION:
							Presence.mIsPttService = false;
							Receiver.engine(getBaseContext()).call(con.mName + "@" + PreferenceManager.getDefaultSharedPreferences(Presence.this)
									.getString(Settings.PREF_SERVER, ""), true);							
							break;
						case CHAT_ACTION:
							Presence.mIsPttService = true;
							String target = Settings.getPTT_Username(getBaseContext())+"@"+ Settings.getPTT_Server(getBaseContext());
							Receiver.engine(getBaseContext()).call(target, true);
							
							Receiver.xmppEngine().startConversation(
									con.mName +
									"@" + Settings.getXMPP_Service(getBaseContext()), 
									XMPPEngine.PERSONAL);
							break;
						}
						
					}
				})
				.show();
				
				return false;
			}
		});
		PresenceInitialize();
		Receiver.xmppEngine().setContext(this);
		Button btnMySelf = (Button) findViewById(R.id.mystatus);
		btnMySelf.setText(Settings.getAccountUserName(this));
	}

	
	
	private void initStatusList() {
		if (Receiver.mSipdroidEngine.user_profiles[0].username.equals("fire")){
			statusList = new String[] {"On Line", "Off Line", "Busy", "Fire On","Fire Off"};
		}else if (Receiver.mSipdroidEngine.user_profiles[0].username.equals("camera")){
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