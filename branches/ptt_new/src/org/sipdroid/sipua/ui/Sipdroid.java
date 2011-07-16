/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2008 Hughes Systique Corporation, USA (http://www.hsc.com)
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.sipdroid.sipua.ui;


import net.tomp2p.p2p.Peer;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.SipdroidEngine;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

/////////////////////////////////////////////////////////////////////
// this the main activity of Sipdroid
// for modifying it additional terms according to section 7, GPL apply
// see ADDITIONAL_TERMS.txt
/////////////////////////////////////////////////////////////////////
public class Sipdroid extends Activity implements OnDismissListener, Runnable{

	public static final boolean release = true;
	public static final boolean market = false;
	
	public static String ptt_address = "";
	
	Intent intent;
	
	/* Following the menu item constants which will be used for menu creation */
	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFIGURE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int ABOUT_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int EXIT_MENU_ITEM = FIRST_MENU_ID + 3;

	@Override
	public void onStart() {
		super.onStart();
		Receiver.engine(this).registerMore();
	}
	
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		setContentView(R.layout.sipdroid);
		
		pd = ProgressDialog.show(this, "SIPDROID", "Waiting...", true,
                false);

		Thread thread = new Thread(this);
		thread.start();
		
		on(this,true);
		
//		ImageButton loginBtn = (ImageButton) this.findViewById(R.id.btnLogin);
//		loginBtn.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View paramView) {
//				// TODO Auto-generated method stub
//				login();
//				
//			}
//		});
		
		ptt_address = Settings.getPTT_Server(getBaseContext());
	}

	public static boolean on(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Settings.PREF_ON, Settings.DEFAULT_ON);
	}

	public static void on(Context context,boolean on) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context).edit();
		edit.putBoolean(Settings.PREF_ON, on);
		edit.commit();
        if (on) Receiver.engine(context).isRegistered();
	}

	@Override
	public void onResume() {
		super.onResume();
		
		

	}

	public static String getVersion() {
		return getVersion(Receiver.mContext);
	}
	
	public static String getVersion(Context context) {
		final String unknown = "Unknown";
		
		if (context == null) {
			return unknown;
		}
		
		try {
	    	String ret = context.getPackageManager()
			   .getPackageInfo(context.getPackageName(), 0)
			   .versionName;
	    	if (ret.contains(" + "))
	    		ret = ret.substring(0,ret.indexOf(" + "))+"b";
	    	return ret;
		} catch(NameNotFoundException ex) {}
		
		return unknown;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		onResume();
	}
	
	public void login() {
		if(!Receiver.engine(this).isRegistered()) {
			//Toast.makeText(this, "No suitable network connection available. Please check your configuration!", Toast.LENGTH_LONG).show();
			intent = new Intent(getBaseContext(), org.sipdroid.sipua.ui.Settings.class);
			startActivity(intent);
		} else {
			Receiver.xmppEngine().login(org.sipdroid.sipua.ui.Settings.getAccountUserName(getBaseContext()) + "@" +  
					org.sipdroid.sipua.ui.Settings.getXMPP_Service(getBaseContext()), 
					org.sipdroid.sipua.ui.Settings.getAccountUserName(getBaseContext()));
			
			intent = new Intent(getBaseContext(), org.sipdroid.sipua.ui.MainUIActivity.class);
			startActivity(intent);
			finish();
			
		}
	}
	// <--

	private ProgressDialog pd;
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		handler.sendEmptyMessage(0);
	}
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			login();
			pd.dismiss();
			
		}
	};
}
