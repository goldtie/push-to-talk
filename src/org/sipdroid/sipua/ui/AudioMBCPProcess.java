package org.sipdroid.sipua.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import org.sipdroid.media.MediaLauncher;
import org.sipdroid.sipua.UserAgent;

import android.util.Log;

public class AudioMBCPProcess {

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
	int ptt_port = 2828;
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
					Log.d("SIPDROID", "[AudioMBCPProcess - ReceiveThread]");
					socket.receive(receivePacket);
				} catch (Exception ex) {
					Log.d("SIPDROID", "[AudioMBCPProcess - ReceiveThread] - run - Exception: " + ex.getMessage());
					running = false;
					break;
				}

				MBCP_parse(data);
				Log.d("SIPDROID", "[AudioMBCPProcess - ReceiveThread] - run - State: " + String.valueOf(ptt_state));
				
				if (ptt_state == MBCP_BURST_GRANTED) {
					try {
						UserAgent.audio_app.startSendMedia();
					} catch (Exception ex) {
						Log.d("SIPDROID", "[AudioMBCPProcess - ReceiveThread] - run - Exception: MBCP sending fails. Details: " + ex.getMessage());
					}
					PTTCallScreen.isAudioSending = true;
				} else if (ptt_state != MBCP_CONNECT) {
					try {
						UserAgent.audio_app.stopSendMedia();
					} catch (Exception ex) {
						Log.d("SIPDROID", "[AudioMBCPProcess - ReceiveThread] - run - Exception: MBCP stopping fails. Details: " + ex.getMessage());
					}
					PTTCallScreen.isAudioSending = false;
				}
			}
		}
	}	

	// ����
	AudioMBCPProcess(PTTCallScreen pttCallScreen) {
		gua = pttCallScreen;
		try {
			socket = new DatagramSocket();
		} catch (SocketException se) {
			android.util.Log.d("SIPDROID", "[AudioMBCPProcess] - initilizing AudioMBCPProcess error - Exception: " + se.getMessage());
		}
	}

	public void network_send(String servaddr, int port, byte data[],
			int buf_size) {
		android.util.Log.d("SIPDROID", "[AudioMBCPProcess] - network_send - servaddr: " + servaddr + " - port: " + Integer.toString(port));
		byte sendbuf[] = new byte[buf_size];
		System.arraycopy(data, 0, sendbuf, 0, buf_size);
		// System.out.println("���ۻ������ : " + sendbuf.length);
		try {
			String strInput;
			DatagramPacket outPacket;
			InetAddress server = InetAddress.getByName(servaddr);
			BufferedReader userInput = new BufferedReader(
					new InputStreamReader(System.in));

			outPacket = new DatagramPacket(sendbuf, sendbuf.length, server,
					port);
			// this.viewInfo(outPacket);
			socket.send(outPacket);

		} catch (UnknownHostException e) {
			android.util.Log.d("SIPDROID", "[AudioMBCPProcess] - network_send - UnknownHostException: " + e.getMessage());
		} catch (IOException e) {
			android.util.Log.d("SIPDROID", "[AudioMBCPProcess] - network_send - IOException: " + e.getMessage());
		}
		Allinit();
	}
	
	public void Allinit() {
		mbcp_header.vps = 0;
		mbcp_header.pt = 0;
		mbcp_header.len = 0;
		mbcp_body.ssrc = 0;
		// mbcp_body.name = ;
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
		Log.d("SIPDROID", "[AudioMBCPProcess] - HelloMSG");
		ssrc = gen_ssrc();
		byte name[] = URI.getBytes();
		encode_mbcp_media_burst_hello_msg(ssrc, name);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf, get_mbcp_length());
		try {
			
			RT = new ReceiveThread("Receive");
			
		} catch (Exception ex) {
			Log.d("SIPDROID", "[AudioMBCPProcess] - HelloMSG - Exception: " + ex.getMessage());
		}
		RT.start();
	}

	public void RequestMSG() {
		Log.d("SIPDROID", "[AudioMBCPProcess] - RequestMSG");
		encode_mbcp_media_burst_req_msg(ssrc, (short) 0);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf, get_mbcp_length());
	}

	public void ReleaseMSG() {
		Log.d("SIPDROID", "[AudioMBCPProcess] - ReleaseMSG");
		encode_mbcp_media_burst_rel_msg(ssrc);
		network_send(Sipdroid.ptt_address, ptt_port, msgbuf, get_mbcp_length());
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
		System.out.println("Audio_ssrc :" + ssrc);
		return ssrc;
	}

	public void makeArray() {
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
			System.out.println("\nAudio Request state.\n");
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Request state");
			Status = "Waiting";
			break;
		case MBCP_BURST_GRANTED:
			ptt_state = MBCP_BURST_GRANTED;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Granted state");

			Status = "Sending";
			break;
		case MBCP_BURST_TAKEN_EXPECT_NO_REPLY:
			ptt_state = MBCP_BURST_TAKEN_EXPECT_NO_REPLY;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Taken state");
			Status = "Receiving";
			break;
		case MBCP_BURST_DENY:
			ptt_state = MBCP_BURST_DENY;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Deny state");
			Status =  "Receiving";
			
			break;
		case MBCP_BURST_RELEASE:
			ptt_state = MBCP_BURST_RELEASE;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Release state");
			Status = "Waiting";
			break;
		case MBCP_BURST_IDLE:
			ptt_state = MBCP_BURST_IDLE;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio IDLE state");
			
			Status="Waiting";
			break;
		case MBCP_BURST_REVOKE:
			ptt_state = MBCP_BURST_REVOKE;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Revoke state");
			
			break;
		case MBCP_BURST_ACKNOWLEDGMENT:
			ptt_state = MBCP_BURST_ACKNOWLEDGMENT;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Acknowledgement state");
			
			break;
		case MBCP_QUEUE_STATUS_REQUEST:
			ptt_state = MBCP_QUEUE_STATUS_REQUEST;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Queue status request state");
			
			break;
		case MBCP_QUEUE_STATUS_RESPONSE:
			ptt_state = MBCP_QUEUE_STATUS_RESPONSE;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Queue status response state");
			
			break;
		case MBCP_DISCONNECT:
			ptt_state = MBCP_DISCONNECT;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Disconnect state");
			
			Status="Waiting";
			break;
		case MBCP_CONNECT:
			ptt_state = MBCP_CONNECT;
			Allinit();
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Connect state");
			
			encode_mbcp_media_burst_ack_msg(ssrc);
			network_send(Sipdroid.ptt_address, ptt_port, msgbuf,get_mbcp_length());
			System.out.println("\nAudio Connect state, sendto success!!!!\n");
			Allinit();
			
			Status="Waiting";
			break;
		case MBCP_BURST_TAKEN_EXPECT_REPLY:
			ptt_state = MBCP_BURST_TAKEN_EXPECT_REPLY;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Taken expect reply state");
			
			Status="Waiting";
			break;
		case MBCP_BURST_HELLO:
			ptt_state = MBCP_BURST_HELLO;
			Log.d("SIPDROID", "[AudioMBCPProcess] - MBCP_parse - Audio Hello state");
			
			Status="Waiting";
			break;
		}
	}

	int network_recv(int sock, char buf[]) {
		return 0;
	}
}
