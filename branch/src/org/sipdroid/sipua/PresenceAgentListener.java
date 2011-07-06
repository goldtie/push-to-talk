/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * Copyright (C) 2005 Luca Veltri - University of Parma - Italy
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

package org.sipdroid.sipua;


import org.zoolu.sip.address.NameAddress;

/** Listener of PresenceAgent */
public interface PresenceAgentListener {
	// //////09.08.20 add by LHK
	/** When a new PUBLISH is received. */
	public void onPaPublicationRequest(PresenceAgent pa,
			NameAddress presentity, NameAddress watcher);

	/** When a publication request successes. */
	public void onPaPublicationSuccess(PresenceAgent pa, NameAddress presentity);

	/** When a publication terminates. */
	public void onPaPublicationTerminated(PresenceAgent pa,
			NameAddress presentity, String reason);

	// ////////////////////////////////////////////////////////////////

	/** When a new SUBSCRIBE is received. */
	public void onPaSubscriptionRequest(PresenceAgent pa,
			NameAddress presentity, NameAddress watcher);

	/** When a subscription request successes. */
	public void onPaSubscriptionSuccess(PresenceAgent pa, NameAddress presentity);

	/** When a subscription terminates. */
	public void onPaSubscriptionTerminated(PresenceAgent pa,
			NameAddress presentity, String reason);

	/** When a new NOTIFY is received. */
	public void onPaNotificationRequest(PresenceAgent pa,
			NameAddress recipient, NameAddress notifier, String event,
			String content_type, String body);

	/** When a subscription request successes. */
	public void onPaNotificationFailure(PresenceAgent pa,
			NameAddress recipient, String reason);

}
