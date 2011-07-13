package org.sipdroid.sipua.ui;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Random;
import java.util.Vector;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;

import android.R.bool;

import net.tomp2p.connection.Bindings;
import net.tomp2p.connection.Bindings.Protocol;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDHT;
import net.tomp2p.futures.FutureForkedBroadcast;
import net.tomp2p.futures.FutureRouting;
import net.tomp2p.p2p.Peer;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.storage.Data;

public class TomP2PFunctions {

	final Random rnd = new Random(42L);
	final int DEFAULT_PORTTCP = 4003;
	final int DEFAULT_PORTUDP = 4002;
	static Peer peer;
	static String ownIP = Settings.getLocalIpAddress();
	String ipBootstrap;
	static Number160 peerId;

	boolean isStarted = false;
	public boolean isBootstrap = false;

	public TomP2PFunctions(Boolean isBootstrap, String ip_bootstrap) {
		this.isBootstrap = isBootstrap;
		this.ipBootstrap = ip_bootstrap;
	}

	public boolean start() {
		if (!isStarted) {
			boolean rs = false;
			try {
				if (isBootstrap) {
					initNetwork(DEFAULT_PORTTCP, DEFAULT_PORTUDP);
					rs = true;
				} else {
					rs = joinNetwork(ipBootstrap, DEFAULT_PORTTCP,
							DEFAULT_PORTUDP);
				}
			} catch (Exception e) {
				e.printStackTrace();
				rs = false;
			}
			isStarted = rs;
			return rs;
		}
		return true;
	}

	public void initNetwork(int portTCP, int portUDP) throws Exception {
		Bindings bindings = new Bindings(false);
		bindings.addProtocol(Protocol.IPv4);
		bindings.addAddress(InetAddress.getByName(ownIP));
		bindings.setOutsideAddress(InetAddress.getByName(ownIP), portTCP,
				portUDP);
		Number160 pId = new Number160(1);
		peer = new Peer(pId);
		peerId = pId;

		peer.listen(portUDP, portTCP, bindings);
		peer.getPeerBean().getPeerMap().addAddressFilter(
				InetAddress.getByName("0.0.0.0"));
		// Peer[] nodes = createAndAttachNodes(peer, 10);
		// attachMessageReceiver ();
	}

	public boolean joinNetwork(String ipBootstrap, int portTCP, int portUDP)
			throws Exception {

		Bindings bindings = new Bindings(false);
		bindings.addAddress(InetAddress.getByName(ownIP));
		bindings.addProtocol(Protocol.IPv4);
		bindings.setOutsideAddress(InetAddress.getByName(ownIP), portTCP,
				portUDP);
		Number160 pId = new Number160(rnd.nextInt());
		peer = new Peer(pId);
		peerId = pId;
		peer.listen(portUDP, portTCP, bindings);

		peer.getP2PConfiguration().setReplicationRefreshMillis(5 * 1000);
		peer.setDefaultStorageReplication();

		// filter IP 0.0.0.0, to prevent bugs cause by tomp2p lib
		peer.getPeerBean().getPeerMap().addAddressFilter(
				InetAddress.getByName("0.0.0.0"));

		FutureBootstrap tmp = peer.bootstrap(new PeerAddress(new Number160(1),
				InetAddress.getByName(ipBootstrap), portTCP, portUDP));
		tmp.awaitUninterruptibly();

		boolean failed = true;
		FutureForkedBroadcast forkedBroadcast = (FutureForkedBroadcast) tmp;
		forkedBroadcast.getIntermediateFutures().add(forkedBroadcast.getLast());
		for (FutureRouting futureRouting : forkedBroadcast
				.getIntermediateFutures()) {
			if (futureRouting.isSuccess()
					&& futureRouting.getDirectHits().size() == 0
					&& futureRouting.getPotentialHits().size() == 1) {

				if (!futureRouting.getPotentialHits().first().equals(
						peer.getPeerAddress())) {
					failed = false;
					break;
				}
			} else {
				failed = false;
				break;
			}
		}

		if (!failed) {
			System.out.println("==> Join the P2P overlay successfully.");
			return true;
		} else {
			System.out.println("==> Fail to join the P2P overlay");
			return false;
		}

	}

	public static boolean register(String key) throws IOException {
		return register(key, ownIP);
	}

	public static boolean register(String key, String value) throws IOException {
		Data data = new Data(value.getBytes());

		FutureDHT futureDHT = peer.put(Number160.createHash(key), data);
		futureDHT.awaitUninterruptibly();

		if (futureDHT.isSuccess()) {
			System.out.println("   => IP:" + value + " is mapped to " + key);
			return true;
		} else {
			System.err.println("==> Fail to map IP:" + value + " to " + key);
			return false;
		}
	}

	public static boolean unregister(String key) throws IOException {
		FutureDHT futureDHT = peer.remove(Number160.createHash(key));
		futureDHT.awaitUninterruptibly();
		if (futureDHT.isSuccess()) {
			System.out.println("==> UnRegister URL:" + key + " successfully.");
			return true;
		} else {
			System.out.println("==> Failure to UnRegister URL:" + key);
			return false;
		}
	}

	public static String retrieve(String key) {
		FutureDHT futureDHT = peer.get(Number160.createHash(key));
		futureDHT.awaitUninterruptibly();
		String value;
		if (futureDHT.isSuccess()) {
			Data d1 = futureDHT.getRawData().values().iterator().next()
					.values().iterator().next();
			value = new String(d1.getData(), d1.getOffset(), d1.getLength());
		} else {
			return null;
		}
		return value;
	}

	public void leaveNetwork(Vector<String> lstURLs) {
		// Unregister all
		for (String strURL : lstURLs) {
			try {
				unregister(strURL);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
		// back up if have

		// shut down the peer
		peer.shutdown();
	}
}
