// 09.08.17 add by LHK

package org.zoolu.sip.dialog;


import org.zoolu.sip.message.Message;
import org.zoolu.sip.address.NameAddress;


/** A SubscriberDialogListener listens for SubscriberDialog events.
  * It collects all SubscriberDialog callback functions.
  */
public interface PublisherDialogListener {  
 
   /** When an incoming SUBSCRIBE is received. */
	public void onDlgPublish(PublisherDialog dialog, NameAddress target, NameAddress subscriber,
			String event, String id, Message msg);
   
   /** When a 300-699 response is received for an SUBSCRIBE transaction. */ 
   public void onDlgPublicationFailure(PublisherDialog dialog, int code, String reason, Message msg);

   /** When SUBSCRIBE transaction expires without a final response. */ 
   public void onDlgPublishTimeout(PublisherDialog dialog);

   /** When the dialog is terminated. */ 
   public void onDlgPublicationTerminated(PublisherDialog dialog);

   /** When an incoming NOTIFY is received. */ 
  // public void onDlgNotify(PublisherDialog dialog, NameAddress target, NameAddress notifier, NameAddress contact, String state, String content_type, String body, Message msg);

/** When a 2xx successfull final response is received for an SUBSCRIBE transaction. */ 
   public void onDlgPublicationSuccess(PublisherDialog dialog, int code, String reason, Message msg);
}
