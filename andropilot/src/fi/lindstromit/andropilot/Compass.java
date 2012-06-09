package fi.lindstromit.andropilot;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Compass implements SensorEventListener {
	
	private final AndropilotActivity andropilot;
	private final SensorManager sensorManager;
	private float[] gravity = new float[3];
	private float[] geomagnetic  = new float[3];
	private float[] R = new float[9];
	private float[] I = new float[9];
	private float[] orientation = new float[3];

	public Compass(AndropilotActivity andropilot) {
		this.andropilot = andropilot;
		this.sensorManager = (SensorManager)andropilot.getSystemService(
				Context.SENSOR_SERVICE);
	}
	
	public void open() {
		Sensor am = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(this, am, SensorManager.SENSOR_DELAY_NORMAL);
		Sensor mm = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	    sensorManager.registerListener(this, mm, SensorManager.SENSOR_DELAY_NORMAL);			
	}
	public void close() {
		sensorManager.unregisterListener(this);
	}
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, gravity, 0, 3);
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(event.values, 0, geomagnetic, 0, 3);
		}
		if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
			SensorManager.getOrientation(R, orientation);
			float azimuth = orientation[0] * 180 / (float)Math.PI - 90;
			if (azimuth < 0) azimuth += 360;
			andropilot.setCourse(azimuth);
		}
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}