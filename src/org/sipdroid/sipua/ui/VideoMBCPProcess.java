package org.sipdroid.sipua.ui;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.sipdroid.sipua.ui.screen.PTTCallScreen;

import android.util.Log;

public class VideoMBCPProcess{

	public final static int MBCP_BURST_REQUEST = 0;
	public final static int MBCP_BURST_GRANTED = 1;
	public final static int MBCP_BURST_TAKEN_EXPECT_NO_REPLY = 2;
	public final static int MBCP_BURST_DENY = 3;
	public final static int MBCP_BURST_RELEASE = 4;
	public final static int MBCP_BURST_IDLE = 5;
	public final static int MBCP_BURST_REVOKE = 6;
	public final static int MBCP_BURST_ACKNOWLEDGMENT = 7;
	public final static int MBCP_QUEUE_STATUS_REQUEST = 8;
	public final static int MBCP_QUEUE_STATUS_RESPONSE = 9;
	public final static int MBCP_DISCONNECT = 11;
	public final static int MBCP_CONNECT = 15;
	public final static int MBCP_BURST_TAKEN_EXPECT_REPLY = 18;
	public final static int MBCP_BURST_HELLO = 19;
	public final static int MBCP_BURST_BYE = 20;
	public final static int MBCP_BURST_BYEACK = 21;
	public final static int MBCP_BURST_MEDIA_READY = 22;
	public final static int RTCP_APP = 204;
	// MBCP specific fields
	// P-count
	public final static int P_COUNT = 100;
	// T2-timer
	public final static int T2_TIMER = 101;
	// MB-priority-level
	public final static int MB_PRIORITY_LEVEL = 102;
	// Time-stamp
	public final static int TIME_STAMP = 103;
	// Alert-margin
	public final static int ALERT_MARGIN = 104;
	// Privacy
	public final static int PRIVACY = 105;
	// Anonymous identity
	public final static int ANONYMOUS_IDENTITY = 106;
	// MBCP-restrict
	public final static int MBCP_RESTRICT = 107;
	// Media-Streams
	public final static int MEDIA_STREAMS = 108;
	// Reason-Header
	public final static int REASON_HEADER = 109;
	// /////////////////////////////////////////////////////////////////////////////////////////////////////
	PTTCallScreen gua;
	
	MBCP_HEADER mbcp_header = new MBCP_HEADER();
	MBCP_BODY mbcp_body = new MBCP_BODY();
	MBCP_SPECIFIC_FIELD specific_field = new MBCP_SPECIFIC_FIELD();

	int ptt_state = MBCP_DISCONNECT;
	int ptt_port = 2829;

	final int BUF_SIZE = 256;
	long ssrc = 0;
	byte msgbuf[] = new byte[256];

	DatagramSocket socket = null;
	ReceiveThread RT = null;
	
	// Hao SUA here 
	public static String Status = "Disconnect";

	// ////////////////////////////////////////////////////////////////////////////////////////////////////
	class value_string {
		int type = 0;
		String val = null;
	};

	class MBCP_HEADER {
		byte vps = 0;
		byte pt = 0;
		short len = 0;
	};

	class MBCP_BODY {
		long ssrc = 0;
		byte name[] = new byte[4];
	};

	class MBCP_SPECIFIC_FIELD {
		byte type = 0;
		byte len = 0;
	};

	public class ReceiveThread extends Thread {

		private boolean running;

		public ReceiveThread(String name) throws Exception {
			super(name);
			running = false;
		}

