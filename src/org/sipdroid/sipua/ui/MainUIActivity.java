package org.sipdroid.sipua.ui;


import org.sipdroid.sipua.R;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class MainUIActivity extends TabActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mainui);
        
        TabHost tabHost = (TabHost)findViewById(android.R.id.tabhost);
        
        
        TabSpec friendTab = tabHost.newTabSpec("friend");
        TabSpec settingTab = tabHost.newTabSpec("setting");
        TabSpec chatTab = tabHost.newTabSpec("chat");
        
        friendTab.setIndicator("Friends", getResources().getDrawable(R.drawable.friends)).setContent(new Intent(this,Presence.class));
        chatTab.setIndicator("Chat", getResources().getDrawable(R.drawable.chat)).setContent(new Intent(this,ContactsListActivity.class));
        settingTab.setIndicator("Settings", getResources().getDrawable(R.drawable.settings)).setContent(new Intent(this,Settings.class));
        
        
        tabHost.addTab(friendTab);
        tabHost.addTab(chatTab);
        tabHost.addTab(settingTab);
        
    }
}
