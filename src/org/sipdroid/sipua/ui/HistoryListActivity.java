package org.sipdroid.sipua.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sipdroid.sipua.MessageStruct;
import org.sipdroid.sipua.R;

import android.app.ListActivity;
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
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryListActivity extends ListActivity{

	public static final String EMPTY = "";
	
	private SeparatedListAdapter mAdapter;
	
	
	public static ChatArchiveStruct mTarget = null;
	public static String mDateTime = EMPTY;
	
	public ContactManagement mContactManagement = new ContactManagement();
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.chat_archive_list);
		getListView().setEmptyView(findViewById(R.id.empty));
		//mSearchEditText = (SearchEditText)findViewById(R.id.search_src_text);
		// create our list and custom adapter
        mAdapter = new SeparatedListAdapter(this);
        setListAdapter(mAdapter);

        File[] files = new File(PTTCallScreen.STORAGE_CHAT_HISTORY).listFiles();
        if(files != null) {
        	for(File f : files) {
        		for(Contact c : mContactManagement.mContactList) {

        			String fileName = f.getName().substring(0, f.getName().indexOf(".car"));

        			if(fileName.equals(c.mUserName) || fileName.equals("conference")) {
        				try {
        					ObjectInputStream in = new ObjectInputStream(new FileInputStream(f.getAbsolutePath()));
        					mDateTime = (String) in.readObject();
        					@SuppressWarnings("unchecked")
        					ArrayList<MessageStruct> messageArr = (ArrayList<MessageStruct>)in.readObject();
        					if(messageArr != null) {
        						mAdapter.addNewContact(new ChatArchiveStruct(fileName, messageArr));	//if fileName is conference, its display name is Conference
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
        
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> paramAdapterView,
					View paramView, int paramInt, long paramLong) {
				mTarget = (ChatArchiveStruct)((SeparatedListAdapter)paramAdapterView.getAdapter()).getItem(paramInt);
				Intent intent = new Intent(getBaseContext(), ChatArchiveContentActivity.class);
				startActivity(intent);
				
			}
        	
        });

	}

	
	private class ContactItemAdapter extends ArrayAdapter<ChatArchiveStruct> {
		
		private ArrayList<ChatArchiveStruct> mContacts;
		
		private Context mContext;

		public ContactItemAdapter(Context context, int textViewResourceId, ArrayList<ChatArchiveStruct> contacts) {
			super(context, textViewResourceId, contacts);
			mContext = context;
			this.mContacts = contacts;
		}
		
		public void addContact(ChatArchiveStruct contact) {
			for(ChatArchiveStruct c : mContacts) {
				if(c.mUserName == contact.mUserName) {
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
				v = vi.inflate(R.layout.chat_archive_list_item, null);
			}
			
			ChatArchiveStruct user = mContacts.get(position);
			if (user != null) {
				Contact contact = mContactManagement.getContact(user.mUserName);
				
				TextView username = (TextView) v.findViewById(R.id.username);
				TextView status = (TextView) v.findViewById(R.id.briefChatArchive);
				ImageView avatar = (ImageView) v.findViewById(R.id.avatar);
				if(username != null)
					username.setText(contact == null ? "Conference" : contact.mDisplayName);
				
				//just display the first message in archive
				if(status != null)
					status.setText(mContacts.get(position).mChatArchiveContent.get(0).mMessageContent);
				if(avatar != null) {
					if(contact == null) {
						Bitmap iconOnLine = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.icon_conference);
						avatar.setImageBitmap(iconOnLine);
					} else if(contact.mAvatar == null) {
						contact.mAvatar = BitmapFactory.decodeFile(contact.mImagePath);
						avatar.setImageBitmap(contact.mAvatar);
					}					
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
		
		public void addNewContact(ChatArchiveStruct contact) {
			String str = mContactManagement.getDisplayName(contact.mUserName);
			String section = str.equals(EMPTY) ? "C" : str.substring(0, 1);
			if(headers.getPosition(section) == -1) {
				headers.add(section);
			}
			if(mSections.get(section) != null) {
				((ContactItemAdapter)mSections.get(section)).addContact(contact);
			} else {
				ArrayList<ChatArchiveStruct> contactsList = new ArrayList<ChatArchiveStruct>();
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
			View v = convertView;
			for(Object section : this.mSections.keySet()) {
				Adapter adapter = mSections.get(section);
				int size = adapter.getCount() + 1;
				
				// check if position inside this section
				if(position == 0) {
					LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					v = vi.inflate(R.layout.contacts_list_header, null);
					TextView sectionView = (TextView) v.findViewById(R.id.list_header_title);
					sectionView.setText(section.toString());
					return v;//headers.getView(sectionnum, convertView, parent);
				}
				if(position < size) return adapter.getView(position - 1, convertView, parent);

				// otherwise jump into next section
				position -= size;
				sectionnum++;
			}
			return v;
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
	
}