		public void run() {
			byte data[] = new byte[256];
			DatagramPacket receivePacket = new DatagramPacket(data, data.length);
			running = true;

			while (running) {
				try {
					socket.receive(receivePacket);
				} catch (Exception ex) {
					System.out.println(ex.toString());
					running = false;
					break;
				}

				MBCP_parse(data);

				if (ptt_state == MBCP_BURST_GRANTED) {
					//if (gua.videortpmanager.getRealized_mode() == true) {
						try {	
							Log.d("HAO", "VIDEO_MBCP_BURST_GRANTED");
							
							//gua.panel_ptt.jButton_PttVideo_Request.setVisible(false);
							//gua.panel_ptt.jButton_PttVideo_Release.setVisible(true);
							
							// Start SENDing VIDEO STREAM
							
							
							//gua.panel_ptt.jPanel_Video.removeAll();
							//gua.panel_ptt.jPanel_Video.add(gua.panel_ptt.jSoongmain);
							//gua.panel_ptt.jPanel_Video.revalidate();
							//gua.panel_ptt.jPanel_Video.repaint();
						} catch (Exception ex) {
							System.out.println("mbcp send Stream fail to start: " + ex);
						}
					//}
				} else if (ptt_state == MBCP_BURST_TAKEN_EXPECT_NO_REPLY) {
					//if (gua.videortpmanager.created == true && gua.videortpmanager.getRealized_mode() == true) {
						try {
							Log.d("HAO", "VIDEO_MBCP_BURST_TAKEN_EXPECT_NO_REPLY");
							//gua.panel_ptt.jButton_PttVideo_Release.setVisible(false);
							//gua.panel_ptt.jButton_PttVideo_Request.setVisible(true);
							
							// Stop Sending VIDEO STREAM
							//VideoRTPManager.getSendStream().stop();
							//gua.panel_ptt.jPanel_Video.removeAll();
							//gua.panel_ptt.jPanel_Video.add(gua.player_panel);
							//gua.panel_ptt.jPanel_Video.revalidate();
							//gua.panel_ptt.jPanel_Video.repaint();
						} catch (Exception ex) {
							System.out.println("mbcp send Stream stop fail : "
									+ ex);
						}
					//}
				} else if (ptt_state == MBCP_BURST_IDLE) {
					//if (gua.videortpmanager.getRealized_mode() == true) {
						try {
							Log.d("HAO", "VIDEO_MBCP_BURST_IDLE");
							//gua.panel_ptt.jButton_PttVideo_Release.setVisible(false);
							//gua.panel_ptt.jButton_PttVideo_Request.setVisible(true);
							
							// Stop Sending VIDEO STREAM
							//VideoRTPManager.getSendStream().stop();
							//gua.panel_ptt.jPanel_Video.removeAll();
							//gua.panel_ptt.jPanel_Video.add(gua.panel_ptt.jSoongmain);
							//gua.panel_ptt.jPanel_Video.revalidate();
							//gua.panel_ptt.jPanel_Video.repaint();
						} catch (Exception ex) {
							System.out.println("mbcp send Stream stop fail : "
									+ ex);
						}
					//}
				} else if (ptt_state != MBCP_CONNECT) {
					//if (gua.videortpmanager.created == true && gua.videortpmanager.getRealized_mode() == true) {
						try {
							Log.d("HAO", "MBCP_CONNECT");
							//gua.panel_ptt.jButton_PttVideo_Release.setVisible(false);
							//gua.panel_ptt.jButton_PttVideo_Request.setVisible(true);
							
							// Stop Sending VIDEO STREAM
							//VideoRTPManager.getSendStream().stop();
						} catch (Exception ex) {
							System.out.println("mbcp send Stream stop fail : "
									+ ex);
						}
					//}
				}
			}
		}
	}

	// ����
	VideoMBCPProcess(PTTCallScreen ptt) {
		gua = ptt;
		ssrc = gen_ssrc();
		try {
			socket = new DatagramSocket();
		} catch (SocketException se) {
			System.err.println(se);
		}
	}

