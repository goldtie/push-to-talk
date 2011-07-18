/* Modified by:
 * 2009.08.16. LHK
 */

package org.zoolu.sip.dialog;

import org.sipdroid.sipua.ui.screen.Presence;
import org.sipdroid.sipua.ui.TomP2PFunctions;
import org.zoolu.sip.address.NameAddress;
import org.zoolu.sip.address.SipURL;
import org.zoolu.sip.header.CSeqHeader;
import org.zoolu.sip.header.ContentTypeHeader;
import org.zoolu.sip.header.EventHeader;
import org.zoolu.sip.header.ExpiresHeader;
import org.zoolu.sip.header.StatusLine;
import org.zoolu.sip.message.Message;
import org.zoolu.sip.message.MessageFactory;
import org.zoolu.sip.message.SipMethods;
import org.zoolu.sip.message.SipResponses;
import org.zoolu.sip.provider.MethodIdentifier;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.transaction.TransactionClient;
import org.zoolu.sip.transaction.TransactionClientListener;
import org.zoolu.sip.transaction.TransactionServer;
import org.zoolu.tools.LogLevel;

/**
 * PublisherDialog.
 */
public class PublisherDialog extends Dialog implements
		TransactionClientListener {
	/** String "active" */
	protected static final String ACTIVE = "active";
	/** String "pending" */
	protected static final String PENDING = "pending";
	/** String "terminated" */
	protected static final String TERMINATED = "terminated";

	/** The PublisherDialog listener */
	PublisherDialogListener listener;

	/** The current publish method */
	Message publish_req;

	/** The notify transaction */
	TransactionServer notify_transaction=null;
	/** The publish transaction */
	TransactionClient publish_transaction;

	/** The event package name */
	String event;

	/** The publication id */
	String id;

	// HAO SUA TAM 
	//==>protected GraphicalUA gua;

	/** Internal state D_INIT */
	protected static final int D_INIT = 0;
	/** Internal state D_PUBLISHING */
	protected static final int D_PUBLISHING = 1;
	/** Internal state D_PUBLISHED */
	protected static final int D_ACCEPTED = 2;
	/** Internal state D_PENDING */
	protected static final int D_PENDING = 3;
	/** Internal state D_ACTIVE */
	protected static final int D_ACTIVE = 4;
	/** Internal state D_TERMINATED */
	protected static final int D_TERMINATED = 9;

	/** Gets the dialog state */
	protected String getStatusString() {
		switch (status) {
		case D_INIT:
			return "D_INIT";
		case D_PUBLISHING:
			return "D_PUBLISHING";
		case D_ACCEPTED:
			return "D_ACCEPTED";
		case D_PENDING:
			return "D_PENDING";
		case D_ACTIVE:
			return "D_ACTIVE";
		case D_TERMINATED:
			return "D_TERMINATED";
		default:
			return null;
		}
	}

	// *************************** Public methods **************************

	/** Whether the dialog is in "early" state. */
	public boolean isEarly() {
		return (status < D_ACCEPTED);
	}

	/** Whether the dialog is in "confirmed" state. */
	public boolean isConfirmed() {
		return (status >= D_ACCEPTED && status < D_TERMINATED);
	}

	/** Whether the dialog is in "active" state. */
	public boolean isTerminated() {
		return (status == D_TERMINATED);
	}

	/** Whether the publication is "pending". */
	public boolean isPublicationPending() {
		return (status >= D_ACCEPTED && status < D_ACTIVE);
	}

	/** Whether the Publication is "active". */
	public boolean isPublicationActive() {
		return (status == D_ACTIVE);
	}

	/** Whether the Publication is "terminated". */
	public boolean isPublicationTerminated() {
		return (status == D_TERMINATED);
	}

	/** Gets event type. */
	public String getEvent() {
		return event;
	}

	/** Gets the event "id" parameter. */
	public String getId() {
		return id;
	}

	// **************************** Costructors ****************************
	/** Creates a new NotifierDialog. */
	public PublisherDialog(SipProvider sip_provider, PublisherDialogListener listener) {
		super(sip_provider);
		init(listener);
	}


	public PublisherDialog(SipProvider sip_provider, Message publish,
			PublisherDialogListener listener) {
		super(sip_provider);
		init(listener);

		changeStatus(D_ACCEPTED);
		publish_req = publish;
		notify_transaction = new TransactionServer(sip_provider, publish, null);
		update(Dialog.UAS, publish);
		EventHeader eh = publish.getEventHeader();
		if (eh != null) {
			event = eh.getEvent();
			id = eh.getId();
		}
	}

/** Inits the NotifierDialog. */
	private void init(PublisherDialogListener listener) {
		this.listener = listener;
		this.notify_transaction = null;
		this.publish_transaction = null;
		this.publish_req = null;
		this.event = null;
		this.id = null;
		changeStatus(D_INIT);
	}


	/** Creates a new PublisherDialog. */
	public PublisherDialog(SipProvider sip_provider, /*String publisher, String contact, */
	String event, String id, PublisherDialogListener listener) {
		super(sip_provider);
		init(listener);
		this.event = event;
		this.id = id;
	}

	// *************************** Public methods **************************

	/** Listen for the first subscription request. */
	public void listen() {
		printLog("inside method listen()", LogLevel.MEDIUM);
		if (!statusIs(D_INIT)) {
			printLog("first subscription already received", LogLevel.MEDIUM);
			return;
		}
		// else
		changeStatus(D_PUBLISHING);
		// listen for the first PUBLISH request
		sip_provider.addSipProviderListener(new MethodIdentifier(SipMethods.PUBLISH), this);
	}

	/**
	 * Sends a new PUBLISH request (starts a new publication). It also
	 * initializes the dialog state information.
	 * 
	 * @param target
	 *            the target url (and display name)
	 * @param subscriber
	 *            the publisher url (and display name)
	 * @param contact
	 *            the contact url OR the contact user-name
	 */
	public void publish(String target, String publisher, String contact,
			int expires, String state, String state_img) {
		printLog("inside publish(target=" + target + ",publisher=" + publisher
				+ ",contact=" + contact + ",id=" + id + ",expires=" + expires
				+ ")", LogLevel.MEDIUM);
		SipURL request_uri = new SipURL(target);
		NameAddress to_url = new NameAddress(target);
		NameAddress from_url = new NameAddress(publisher);
		NameAddress contact_url;
		if (contact != null)
			contact_url = new NameAddress(contact);
		else
			contact_url = from_url;
		String content_type = null;
		String basic = "close";
		if (state.equals("Log out")) {
			basic = "close";
		} else {
			basic = "open";
		}
		String xml = "<?xml version='1.0' encoding='UTF-8'?>"
				+ "<presence xmlns='urn:ietf:params:xml:ns:pidf' "
				+ "xmlns:dm='urn:ietf:params:xml:ns:pidf:data-model' "
				+ "xmlns:rpid='urn:ietf:params:xml:ns:pidf:rpid' "
				+ "xmlns:c='urn:ietf:params:xml:ns:pidf:cipid' "
				+
				// "entity='sip:1000@220.70.2.141'>" +
				"entity='sip:" + target + "'>" + "<tuple id='tdd13ee5c'>"
				+ "<status><basic>" + basic + "</basic></status></tuple>"
				+ "<dm:person id='pb82dcd2c'>" + "<rpid:activities>" + "<rpid:"
				+ state_img + "/>" + "</rpid:activities>" + "<dm:note>" + state
				+ "</dm:note></dm:person></presence>";
		Message req = MessageFactory.createPublishRequest(sip_provider,
				request_uri, to_url, from_url, contact_url, event, id,
				content_type, xml);
		req.setHeader(new ContentTypeHeader("application/pidf+xml"));
		req.setExpiresHeader(new ExpiresHeader(expires));
		publish(req);
		Presence.lastPublishMsg = req;
	}

	/**
	 * Sends a new PUBLISH request (starts a new publication). It also
	 * initializes the dialog state information.
	 * 
	 * @param req
	 *            the PUBLISH message
	 */
	public void publish(Message req) {
		printLog("inside publish(req)", LogLevel.MEDIUM);
		if (statusIs(D_TERMINATED)) {
			printLog("publication already terminated: request aborted",
					LogLevel.MEDIUM);
			return;
		}
		// else
		if (statusIs(D_INIT)) {
			changeStatus(D_PUBLISHING);
		}
		
		// ==> jinsub for presence server
		try {

			if (req.getContactHeader().toString().contains(TomP2PFunctions.ownIP)) {
				publish_transaction = new TransactionClient(sip_provider, req, this);
				publish_transaction.request(true);
				return;
			}
		} catch (Exception e) {
			System.err.println("PublisherDialog : " + e);
		}
		//

		update(UAC, req);
		// start client transaction
		publish_transaction = new TransactionClient(sip_provider, req, this);
		publish_transaction.request();
	}

	// HAO edit for presence service --> republish
	public void rePublish(Message req) {
		if (req != null) {
			req.setCSeqHeader(new CSeqHeader(req.getCSeqHeader()
					.getSequenceNumber() + 1, SipMethods.PUBLISH));

			req.setExpiresHeader(new ExpiresHeader(SipStack.default_expires * 2));
			publish(req);
		}
	}

	// <---

	/** Sends a new PUBLISH request (starts a new publication). */
	/*
	 * public void rePublish(String target, String publisher, String contact,
	 * int expires) { String state = null;
	 * publish(target,publisher,contact,expires,state); }
	 */

	// ************** Inherited from TransactionClientListener **************

	/**
	 * When the TransactionClient is (or goes) in "Proceeding" state and receives a new 1xx
	 * provisional response
	 */
	@Override
	public void onTransProvisionalResponse(TransactionClient tc, Message resp) {
		printLog("onTransProvisionalResponse()", LogLevel.MEDIUM);
		// do nothing.
	}

	/** When the TransactionClient goes into the "Completed" state receiving a 2xx response */
	@Override
	public void onTransSuccessResponse(TransactionClient tc, Message resp) {
		printLog("onTransSuccessResponse()", LogLevel.MEDIUM);
		if (!statusIs(D_ACTIVE)) {
			changeStatus(D_ACCEPTED);
			update(UAC, resp);
			StatusLine status_line = resp.getStatusLine();
			if (listener != null)
				listener.onDlgPublicationSuccess(this, status_line.getCode(),
						status_line.getReason(), resp);
		} else if (statusIs(D_ACTIVE)) {
			StatusLine status_line = resp.getStatusLine();
			if (listener != null)
				listener.onDlgPublicationSuccess(this, status_line.getCode(),
						status_line.getReason(), resp);
		}
	}

	/** When the TransactionClient goes into the "Completed" state receiving a 300-699 response */
	@Override
	public void onTransFailureResponse(TransactionClient tc, Message resp) {
		printLog("onTransFailureResponse()", LogLevel.MEDIUM);
		changeStatus(D_TERMINATED);
		StatusLine status_line = resp.getStatusLine();
		if (listener != null)
			listener.onDlgPublicationFailure(this, status_line.getCode(),
					status_line.getReason(), resp);
	}

	/** When the TransactionClient goes into the "Terminated" state, caused by transaction timeout */
	@Override
	public void onTransTimeout(TransactionClient tc) {
		printLog("onTransTimeout()", LogLevel.MEDIUM);
		changeStatus(D_TERMINATED);
		if (listener != null)
			listener.onDlgPublishTimeout(this);
	}

	// ***************** Inherited from SipProviderListener *****************

	/** When a new Message is received by the SipProvider. */
	@Override
	public void onReceivedMessage(SipProvider sip_provider, Message msg) {
		printLog("onReceivedMessage()", LogLevel.MEDIUM);
		if (statusIs(D_TERMINATED)) {
			printLog("publication already terminated: message discarded",
					LogLevel.MEDIUM);
			return;
		}
		// else
		if (msg.isRequest() && msg.isNotify()) {
			TransactionServer ts = new TransactionServer(sip_provider, msg,
					null);
			ts.respondWith(MessageFactory.createResponse(msg, 200, SipResponses
					.reasonOf(200), null));

			NameAddress to = msg.getToHeader().getNameAddress();
			NameAddress from = msg.getFromHeader().getNameAddress();
			NameAddress contact = null;
			if (msg.hasContactHeader())
				contact = msg.getContactHeader().getNameAddress();
			String state = null;
			if (msg.hasSubscriptionStateHeader())
				state = msg.getSubscriptionStateHeader().getState();
			String content_type = null;
			if (msg.hasContentTypeHeader())
				content_type = msg.getContentTypeHeader().getContentType();
			String body = null;
			if (msg.hasBody())
				body = msg.getBody();

			// if (listener!=null)
			// listener.onDlgNotify(this,to,from,contact,state,content_type,body,msg);

			if (state != null) {
				if (state.equalsIgnoreCase(ACTIVE) && !statusIs(D_TERMINATED)) {
					changeStatus(D_ACTIVE);
				} else if (state.equalsIgnoreCase(PENDING)
						&& statusIs(D_ACCEPTED)) {
					changeStatus(D_PENDING);
				} else if (state.equalsIgnoreCase(TERMINATED)
						&& !statusIs(D_TERMINATED)) {
					changeStatus(D_TERMINATED);
					if (listener != null)
						listener.onDlgPublicationTerminated(this);
				}
			}
		} else {
			printLog("message is not a NOTIFY: message discarded",
					LogLevel.HIGH);
		}
		
		if (msg.isRequest() && msg.isPublish()) {

			if (statusIs(PublisherDialog.D_PUBLISHING)) { // the first SUBSCRIBE request
				changeStatus(D_ACCEPTED);
				sip_provider.removeSipProviderListener(new MethodIdentifier(SipMethods.PUBLISH));
			}
			publish_req = msg;
			NameAddress target = msg.getToHeader().getNameAddress();
			NameAddress publisher = msg.getFromHeader().getNameAddress();
			EventHeader eh = msg.getEventHeader();
			if (eh != null) {
				event = eh.getEvent();
				id = eh.getId();
			}
			update(UAS, msg);
			notify_transaction = new TransactionServer(sip_provider, msg, null);
			if (listener != null)
				listener.onDlgPublish(this, target, publisher, event, id, msg);

		} else {
			printLog("message is not a SUBSCRIBE: message discarded", LogLevel.HIGH);
		}
	}

	// **************************** Logs ****************************/

	/** Adds a new string to the default Log */
	@Override
	protected void printLog(String str, int level) {
		if (log != null)
			log.println("PublisherDialog#" + dialog_sqn + ": " + str, level
					+ SipStack.LOG_LEVEL_DIALOG);
	}

	@Override
	protected String getStatusDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

}
