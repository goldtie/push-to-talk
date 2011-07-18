package org.sipdroid.sipua.ui.screen;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sipdroid.sipua.MessageStruct;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.component.ChatArchiveStruct;
import org.sipdroid.sipua.component.Contact;
import org.sipdroid.sipua.component.ContactManagement;
import org.sipdroid.sipua.ui.MainUIActivity;
import org.sipdroid.sipua.ui.Settings;
import org.sipdroid.sipua.ui.screen.PTTCallScreen.MessageListAdapter.ViewHolder;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

public class HistoryListActivity extends ListActivity{

	public static final String EMPTY = "";
	
	private MessageListAdapter mAdapter;
	
	
	public static ChatArchiveStruct mTarget = null;
	public static String mDateTime = EMPTY;
	public static boolean bNeedToUpdate = false;
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.chat_archive_list);
		getListView().setEmptyView(findViewById(R.id.empty));
		//mSearchEditText = (SearchEditText)findViewById(R.id.search_src_text);
		// create our list and custom adapter
        mAdapter = new MessageListAdapter(this);
        setListAdapter(mAdapter);
        
        loadArchiveContent();
        bNeedToUpdate = false;
        
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				mTarget = (ChatArchiveStruct)((MessageListAdapter)paramAdapterView.getAdapter()).getItem(paramInt);
				Intent intent = new Intent(getBaseContext(), ChatArchiveContentActivity.class);
				startActivity(intent);
				
			}
        	
        });

	}

	public void loadArchiveContent() {
		mContacts.clear();
		 File[] files = new File(PTTCallScreen.STORAGE_CHAT_HISTORY).listFiles();
	        if(files != null) {
	        	for(File f : files) {
	        		for(Contact c : MainUIActivity.mContactManagement.mContactList) {

	        			String fileName = f.getName().substring(0, f.getName().indexOf(".car"));

	        			if(fileName.equals(c.mUserName) || fileName.equals("conference")) {
	        				try {
	        					ObjectInputStream in = new ObjectInputStream(new FileInputStream(f.getAbsolutePath()));
	        					mDateTime = (String) in.readObject();
	        					@SuppressWarnings("unchecked")
	        					ArrayList<MessageStruct> messageArr = (ArrayList<MessageStruct>)in.readObject();
	        					if(messageArr != null) {
	        						mAdapter.addNewContact(new ChatArchiveStruct(this, fileName, messageArr));	//if fileName is conference, its display name is Conference
	        					}
	        					in.close();
	        				} catch (Exception e) {
	        					Log.d("SIPDROID", "[ContactsListActivity] - OnCreate - Exception: " + e.getMessage());
	        				}
	        				break;
	        			}
	        		}
	        	}
	        }
	        
	}
	
	@Override
	public void onResume() {
		super.onResume();
		if(bNeedToUpdate) {
			loadArchiveContent();
			bNeedToUpdate = false;
		}
	}

	private ArrayList<ChatArchiveStruct> mContacts = new ArrayList<ChatArchiveStruct>();
	
	public class MessageListAdapter extends BaseAdapter {
		private LayoutInflater mInflater;
		
		private Context mContext;
		
		public MessageListAdapter(Context context) {
			mInflater = LayoutInflater.from(context);
			mContext = context;
		}

		public int getCount() {
			return mContacts.size();
		}

		public Object getItem(int position) {
			return mContacts.get(position);
		}

		public long getItemId(int position) {
			return position;
		}

		public void addNewContact(ChatArchiveStruct newMessage) {
			mContacts.add(newMessage);
            notifyDataSetChanged();
        }
		
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.chat_archive_list_item, null);

				holder = new ViewHolder();
				holder.mUserName = (TextView) convertView.findViewById(R.id.username);
				holder.mSummary = (TextView) convertView.findViewById(R.id.briefChatArchive);
				holder.mAvatar = (ImageView) convertView.findViewById(R.id.avatar);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			
			ChatArchiveStruct user = mContacts.get(position);
			if (user != null) {
				holder.mUserName.setText(MainUIActivity.mContactManagement.getDisplayName(user.mUserName));
				
				//just display the first message in archive
				holder.mSummary.setText(mContacts.get(position).mChatArchiveContent.get(0).mMessageContent);
				
				holder.mAvatar.setImageBitmap(MainUIActivity.mContactManagement.getAvatar(user.mUserName));
			}
			
			return convertView;
		}

		public boolean areAllItemsEnabled() {
			return false;
		}
		
		class ViewHolder {
			ImageView mAvatar;
			TextView mUserName;
			TextView mSummary;
		}
	}
	
}