	public void network_send(String servaddr, int port, byte data[],
			int buf_size) {
		android.util.Log.d("SIPDROID", "VideoMBCPProcess] - network_send - servaddr: " + servaddr + " - port: " + Integer.toString(port));
		byte sendbuf[] = new byte[BUF_SIZE];
		System.arraycopy(data, 0, sendbuf, 0, buf_size);
		try {
			InetAddress server = InetAddress.getByName(servaddr);
			DatagramPacket outPacket = new DatagramPacket(sendbuf,
					sendbuf.length, server, port);
			socket.send(outPacket);
			System.out.println(outPacket);
		} catch (UnknownHostException e) {
			android.util.Log.d("SIPDROID", "VideoMBCPProcess] - network_send - UnknownHostException: " + e.getMessage());
		} catch (IOException e) {
			android.util.Log.d("SIPDROID", "VideoMBCPProcess] - network_send - IOException: " + e.getMessage());
		}
		Allinit();
	}

	public void Allinit() {
		mbcp_header.vps = 0;
		mbcp_header.pt = 0;
		mbcp_header.len = 0;
		mbcp_body.ssrc = 0;
		for (int i = 0; i < mbcp_body.name.length; i++) {
			mbcp_body.name[i] = 0;
		}
		specific_field.len = 0;
		specific_field.type = 0;
		for (int i = 0; i < msgbuf.length; i++) {
			msgbuf[i] = 0;
		}
	}

	public void HelloMSG(String URI) {
		byte name[] = URI.getBytes();
		encode_mbcp_media_burst_hello_msg(ssrc, name);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf,
				get_mbcp_length());
		try {
			RT = new ReceiveThread("Receive");
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
		RT.start();
	}

