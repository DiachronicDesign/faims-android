package au.org.intersect.faims.android.tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import net.sf.marineapi.nmea.parser.DataNotAvailableException;
import net.sf.marineapi.nmea.parser.SentenceFactory;
import net.sf.marineapi.nmea.sentence.GGASentence;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class ExternalGPSTasks implements Runnable {

	private BluetoothDevice gpsDevice;
	private Handler handler;
	private BluetoothActionListener actionListener;
    private String GGAMessage;
    private String BODMessage;
    private int gpsUpdateInterval;
    private BluetoothSocket bluetoothSocket;
    private InputStream in;
    private InputStreamReader isr;
    private BufferedReader br;

    public ExternalGPSTasks(BluetoothDevice gpsDevice, Handler handler, BluetoothActionListener actionListener, int gpsUpdateInterval){
    	this.gpsDevice = gpsDevice;
    	this.handler = handler;
    	this.actionListener = actionListener;
    	this.gpsUpdateInterval = gpsUpdateInterval;
		try {
			if(this.gpsDevice != null){
				initialiseBluetoothSocket();
			}
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		} catch (IOException e) {
			System.out.println("failed");
			this.bluetoothSocket = null;
		}
    }

	@Override
	public void run() {
		handler.postDelayed(this, this.gpsUpdateInterval);
		System.out.println("run run run");
		readSentences();
		System.out.println("done sentences");
		this.actionListener.handleGPSUpdates(this.GGAMessage, this.BODMessage);
	}

	public void closeBluetoothConnection(){
		if(this.bluetoothSocket != null){
    		try {
    			br.close();
	            isr.close();
	            in.close();
				this.bluetoothSocket.close();
			} catch (IOException exception) {
			}
    	}
	}
	
	private void readSentences() {
        this.GGAMessage = null;
        this.BODMessage = null;
        if(this.gpsDevice!= null) System.out.println(this.gpsDevice.getName());
        if(this.gpsDevice != null){
	        try {
	            if(this.bluetoothSocket == null){
	            	System.out.println("null here");
	            	initialiseBluetoothSocket();
	            }
	            System.out.println("start stream");
	
	            long start = System.currentTimeMillis();
	            long end = start + 1000; // check for 0.5 seconds to get valid GPGGA message
	            while (System.currentTimeMillis() < end){
	                String nmeaMessage = br.readLine();
	                if (nmeaMessage.startsWith("$GPGGA")) {
	                    if(hasValidGGAMessage()){
	                        break;
	                    }else{
	                        this.GGAMessage = nmeaMessage;
	                        if(!hasValidGGAMessage()){
	                        	this.GGAMessage = null;
	                        }
	                    }
		            } else if (nmeaMessage.startsWith("$GPBOD")) {
	                    this.BODMessage = nmeaMessage;
	                }
	            }
	            System.out.println("done looping");
	        } catch (IOException e) {
	        	this.gpsDevice = null;
	        	if(this.bluetoothSocket != null){
	        		try {
	        			br.close();
	    	            isr.close();
	    	            in.close();
						this.bluetoothSocket.close();
						this.bluetoothSocket = null;
					} catch (IOException exception) {
					}
	        	}
	            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
	            if( adapter != null){
	                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
	                if (pairedDevices.size() > 0) {
	                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
	                        this.gpsDevice = bluetoothDevice;
	                        break;
	                    }
	                }
	            }
	            System.out.println("can not connect");
	        } catch (IllegalArgumentException e) {
	        } catch (NoSuchMethodException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
        }else{
        	this.gpsDevice = null;
        	BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if( adapter != null){
                Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice bluetoothDevice : pairedDevices) {
                        this.gpsDevice = bluetoothDevice;
                        break;
                    }
                }
            }
            System.out.println("no connection");
        }
    }

	private void initialiseBluetoothSocket() throws NoSuchMethodException,
			IllegalAccessException, InvocationTargetException, IOException {
		Method m = this.gpsDevice.getClass().getMethod("createRfcommSocket",
		        new Class[] { int.class });
		this.bluetoothSocket = (BluetoothSocket) m.invoke(
				this.gpsDevice, 1);
		this.bluetoothSocket.connect();
		in = bluetoothSocket.getInputStream();
		isr = new InputStreamReader(in);
		br = new BufferedReader(isr);
	}

	private boolean hasValidGGAMessage() {
        GGASentence sentence = null;
        if (this.GGAMessage != null) {
            sentence = (GGASentence) SentenceFactory.getInstance()
                    .createParser(this.GGAMessage);
        }
        try{
        	return this.GGAMessage != null && sentence != null && sentence.getPosition() != null;
        } catch (DataNotAvailableException e){
        	return false;
        }
    }

}