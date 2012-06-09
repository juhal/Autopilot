package fi.lindstromit.andropilot;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;

public class Controller implements Runnable, OnSharedPreferenceChangeListener {

	private AndropilotActivity andropilot;
	private float target;
	private float course;
	private float speed;
	private boolean courseUpdated;
	private Handler handler;
	private StEstContr controlAlgorithm;
	private boolean targetHeadingChanged;
	private float dt;
		
	public Controller(AndropilotActivity andropilot) {
		this.andropilot = andropilot;
		this.handler = new Handler();
		this.andropilot.getConfig().registerOnSharedPreferenceChangeListener(this);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		updateParameters();
	}
		
	private void updateParameters() {
		SharedPreferences cfg = this.andropilot.getConfig();
		dt = Float.parseFloat(cfg.getString("dt", "0.5"));
		float tp = Float.parseFloat(cfg.getString("tp", "2.5"));
		float tv = Float.parseFloat(cfg.getString("tv", "50"));		
		StEstContr ca = new StEstContr(dt, 255, tp, tv);
		ca.q1 = Float.parseFloat(cfg.getString("q1", "2.0"));
		ca.r = Float.parseFloat(cfg.getString("r", "2.0"));
		ca.g1 = Float.parseFloat(cfg.getString("g1", "15.0"));
		this.controlAlgorithm = ca;
		this.targetHeadingChanged = true;
	}
	
	public void start() {
		speed = andropilot.getCurrentSpeed();
		course = andropilot.getCurrentCourse();
		target = andropilot.getTargetCourse();
		updateParameters();
		handler.postDelayed(this, 0);
	}

	public void stop() {
		handler.removeCallbacks(this);
	}

	public void onTargetChanged(float targetCourse) {
		this.target = targetCourse;
		this.targetHeadingChanged = true;
	}

	public void onCourseUpdated(float course) {
		this.course = course;
		this.courseUpdated = true;
	}

	public void onSpeedUpdated(float speed) {
		this.speed = speed;
	}
	
	public void run() {
		handler.postDelayed(this, (int)(dt*1000));
		int u = controlAlgorithm.headingControl(
				deltaAngle(course, target), speed, 
				courseUpdated, targetHeadingChanged);
		this.targetHeadingChanged = false;		
		andropilot.setDrivePower(u);
	}
	
	private float deltaAngle(float a1, float a2) {
		float da = a1 - a2;
		if (Math.abs(da) > 360) 
			da = (float)Math.IEEEremainder(da, 360);
		if (da > 180) return da - 360;
		if (da < -180) return da + 360;
		return da;
	}		
}