	public void MediaReadyMSG(String URI) {
		byte name[] = URI.getBytes();
		encode_mbcp_media_burst_media_ready_msg(ssrc, name);
		System.out.println("*********MediaReadyMSG****************");
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf,
				get_mbcp_length());
	}

	public void RequestMSG() {
		Log.d("SIPDROID", "[VideoMBCPProcess] - RequestMSG");
		encode_mbcp_media_burst_req_msg(ssrc, (short) 0);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf,
				get_mbcp_length());
		Log.d("HAO", "RequestMSG");
	}

	public void ReleaseMSG() {
		Log.d("SIPDROID", "[VideoMBCPProcess] - ReleaseMSG");
		encode_mbcp_media_burst_rel_msg(ssrc);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf,
				get_mbcp_length());
	}

	void mbcp_encode_header(int type) {
		Allinit();
		mbcp_header.vps |= ((byte) 2 << 6); //
		mbcp_header.vps |= (0 << 5);// & 0x20 ;
		mbcp_header.vps |= type & 0x1F;
		mbcp_header.pt = (byte) RTCP_APP;
		mbcp_header.len = 0;

	}

	void mbcp_encode_body(long ssrc, String name, MBCP_HEADER mbcp_header) {
		mbcp_body.ssrc = ssrc;
		mbcp_body.name = name.getBytes();
		mbcp_header.len += 8;
	}

	void mbcp_encode_specific_field(byte type, byte len, MBCP_HEADER mbcp_header) {
		specific_field.type = type;
		specific_field.len = len;
		mbcp_header.len += 2;// type + length field
	}

	void mbcp_get_header(byte[] data) {
		byte header[] = new byte[4];
		System.arraycopy(data, 0, header, 0, 4);
		mbcp_header.vps = header[0];
		mbcp_header.pt = header[1];
		mbcp_header.len |= (short) header[3] & 0x00FF;
		mbcp_header.len |= ((short) header[2] << 8) & 0xFF00;
	}

	void mbcp_get_body(byte[] data) {
		byte body[] = new byte[8];
		System.arraycopy(data, 4, body, 0, 8); // get ssrc
		System.arraycopy(body, 4, mbcp_body.name, 0, 4); // get name

		mbcp_body.ssrc = (int) body[0] & 0x000000FF;
		mbcp_body.ssrc |= ((int) body[1] << 8) & 0x0000FF00;
		mbcp_body.ssrc |= ((int) body[2] << 16) & 0x00FF0000;
		mbcp_body.ssrc |= ((int) body[3] << 24) & 0xFF000000;
	}

	void mbcp_get_specific_field() {

	}

	long gen_ssrc() {
		long ssrc;
		ssrc = (long) Math.random();
		Random rand = new Random();
		ssrc = rand.nextLong();
		if (ssrc < 0)
			ssrc = ssrc * (-1);
		System.out.println("Video_ssrc :" + ssrc);
		return ssrc;
	}

	public void makeArray()// spcific �ʵ��� ������ ���
	{
		msgbuf[0] = mbcp_header.vps;
		msgbuf[1] = mbcp_header.pt;
		msgbuf[2] = (byte) (mbcp_header.len & 0x00FF);
		msgbuf[3] = (byte) (mbcp_header.len >> 8 & 0x00FF);

		msgbuf[4] = (byte) (mbcp_body.ssrc >> 24 & 0x000000FF);
		msgbuf[5] = (byte) (mbcp_body.ssrc >> 16 & 0x000000FF);
		msgbuf[6] = (byte) (mbcp_body.ssrc >> 8 & 0x000000FF);
		msgbuf[7] = (byte) (mbcp_body.ssrc >> 0 & 0x000000FF);
		System.arraycopy(mbcp_body.name, 0, msgbuf, 8, 4); // name
	}

	void encode_mbcp_media_burst_hello_msg(long cl_ssrc, byte name[]) {
		mbcp_encode_header(MBCP_BURST_HELLO);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		mbcp_encode_specific_field((byte) 0x01, (byte) name.length, mbcp_header);
		mbcp_header.len += name.length;
		makeArray();
		msgbuf[12] = specific_field.type;
		msgbuf[13] = specific_field.len;
		System.arraycopy(name, 0, msgbuf, 14, name.length);
	}

	void encode_mbcp_media_burst_media_ready_msg(long cl_ssrc, byte name[]) {
		mbcp_encode_header(MBCP_BURST_MEDIA_READY);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		mbcp_encode_specific_field((byte) 0x01, (byte) name.length, mbcp_header);
		mbcp_header.len += name.length;
		makeArray();
		msgbuf[12] = specific_field.type;
		msgbuf[13] = specific_field.len;
		System.arraycopy(name, 0, msgbuf, 14, name.length);
	}

	int encode_mbcp_media_burst_ack_msg(long cl_ssrc) {
		mbcp_encode_header(MBCP_BURST_ACKNOWLEDGMENT);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		/* sub type */
		makeArray();
		return mbcp_header.len + 4;// + sizeof(MBCP_HEADER_t);
	}

	void encode_mbcp_media_burst_req_msg(long cl_ssrc, short mb_privacy) {

		mbcp_encode_header(MBCP_BURST_REQUEST);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		mbcp_encode_specific_field((byte) MB_PRIORITY_LEVEL, (byte) 2,
				mbcp_header);
		mbcp_header.len += 2;
		makeArray();
		msgbuf[12] = specific_field.type;
		msgbuf[13] = specific_field.len;
		msgbuf[14] = (byte) (mb_privacy >> 8 & 0x00FF);
		msgbuf[15] = (byte) (mb_privacy >> 0 & 0x00FF);
		/* time stemp */
	}

	void encode_mbcp_media_burst_rel_msg(long cl_ssrc) {
		mbcp_encode_header(MBCP_BURST_RELEASE);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		makeArray();
		/* seq num */
	}

	void encode_mbcp_burst_BYE(long cl_ssrc, char buf[]) {
		mbcp_encode_header(MBCP_BURST_BYE);
		mbcp_encode_body(cl_ssrc, "PoC1", mbcp_header);
		makeArray();
		/* seq num */
	}

	int get_mbcp_length() {
		return mbcp_header.len + 4;
	}

	int get_mbcp_subtype(MBCP_HEADER mbcp_header) {
		int type = 0;
		if (mbcp_header.vps != 0) {
			type = mbcp_header.vps & 0x1F;
			return type;
		} else
			return -1;
	}

	public void viewInfo(DatagramPacket packet) {
		System.out
				.println("server address : " + packet.getAddress().toString());
		System.out.println("server port : " + packet.getPort());
		System.out.println("data length : " + packet.getLength());
		System.out.println("data : " + packet.getData());
	}

	public void MBCP_parse(byte data[]) {
		mbcp_get_header(data);
		mbcp_get_body(data);

		switch (get_mbcp_subtype(mbcp_header)) {
		case MBCP_BURST_REQUEST:
			ptt_state = MBCP_BURST_REQUEST;
			System.out.println("\nVideo Request state.\n");
			
			Status = "Waiting";
			break;
		case MBCP_BURST_GRANTED:
			ptt_state = MBCP_BURST_GRANTED;
			System.out.println("\nVideo Granted state.\n");
			
			Status = "Sending";
			break;
		case MBCP_BURST_TAKEN_EXPECT_NO_REPLY:
			ptt_state = MBCP_BURST_TAKEN_EXPECT_NO_REPLY;
			System.out.println("\nVideo Taken state.\n");
			
			Status = "Receiving";
			break;
		case MBCP_BURST_DENY:
			ptt_state = MBCP_BURST_DENY;
			System.out.println("\nVideo Deny state.\n");
			
			Status =  "Receiving";
			break;
		case MBCP_BURST_RELEASE:
			ptt_state = MBCP_BURST_RELEASE;
			System.out.println("\nVideo Release state.\n");
			
			Status = "Waiting";
			break;
		case MBCP_BURST_IDLE:
			ptt_state = MBCP_BURST_IDLE;
			System.out.println("\nVideo IDLE state.\n");
			
			Status="Waiting";
			break;
		case MBCP_BURST_REVOKE:
			ptt_state = MBCP_BURST_REVOKE;
			System.out.println("\nVideo Revoke state.\n");
			break;
		case MBCP_BURST_ACKNOWLEDGMENT:
			ptt_state = MBCP_BURST_ACKNOWLEDGMENT;
			System.out.println("\nAudio Acknowledgement state.\n");
			break;
		case MBCP_QUEUE_STATUS_REQUEST:
			ptt_state = MBCP_QUEUE_STATUS_REQUEST;
			System.out.println("\nVideo nQueue status request state.\n");
			break;
		case MBCP_QUEUE_STATUS_RESPONSE:
			ptt_state = MBCP_QUEUE_STATUS_RESPONSE;
			System.out.println("\nVideo nQueue status response state.\n");
			break;
		case MBCP_DISCONNECT:
			ptt_state = MBCP_DISCONNECT;
			System.out.println("\n\nVideo Disconnect state.\n\n");
			
			Status="Waiting";
			break;
		case MBCP_CONNECT:
			ptt_state = MBCP_CONNECT;
			Allinit();
			System.out.println("\n\nAudio Connect state.\n\n");
			encode_mbcp_media_burst_ack_msg(ssrc);
			network_send(Sipdroid.ptt_address, ptt_port, msgbuf,get_mbcp_length());
			System.out.println("\nVideo Connect state, sendto success!!!!\n");
			MediaReadyMSG(Receiver.mSipdroidEngine.user_profiles[0].username);
			
			//gua.remove(gua.jPanel_main);
			//gua.setContentPane(gua.panel_ptt);
			//gua.setVisible(true);

			//gua.setBeforePanel(Sipdroid.S_Call);

			Allinit();
			Status="Waiting";
			// kiem tra neu la CAMERA ON thi se~ GRANT quyen xem 
			//if (gua.presence_panel != null) {
			//	if (gua.isCamera()) {
			//		RequestMSG();
			//	}
			//}
			break;
		case MBCP_BURST_TAKEN_EXPECT_REPLY:
			ptt_state = MBCP_BURST_TAKEN_EXPECT_REPLY;
			System.out.println("\nVideo Taken expect reply state.\n");
			
			Status="Waiting";
			break;
		case MBCP_BURST_HELLO:
			ptt_state = MBCP_BURST_HELLO;
			System.out.println("\nVideo Hellow state.\n");
			
			Status="Waiting";
			break;
		}
	}

	int network_recv(int sock, char buf[]) {
		return 0;
	}
}
