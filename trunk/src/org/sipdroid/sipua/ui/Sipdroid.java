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
import net.tomp2p.peers.Number160;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.SipdroidEngine;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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
public class Sipdroid extends Activity implements OnDismissListener {

	public static final boolean release = true;
	public static final boolean market = false;

	public static String ptt_address = "";

	private Peer peer;

	Intent intent;

	/* Following the menu item constants which will be used for menu creation */
	public static final int FIRST_MENU_ID = Menu.FIRST;
	public static final int CONFIGURE_MENU_ITEM = FIRST_MENU_ID + 1;
	public static final int ABOUT_MENU_ITEM = FIRST_MENU_ID + 2;
	public static final int EXIT_MENU_ITEM = FIRST_MENU_ID + 3;

	private static AlertDialog m_AlertDlg;

	@Override
	public void onStart() {
		super.onStart();
		Receiver.engine(this).registerMore();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.sipdroid);

		on(this, true);

		ImageButton loginBtn = (ImageButton) this.findViewById(R.id.btnLogin);
		loginBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View paramView) {
				login();
			}
		});

		final Context mContext = this;

		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
				Settings.PREF_NOPORT, Settings.DEFAULT_NOPORT)) {
			boolean ask = false;
			for (int i = 0; i < SipdroidEngine.LINES; i++) {
				String j = (i != 0 ? "" + i : "");
				if (PreferenceManager.getDefaultSharedPreferences(this)
						.getString(Settings.PREF_USERNAME + j,
								Settings.DEFAULT_USERNAME).length() != 0
						&& PreferenceManager.getDefaultSharedPreferences(this)
								.getString(Settings.PREF_PORT + j,
										Settings.DEFAULT_PORT).equals(
										Settings.DEFAULT_PORT))
					ask = true;
			}
			if (ask)
				new AlertDialog.Builder(this).setMessage(R.string.dialog_port)
						.setPositiveButton(R.string.yes,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										Editor edit = PreferenceManager
												.getDefaultSharedPreferences(
														mContext).edit();
										for (int i = 0; i < SipdroidEngine.LINES; i++) {
											String j = (i != 0 ? "" + i : "");
											if (PreferenceManager
													.getDefaultSharedPreferences(
															mContext)
													.getString(
															Settings.PREF_USERNAME
																	+ j,
															Settings.DEFAULT_USERNAME)
													.length() != 0
													&& PreferenceManager
															.getDefaultSharedPreferences(
																	mContext)
															.getString(
																	Settings.PREF_PORT
																			+ j,
																	Settings.DEFAULT_PORT)
															.equals(
																	Settings.DEFAULT_PORT))
												edit.putString(
														Settings.PREF_PORT + j,
														"5061");
										}
										edit.commit();
										Receiver.engine(mContext).halt();
										Receiver.engine(mContext).StartEngine();
									}
								}).setNeutralButton(R.string.no,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {

									}
								}).setNegativeButton(R.string.dontask,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										Editor edit = PreferenceManager
												.getDefaultSharedPreferences(
														mContext).edit();
										edit.putBoolean(Settings.PREF_NOPORT,
												true);
										edit.commit();
									}
								}).show();
		} else if (PreferenceManager.getDefaultSharedPreferences(this)
				.getString(Settings.PREF_PREF, Settings.DEFAULT_PREF).equals(
						Settings.VAL_PREF_PSTN)
				&& !PreferenceManager.getDefaultSharedPreferences(this)
						.getBoolean(Settings.PREF_NODEFAULT,
								Settings.DEFAULT_NODEFAULT))
			new AlertDialog.Builder(this).setMessage(R.string.dialog_default)
					.setPositiveButton(R.string.yes,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Editor edit = PreferenceManager
											.getDefaultSharedPreferences(
													mContext).edit();
									edit.putString(Settings.PREF_PREF,
											Settings.VAL_PREF_SIP);
									edit.commit();
								}
							}).setNeutralButton(R.string.no,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).setNegativeButton(R.string.dontask,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Editor edit = PreferenceManager
											.getDefaultSharedPreferences(
													mContext).edit();
									edit.putBoolean(Settings.PREF_NODEFAULT,
											true);
									edit.commit();
								}
							}).show();

		ptt_address = Settings.getPTT_Server(getBaseContext());

	}

	public static boolean on(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getBoolean(Settings.PREF_ON, Settings.DEFAULT_ON);
	}

	public static void on(Context context, boolean on) {
		Editor edit = PreferenceManager.getDefaultSharedPreferences(context)
				.edit();
		edit.putBoolean(Settings.PREF_ON, on);
		edit.commit();
		if (on)
			Receiver.engine(context).isRegistered();
	}

	@Override
	public void onResume() {
		super.onResume();
		// if (Receiver.call_state != UserAgent.UA_STATE_IDLE)
		// Receiver.moveTop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);

		MenuItem m = menu.add(0, ABOUT_MENU_ITEM, 0, R.string.menu_about);
		m.setIcon(android.R.drawable.ic_menu_info_details);
		m = menu.add(0, EXIT_MENU_ITEM, 0, R.string.menu_exit);
		m.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		m = menu.add(0, CONFIGURE_MENU_ITEM, 0, R.string.menu_settings);
		m.setIcon(android.R.drawable.ic_menu_preferences);

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = super.onOptionsItemSelected(item);
		Intent intent = null;

		switch (item.getItemId()) {
		case ABOUT_MENU_ITEM:
			if (m_AlertDlg != null) {
				m_AlertDlg.cancel();
			}
			m_AlertDlg = new AlertDialog.Builder(this).setMessage(
					getString(R.string.about).replace("\\n", "\n").replace(
							"${VERSION}", getVersion(this))).setTitle(
					getString(R.string.menu_about)).setIcon(R.drawable.icon22)
					.setCancelable(true).show();
			break;

		case EXIT_MENU_ITEM:
			on(this, false);
			Receiver.pos(true);
			Receiver.engine(this).halt();
			if (Receiver.mXMPPEngine != null) {
				Receiver.xmppEngine().disconect();
				Receiver.mXMPPEngine = null;
			}
			Receiver.mSipdroidEngine = null;
			Receiver.reRegister(0);
			stopService(new Intent(this, RegisterService.class));
			finish();
			break;

		case CONFIGURE_MENU_ITEM: {
			try {
				intent = new Intent(this, org.sipdroid.sipua.ui.Settings.class);
				startActivity(intent);
			} catch (ActivityNotFoundException e) {
			}
		}
			break;
		}
		return result;
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
			String ret = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0).versionName;
			if (ret.contains(" + "))
				ret = ret.substring(0, ret.indexOf(" + ")) + "b";
			return ret;
		} catch (NameNotFoundException ex) {
		}

		return unknown;
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		onResume();
	}

	public void login() {
		if (!Receiver.engine(this).isRegistered()) {
			// Toast.makeText(this,
			// "No suitable network connection available. Please check your configuration!",
			// Toast.LENGTH_LONG).show();
			intent = new Intent(getBaseContext(),
					org.sipdroid.sipua.ui.Settings.class);
			startActivity(intent);
		} else {
			if (PreferenceManager
					.getDefaultSharedPreferences(Receiver.mContext).getBoolean(
							Settings.PREF_P2P, Settings.DEFAULT_P2P)) {
				// here means using P2P

				intent = new Intent(getBaseContext(),
						org.sipdroid.sipua.ui.Settings.class);
				startActivity(intent);

				// Receiver.tomP2PEngine().start();

			} else {
				// here means using Client-Server
				Receiver.xmppEngine().login(
						org.sipdroid.sipua.ui.Settings
								.getAccountUserName(getBaseContext())
								+ "@"
								+ org.sipdroid.sipua.ui.Settings
										.getXMPP_Service(getBaseContext()),
						org.sipdroid.sipua.ui.Settings
								.getAccountUserName(getBaseContext()));
			}
			intent = new Intent(getBaseContext(),
					org.sipdroid.sipua.ui.MainUIActivity.class);
			startActivity(intent);
			finish();
		}

	}
}
