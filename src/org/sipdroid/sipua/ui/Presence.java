package org.sipdroid.sipua.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
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
import android.database.Cursor;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class Presence extends ListActivity implements PresenceAgentListener, DialogInterface.OnClickListener{

	
	public static final String EMPTY = "";
	public static final String STORAGE_CONTACT_LIST = "mnt/sdcard/download/contact_list.ctl";
	public static final String CAMERA = "camera";
	public static final String FIRE = "fire";

	private class ContactItemAdapter extends ArrayAdapter<Contact> {
		private ArrayList<Contact> mContacts;

		public ContactItemAdapter(Context context, int textViewResourceId, ArrayList<Contact> contacts) {
			super(context, textViewResourceId, contacts);
			this.mContacts = contacts;
		}
		
		public void addContact(Contact contact) {
			for(Contact c : mContacts) {
				if(c.mUsername == contact.mUsername) {
					return;
				}
			}
			mContacts.add(contact);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = convertView;
			if (v == null) {
				LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				v = vi.inflate(R.layout.contacts_list_item, null);
			}
			
			Contact user = mContacts.get(position);
			if (user != null) {
				TextView username = (TextView) v.findViewById(R.id.username);
				TextView status = (TextView) v.findViewById(R.id.status);

				if (username != null) {
					username.setText(user.mUsername);
				}

				if(status != null) {
					status.setText(user.mPresence );
				}
			}
			return v;
		}
	}
	
	public class SeparatedListAdapter extends CursorAdapter implements Filterable {

		public final Map<String,Adapter> mSections = new LinkedHashMap<String,Adapter>();
		public final ArrayAdapter<String> headers;
		public final static int TYPE_SECTION_HEADER = 0;
		private Context mContext;
		
		public SeparatedListAdapter(Context context) {
			super(context, null, false);
			headers = new ArrayAdapter<String>(context, R.layout.contacts_list_header);
			mContext = context;
		}
		
		public void addNewContact(String section, Contact contact) {
			
			if(headers.getPosition(section) == -1) {
				headers.add(section);
			}
			if(mSections.get(section) != null) {
				((ContactItemAdapter)mSections.get(section)).addContact(contact);
			} else {
				ArrayList<Contact> contactsList = new ArrayList<Contact>();
				contactsList.add(contact);
				mSections.put(section, new ContactItemAdapter(mContext, android.R.layout.simple_list_item_1, contactsList));
			}
			
			notifyDataSetChanged();
		}

		public Object getItem(int position) {
			for(Object section : this.mSections.keySet()) {
				Adapter adapter = mSections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if(position == 0) return section;
				if(position < size) return adapter.getItem(position - 1);

				// otherwise jump into next section
				position -= size;
			}
			return null;
		}

		public int getCount() {
			// total together all sections, plus one for each section header
			int total = 0;
			for(Adapter adapter : this.mSections.values())
				total += adapter.getCount() + 1;
			return total;
		}

		public int getViewTypeCount() {
			// assume that headers count as one, then total all sections
			int total = 1;
			for(Adapter adapter : this.mSections.values())
				total += adapter.getViewTypeCount();
			return total;
		}

		public int getItemViewType(int position) {
			int type = 1;
			for(Object section : this.mSections.keySet()) {
				Adapter adapter = mSections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if(position == 0) return TYPE_SECTION_HEADER;
				if(position < size) return type + adapter.getItemViewType(position - 1);

				// otherwise jump into next section
				position -= size;
				type += adapter.getViewTypeCount();
			}
			return -1;
		}

		public boolean areAllItemsSelectable() {
			return false;
		}

		public boolean isEnabled(int position) {
			return (getItemViewType(position) != TYPE_SECTION_HEADER);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			int sectionnum = 0;
			for(Object section : this.mSections.keySet()) {
				Adapter adapter = mSections.get(section);
				int size = adapter.getCount() + 1;

				// check if position inside this section
				if(position == 0) return headers.getView(sectionnum, convertView, parent);
				if(position < size) return adapter.getView(position - 1, convertView, parent);

				// otherwise jump into next section
				position -= size;
				sectionnum++;
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View newView(Context paramContext, Cursor paramCursor,
				ViewGroup paramViewGroup) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void bindView(View paramView, Context paramContext,
				Cursor paramCursor) {
			// TODO Auto-generated method stub
			
		}

		

	}
	
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

//	@SuppressWarnings("serial")
//	public static ArrayList<contact> mContactList = new ArrayList<contact>() {{
//		add(new contact("fire","Fire Center"));
//		add(new contact("camera","Camera"));
//		add(new contact("1000","UA : 1000"));
//		add(new contact("1001","UA : 1001"));
//		add(new contact("1002","UA : 1002"));
//		add(new contact("1003","UA : 1003"));
//		add(new contact("1004","UA : 1004"));
//	}};
	
	public boolean[] checkFireEvent;

	private String[] statusList;
	
	final Handler mHandler = new Handler();
	
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
			return mContactList.size();
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
			return mContactList.get(position);
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
			holder.text.setText(mContactList.get(position).mUsername);
			
			switch (mContactList.get(position).mPresence) {
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
					mContactList.get(myIndex).mPresence = ONLINE_STATUS;
					break;
				case 1:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Off-Line");
					mContactList.get(myIndex).mPresence = OFFLINE_STATUS;
					break;
				case 2:
					pa.publish(user_profile.username + "@"
							+ user_profile.realm,
							SipStack.default_expires,
							statusList[whichButton], "Busy");
					mContactList.get(myIndex).mPresence = BUSY_STATUS;
					break;
				case 3:
					if (user_profile.username.equals(FIRE)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-On");
						mContactList.get(myIndex).mPresence = FIREON_STATUS;
					}else if (user_profile.username.equals(CAMERA)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-On");
						mContactList.get(myIndex).mPresence = CAMERAON_STATUS;
					}
					break;
				case 4:
					if (user_profile.username.equals(FIRE)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Fire-Off");
						mContactList.get(myIndex).mPresence = FIREOFF_STATUS;
					}else if (user_profile.username.equals(CAMERA)){
						pa.publish(user_profile.username + "@"
								+ user_profile.realm,
								SipStack.default_expires,
								statusList[whichButton], "Camera-Off");
						mContactList.get(myIndex).mPresence = CAMERAOFF_STATUS;
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
            out.writeObject(mContactList);
            out.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			Log.d("SIPDROID", "[Presence]  - onCreate - Exception: " + e.getMessage());
		} finally {
			try {
				out.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.d("SIPDROID", "[Presence]  - presenceFinish - Exception: " + e.getMessage());
			}
		}
		pa.publish(user_profile.username + "@" + user_profile.realm, 0,
				"Off Line", "Off-Line");
		
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
		setContentView(R.layout.presence);
		
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(new FileInputStream(STORAGE_CONTACT_LIST));
			mContactList = (ArrayList<Contact>)in.readObject();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			try {
				mContactList.add(new Contact(FIRE, EMPTY));
				mContactList.add(new Contact(CAMERA, EMPTY));
			    File file = new File(STORAGE_CONTACT_LIST);

			    // Create file if it does not exist
			    file.createNewFile();
			    
			} catch (IOException ex) {
				Log.d("SIPDROID", "[Presence]  - onCreate - Exception: " + ex.getMessage());
			}
		}
		
		sip_provider = Receiver.mSipdroidEngine.sip_providers[0];
		user_profile = Receiver.mSipdroidEngine.user_profiles[0];
		
		checkFireEvent = new boolean[mContactList.size()];
		
		lst_PA = new Vector<PresenceAgent>();
		// pa_presence is used for presence.info ~ winfo.
		pa_presence = new PresenceAgent(sip_provider, user_profile, this, this);
		pa = new PresenceAgent(sip_provider, user_profile, this);
		
//		for (int i =0; i<mContactList.size();i++) {
//			if (mContactList.get(i).mUsername.equals(user_profile.username)){
//				myIndex = i;
//			}
//		}
		
		//mContactList.get(myIndex).mPresence = ONLINE_STATUS;
				
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
				
				return false;
			}
		});
		PresenceInitialize();
		Receiver.xmppEngine().setContext(this);
		Button btnMySelf = (Button) findViewById(R.id.mystatus);
		btnMySelf.setText(Settings.getAccountUserName(this));
		
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