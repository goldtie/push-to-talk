package org.sipdroid.sipua.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.sipdroid.sipua.R;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.Filterable;
import android.widget.TextView;

public class ContactsListActivity extends ListActivity implements TextWatcher, TextView.OnEditorActionListener {

	public final static String ITEM_TITLE = "title";
	public final static String ITEM_CAPTION = "caption";
	public static final String EMPTY = "";
	
	private SeparatedListAdapter mAdapter;
	//private SearchEditText mSearchEditText;
	
	
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		setContentView(R.layout.contacts_list);
		getListView().setEmptyView(findViewById(R.id.empty));
		//mSearchEditText = (SearchEditText)findViewById(R.id.search_src_text);
		// create our list and custom adapter
        mAdapter = new SeparatedListAdapter(this);
        setListAdapter(mAdapter);
		
		mAdapter.addNewContact("A", new Contact("Anh", EMPTY, EMPTY, EMPTY, EMPTY));
        mAdapter.addNewContact("A", new Contact("Anh", EMPTY, EMPTY, EMPTY, EMPTY));
        mAdapter.addNewContact("B", new Contact("Binh", EMPTY, EMPTY, EMPTY, EMPTY));
        mAdapter.addNewContact("B", new Contact("Ban", EMPTY, EMPTY, EMPTY, EMPTY));

	}
	
	
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
					status.setText(user.mStatus );
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

	@Override
	public void beforeTextChanged(CharSequence paramCharSequence,
			int paramInt1, int paramInt2, int paramInt3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onTextChanged(CharSequence paramCharSequence, int paramInt1,
			int paramInt2, int paramInt3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTextChanged(Editable paramEditable) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onEditorAction(TextView paramTextView, int paramInt,
			KeyEvent paramKeyEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	
}
