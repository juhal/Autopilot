package fi.lindstromit.andropilot;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class AndropilotActivity extends Activity 
implements OnClickListener, OnSharedPreferenceChangeListener, LocationListener {
	
	private SensorManager sensorManager;
	private LocationManager locationManager;
	private State state;
	private float targetCourse;
	private float currentCourse;
	private TextView courseView;
	private TextView targetCourseView;
	private TextView driveView;
	private ToggleButton autoButton;
	private ToggleButton manualButton;
	private ToggleButton targetButton;
	private Compass compass;
	private Helm helm;
	private SharedPreferences prefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = new StateIdle();
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        setContentView(R.layout.main);
        driveView = (TextView)findViewById(R.id.driveView);
        targetCourseView = (TextView)findViewById(R.id.targetBearingView);
        courseView = (TextView)findViewById(R.id.bearingView);
        autoButton = (ToggleButton)findViewById(R.id.autoButton);
        manualButton = (ToggleButton)findViewById(R.id.manualButton);
        targetButton = (ToggleButton)findViewById(R.id.targetButton);
        autoButton.setOnClickListener(this);
        manualButton.setOnClickListener(this);
        targetButton.setOnClickListener(this);
        ((Button)findViewById(R.id.holdButton)).setOnClickListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }
    
    public SharedPreferences getConfig() {
    	return prefs;
    }
    
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if ("useCompass".equals(key)) {
			openCompass();
		} else if ("helmId".equals(key)) {
			openHelm();
		}
	}
	
	private void openCompass() {
		if (prefs.getBoolean("useCompass", false)) {
			if (compass == null) {
				compass = new Compass(this);
				compass.open();
			}
		} else {
			if (compass != null) {
				compass.close();
				compass = null;
			}
		}
	}
		
	private void openHelm() {
		if (helm != null) {
			helm.close();
			helm = null;
		}
		String helmId = prefs.getString("helmId", null);
		if (helmId == null) {
			Toast.makeText(this, "Autopilotin tunniste puuttuu.", 
					Toast.LENGTH_LONG).show();
			return;
		}
		helm = new Helm(helmId);
		try {
			helm.open();
		} catch (IOException e) {
			Log.w("andropilot", "opening helm connection failed", e);
			helm = null;
			Toast.makeText(this, "Autopilottiyhteys epäonnistui.", 
					Toast.LENGTH_LONG).show();
		}
	}
    
    @Override
    protected void onResume() {
        super.onResume();
        this.state.onEnter();
        openHelm();
        openCompass();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }
    
    @Override
    protected void onPause() {
    	locationManager.removeUpdates(this);
    	if (helm != null) {
    		helm.close();
    		helm = null;
    	}
    	if (compass != null) {
    		compass.close();
    		compass = null;
    	}
    	this.state.onExit();
    	super.onPause();    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	if (item.getItemId() == R.id.settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
    	return super.onOptionsItemSelected(item);
    }
    
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.holdButton:
			setTargetCourse(currentCourse);
			break;
		case R.id.autoButton:
			enter(state instanceof StateAutoPilot 
					? new StateIdle() : new StateAutoPilot());
			break;
		case R.id.manualButton:
			enter(state instanceof StateManual
					? new StateIdle() : new StateManual());
			break;
		case R.id.targetButton:
			enter(state instanceof StateSetTargetCourse
					? new StateIdle() : new StateSetTargetCourse());
			break;
		}
	}    
    
    public void setCourse(float course) {
    	currentCourse = course;
    	courseView.setText(String.valueOf(Math.round(course)));
    	this.state.onCourseUpdated(course);
    }
    
    public void setSpeed(float speed) { 
    	this.state.onSpeedUpdated(speed);
    }

	public void setDrivePower(int power) {
		if (power > 255) power = 255;
		else if (power < -255) power = -255;
		driveView.setText(String.valueOf(power));
		if (helm != null)
			helm.setDrivePower(power);
	}
	
	public void setTargetCourse(float course) {
		while (course > 360) course -= 360;
		while (course < 0) course += 360;
		targetCourse = course;
		targetCourseView.setText(String.valueOf(Math.round(targetCourse)));
		this.state.onTargetChanged(course);
	}
	
	public float getCurrentCourse() {
		return currentCourse;
	}

	public float getTargetCourse() {
		return targetCourse;
	}
	
	@Override
	public void onLocationChanged(Location location) {
		if (compass == null && location.hasBearing()) {
			setCourse(location.getBearing());
		}
		if (location.hasSpeed()) {
			setSpeed(location.getSpeed());
		}
	}
	@Override
	public void onProviderDisabled(String provider) {}
	@Override
	public void onProviderEnabled(String provider) {}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}	

	private void enter(State state) {
		if (this.state != null) {
			this.state.onExit();
		}
		this.state = state;
		this.state.onEnter();
	}	
	
	private abstract class State {
		public void onEnter() {}
		public void onExit() {}
		public void onTargetChanged(float targetCourse) {}
		public void onCourseUpdated(float course) {}
		public void onSpeedUpdated(float speed) {}
	}
	
	private class StateIdle extends State {}
	
	private class StateManual extends State implements SensorEventListener {		
		@Override
		public void onEnter() {
			manualButton.setChecked(true);
	        Sensor am = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	        sensorManager.registerListener(this, am, SensorManager.SENSOR_DELAY_NORMAL);			
		}
		@Override
		public void onExit() {
			setDrivePower(0);
			manualButton.setChecked(false);
			sensorManager.unregisterListener(this);
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			float ay = event.values[1];
			if (Math.abs(ay) < 1) ay = 0;
			setDrivePower((int)(ay/9.81*255));
		}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	}
	
	private class StateSetTargetCourse extends State implements SensorEventListener {
		@Override
		public void onEnter() {
			targetButton.setChecked(true);
	        Sensor am = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	        sensorManager.registerListener(this, am, SensorManager.SENSOR_DELAY_NORMAL);
	        
		}
		@Override
		public void onExit() {
			targetButton.setChecked(false);
			sensorManager.unregisterListener(this);
		}
		@Override
		public void onSensorChanged(SensorEvent event) {
			float ay = event.values[1];
			if (Math.abs(ay) > 1) {
				setTargetCourse(targetCourse + ay/10f);
			}
		}
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	}
	
	private class StateAutoPilot extends State {
		
		private KalmanController controller;
		
		@Override
		public void onEnter() {
			autoButton.setChecked(true);
			controller = new KalmanController(AndropilotActivity.this);
			controller.start();
		}
		@Override
		public void onExit() {
			controller.stop();
			autoButton.setChecked(false);
		}
		
		public void onTargetChanged(float targetCourse) {
			controller.onTargetChanged(targetCourse);
		}
		public void onCourseUpdated(float course) {
			controller.onCourseUpdated(course);
			
		}
		public void onSpeedUpdated(float speed) {
			controller.onSpeedUpdated(speed);
			
		}		
	}
}