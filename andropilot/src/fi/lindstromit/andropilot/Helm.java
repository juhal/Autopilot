package fi.lindstromit.andropilot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

public class Helm extends Thread {
	
	private volatile boolean die;
	private String deviceName;
	private BluetoothDevice btd;
	private BlockingQueue<String> queue;
	
	public Helm(String deviceName) {
		this.deviceName = deviceName;
		queue = new ArrayBlockingQueue<String>(2);
	}
	
	public void open() throws IOException {
		BluetoothAdapter bta = BluetoothAdapter.getDefaultAdapter();
		if (bta == null) {
			throw new IOException("Bluetooth is not supported"); 
		}
		btd = null;
		for (BluetoothDevice dev : bta.getBondedDevices()) {
			if (deviceName.equals(dev.getName())) {
				btd = dev;
				break;
			}
		}
		if (btd == null) {
			throw new IOException("Bluetooth device " 
					+ deviceName + " is not bound");
		}
		start();
	}
	
	public void close() {
		this.die = true;
		this.interrupt();
		try {
			this.join(2000);
		} catch (InterruptedException ie) {
			// ignored
		}		
	}
	
	public void setDrivePower(int power) {
		if (power < -255) power = -255;
		else if (power > 255) power = 255;
		queue.offer("$J " + power + "\r");
	}
	
	public void run() {
		try {
			while (!die) {
				try {
					BluetoothSocket bts = btd.createRfcommSocketToServiceRecord(
							UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
							//UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee"));
					try {
						bts.connect();
						OutputStream out = bts.getOutputStream();
						try {
							Log.d("andropilot", "connected");
							while (true) {
								String msg = queue.take();
								out.write(msg.getBytes());
							}
						} finally {
							out.close();
						}
					} finally {
						bts.close();
						Log.d("andropilot", "connection closed");
					}
				} catch (Exception ex) {
					if (!die) {
						Log.e("andropilot", "unexpected exception", ex);
						Thread.sleep(1000); 
					}
				}
			}
		} catch (Throwable t) {
			Log.e("andropilot", "unknown error", t);
		} finally {
			Log.d("andropilot", "exiting");
		}
	}
}
