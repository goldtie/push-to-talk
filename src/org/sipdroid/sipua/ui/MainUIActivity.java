package org.sipdroid.sipua.ui;


import org.sipdroid.sipua.R;
import org.sipdroid.sipua.component.Contact;
import org.sipdroid.sipua.component.ContactManagement;
import org.sipdroid.sipua.ui.screen.HistoryListActivity;
import org.sipdroid.sipua.ui.screen.Presence;

import android.app.TabActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.TabHost.TabSpec;

public class MainUIActivity extends TabActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.mainui);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_window_title);
        
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        
        TabSpec friendTab = tabHost.newTabSpec("friend");
        TabSpec settingTab = tabHost.newTabSpec("setting");
        TabSpec chatTab = tabHost.newTabSpec("chat");
        
        friendTab.setIndicator("Friends", getResources().getDrawable(R.drawable.friends)).setContent(new Intent(this,Presence.class));
        chatTab.setIndicator("Chat", getResources().getDrawable(R.drawable.chat)).setContent(new Intent(this,HistoryListActivity.class));
        settingTab.setIndicator("Settings", getResources().getDrawable(R.drawable.settings)).setContent(new Intent(this,Settings.class));
        
        
        tabHost.addTab(friendTab);
        tabHost.addTab(chatTab);
        tabHost.addTab(settingTab);
        
        Contact c = new ContactManagement().getContact(org.sipdroid.sipua.ui.Settings.getAccountUserName(getBaseContext()));
        Bitmap mTargetAvatar = BitmapFactory.decodeFile(c.mImagePath);
        
		TextView tvTarget = (TextView) findViewById(R.id.window_title);
		
		tvTarget.setText(c == null ? "Conference" : c.mDisplayName);
		
		ImageView ivTarget = (ImageView) findViewById(R.id.window_icon);
		ivTarget.setImageBitmap(mTargetAvatar);
        
        
    }
}
