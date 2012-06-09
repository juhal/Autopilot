package fi.lindstromit.andropilot;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Handler;

public class KalmanController implements Runnable, OnSharedPreferenceChangeListener {

	private AndropilotActivity andropilot;
	private float target;
	private float course;
	private float speed;
	private boolean courseUpdated;
	private Handler handler;
		
	// state model
	// x1(t+dt) = x1(t) + a1*v*x2(t)
	// x2(t+dt) = x2(t) + u(t)	
	private float x1; // heading deviation in degrees
	private float x2; // "peräsinkulma"
	private float p1, p2, p3; // state covariance matrix
	private float q1, q2, r; // process and measurement noise
	private float a1; // see state model
	private float g1, g2; // control gains 
	private int u; // control output
	
	public KalmanController(AndropilotActivity andropilot) {
		this.andropilot = andropilot;
		this.handler = new Handler();
		this.andropilot.getConfig().registerOnSharedPreferenceChangeListener(this);
		updateParameters();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		updateParameters();
	}
		
	private void updateParameters() {
		SharedPreferences cfg = this.andropilot.getConfig();
		a1 = Float.parseFloat(cfg.getString("a1", "1.0"));
		q1 = Float.parseFloat(cfg.getString("q1", "1.0"));
		q2 = Float.parseFloat(cfg.getString("q2", "1.0"));
		r = Float.parseFloat(cfg.getString("r", "1.0"));
		g1 = Float.parseFloat(cfg.getString("g1", "1.0"));
		g2 = Float.parseFloat(cfg.getString("g2", "1.0"));
	}
	
	public void start() {
		course = andropilot.getCurrentCourse();
		target = andropilot.getTargetCourse();
		x1 = deltaAngle(course, target);
		x2 = 0;
		p1 = p2 = 100;
		p3 = 0;
		updateParameters();
		handler.postDelayed(this, 0);
	}

	public void stop() {
		this.handler.removeCallbacks(this);
	}

	public void onTargetChanged(float targetCourse) {
		this.target = targetCourse;
	}

	public void onCourseUpdated(float course) {
		courseUpdated = true;
		this.course = course;
	}

	public void onSpeedUpdated(float speed) {
		this.speed = speed;
	}
	
	public void run() {
		this.handler.postDelayed(this, 500);
		control();
	}
	
	private float deltaAngle(float a1, float a2) {
		if (Math.abs(a1) > 360) a1 = (float)Math.IEEEremainder(a1, 360);
		if (Math.abs(a2) > 360) a2 = (float)Math.IEEEremainder(a2, 360);
		if (a1 < 0) a1 += 360;
		if (a2 < 0) a2 += 360;
		float da = a1 - a2;
		if (da > 180) {
			da -= 360;
		} else if (da < -180) {
			da += 360;
		}
		return da;
	}
	
	private void control() {
		float b = a1 * speed;
		float x1p = x1 + b * x2;
		float x2p = x2 + u;
		float p1p = p1 + b*(2*p3+b*p2) + q1;
		float p2p = p2 + q2;
		float p3p = p3 + b*p2;
				
		if (courseUpdated) {
			courseUpdated = false;
			// measurement
			float z = deltaAngle(course, target);
			// innovation
			float y = z - x1p;
			// Kalman gain divisor
			float s = p1p + r;
			// Kalman gain
			float k1 = p1p/s;
			float k2 = p3p/s;
			// Updated state
			x1 = x1p + k1*y;
			x2 = x2p + k2*y;
			// Updated state covariance matrix 
			p1 = p1p*(1-k1);
			p2 = p2p-k2*p3p;
			p3 = p3p*(1-k1);
		} else {
			x1 = x1p;
			x2 = x2p;
			p1 = p1p;
			p2 = p2p;
			p3 = p3p;
		}

		u = Math.round(g1*x1 + g2*x2);		
		if(u < -255) u = -255;
		if(u > 255) u = 255;		
		this.andropilot.setDrivePower(u);
	}
}
