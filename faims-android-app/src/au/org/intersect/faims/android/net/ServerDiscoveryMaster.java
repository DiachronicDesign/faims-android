package au.org.intersect.faims.android.net;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Application;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import au.org.intersect.faims.android.R;
import au.org.intersect.faims.android.util.FAIMSLog;
import au.org.intersect.faims.android.util.JsonUtil;

import com.google.gson.JsonObject;

public class ServerDiscoveryMaster {
	
	private static ServerDiscoveryMaster instance;
	
	private LinkedList<IServerDiscovery.ServerDiscoveryListener> listenerList;
	private Application application;
	
	private boolean isFindingServer;
	
	private String serverIP;
	private String serverPort;

	private Timer timer;
	
	public ServerDiscoveryMaster() {
		listenerList = new LinkedList<IServerDiscovery.ServerDiscoveryListener>();
		isFindingServer = false;
	}
	
	public static ServerDiscoveryMaster getInstance() {
		if (instance == null) instance = new ServerDiscoveryMaster();
		return instance;
	}
	
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public String getServerIP() {
		return serverIP;
	}
	
	public String getServerPort() {
		return String.valueOf(serverPort);
	}
	
	public String getServerHost() {
		return "http://" + serverIP + ":" + serverPort;
	}
	
	public void invalidateServerHost() {
		serverIP = null;
		serverPort = null;
	}
	
	public boolean isServerHostValid() {
		return serverIP != null && serverPort != null;
	}
	
	public synchronized void stopDiscovery() {
		FAIMSLog.log();
		
		killThreads();
	}
	
	public synchronized void startDiscovery(IServerDiscovery.ServerDiscoveryListener listener) {
		FAIMSLog.log();
		
		if (isServerHostValid()) {
			FAIMSLog.log("WARNING: server is already valid");
			listener.handleDiscoveryResponse(true);
			return ;
		}
		
		listenerList.add(listener);
		
		if (isFindingServer) return; // already looking for server
		
		isFindingServer = true;
		
		startReceiverThread();
		startBroadcastThread();
		
		// wait for discovery time before killing search
		if (timer != null) {
			FAIMSLog.log("WARNING: already looking for server");
		}
		
		timer = new Timer();
		timer.schedule(new TimerTask() {

			@Override
			public void run() {
				FAIMSLog.log();
				
				killThreads();
			}
			
		}, getDiscoveryTime());
	}
	
	private void killThreads() {
		FAIMSLog.log();
		
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		
		// TODO check if this list needs to be synchronized using Collections.synchronizedList
		synchronized(listenerList) {
			while(!listenerList.isEmpty()) {
				IServerDiscovery.ServerDiscoveryListener listener = listenerList.pop();
				listener.handleDiscoveryResponse(isServerHostValid());
			}
		}
		
		isFindingServer = false;
	}
	
	private void startReceiverThread() {
		FAIMSLog.log();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket(getDevicePort());
					
					while(isFindingServer) {
						receivePacket(socket);
						
						if (isServerHostValid()) {
							killThreads();
						}
					}
						
				} catch(SocketException e) {
					FAIMSLog.log(e);
				} catch(IOException e) {
					FAIMSLog.log(e);
				} finally {
					if (socket != null) socket.close();
				}
			}
		}).start();
		
	}
	
	private void startBroadcastThread() {
		FAIMSLog.log();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				try {
					
					while(isFindingServer) {
						sendBroadcast();
						Thread.sleep(1000);
					}
						
				} catch(SocketException e) {
					FAIMSLog.log(e);
				} catch(IOException e) {
					FAIMSLog.log(e);
				} catch(InterruptedException e) {
					FAIMSLog.log(e);
				}
			}
		}).start();
	}
	
	private void sendBroadcast() throws SocketException, IOException {
		FAIMSLog.log();
		
		DatagramSocket s = new DatagramSocket();
		try {
	    	s.setBroadcast(true);
	    	
	    	String packet = JsonUtil.serializeServerPacket(getIPAddress(), String.valueOf(getDevicePort()));
	    	int length = packet.length();
	    	byte[] message = packet.getBytes();
	    	
	    	DatagramPacket p = new DatagramPacket(message, length, InetAddress.getByName(getBroadcastAddr()), getDiscoveryPort());
	    	
	    	s.send(p);
	    	
	    	FAIMSLog.log("AndroidIP: " + getIPAddress());
	    	FAIMSLog.log("AndroidPort: " + getDevicePort());
		} finally {
			s.close();
		}
	}
	
	private void receivePacket(DatagramSocket r) throws SocketException, IOException {
		FAIMSLog.log();
		
		//DatagramSocket r = new DatagramSocket(getDevicePort());
		try {
			r.setSoTimeout(getPacketTimeout());
			
	    	byte[] buffer = new byte[1024];
	    	DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	    	
	        r.receive(packet);
	       
	        JsonObject data = JsonUtil.deserializeServerPacket(getPacketDataAsString(packet));
	        
	        if (data.has("server_ip"))
	        	serverIP = data.get("server_ip").getAsString();
	        if (data.has("server_port"))
	        	serverPort = data.get("server_port").getAsString();
	        
	        FAIMSLog.log("ServerIP: " + serverIP);
	        FAIMSLog.log("ServerPort: " + serverPort);
		} finally {
			//r.close();
		}
	}
	
	private String getIPAddress() throws IOException {
		WifiManager wifiManager = (WifiManager) application.getSystemService(Application.WIFI_SERVICE);
    	DhcpInfo myDhcpInfo = wifiManager.getDhcpInfo();
    	if (myDhcpInfo == null) {
    		FAIMSLog.log("could not determine device ip");
    		return null;
    	}
    	int broadcast = myDhcpInfo.ipAddress;
		byte[] quads = new byte[4];
		for (int k = 0; k < 4; k++)
		quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
		return InetAddress.getByAddress(quads).getHostAddress();
    }
	
	private int getDiscoveryTime() {
		return application.getResources().getInteger(R.integer.discovery_time) * 1000;
	}
	
	private int getPacketTimeout() {
		return application.getResources().getInteger(R.integer.packet_timeout) * 1000;
	}
	
	private int getDiscoveryPort() {
		return application.getResources().getInteger(R.integer.discovery_port);
	}
	
	private int getDevicePort() {
		return application.getResources().getInteger(R.integer.device_port);
	}
	
	private String getBroadcastAddr() {
		return application.getResources().getString(R.string.broadcast_addr);
	}
	
	private String getPacketDataAsString(DatagramPacket packet) throws IOException {
		FAIMSLog.log();
		
		InputStreamReader reader = null;
		try {
			 reader = new InputStreamReader(new ByteArrayInputStream(packet.getData()), Charset.forName("UTF-8"));
		     StringBuilder sb = new StringBuilder();
		     int value;
		     while((value = reader.read()) > 0)
		    	 sb.append((char) value);
		     
		     return sb.toString();
		 } finally {
			 if (reader != null) reader.close();
		 }
	}
	
	public void clearListeners() {
		listenerList.clear();
	}

}
