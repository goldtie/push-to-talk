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

import org.sipdroid.sipua.R;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;

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
		
		on(this,true);
		
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
		pd = ProgressDialog.show(this, "DCNTalk", "Waiting...", true,
                false);

		Thread thread = new Thread(this);
		thread.start();
		

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
//			if (!PreferenceManager
//					.getDefaultSharedPreferences(Receiver.mContext).getBoolean(
//							Settings.PREF_P2P, Settings.DEFAULT_P2P)) {
//				// here means using Client-Server
//				Receiver.xmppEngine().login(
//						org.sipdroid.sipua.ui.Settings
//								.getAccountUserName(getBaseContext())
//								+ "@"
//								+ org.sipdroid.sipua.ui.Settings
//										.getXMPP_Service(getBaseContext()),
//						org.sipdroid.sipua.ui.Settings
//								.getAccountUserName(getBaseContext()));			
//			}
			intent = new Intent(getBaseContext(), org.sipdroid.sipua.ui.MainUIActivity.class);
			startActivity(intent);
			finish();
			
		}
	}
	// <--

	private ProgressDialog pd;
	
	@Override
	public void run() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
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
