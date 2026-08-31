package com.tiscan.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

/**
 * FIXED VERSION — see emit() for the change.
 *
 * Bug that was here: when the camera-forward vector pointed too close to straight
 * up/down (horizontal-plane magnitude near zero), atan2() becomes numerically
 * unstable and tiny sensor noise produced large heading swings. The old code
 * still folded those unstable samples into the low-pass filter every time,
 * just with a different alpha — so noise leaked into "filtered" even though
 * `cameraPoseUsable`/`reliable` correctly flagged the sample as untrustworthy.
 *
 * Fix: when cameraPoseUsable is false, skip updating the filter state entirely.
 * `filtered` now holds its last trustworthy value instead of drifting, and the
 * `reliable` flag (already wired to MainActivity) tells the UI to say so.
 */
public class CompassManager implements SensorEventListener {
    private final Sensor accelerometer;
    private float filtered;
    private final Sensor geomagneticRotationVector;
    private boolean haveAccel;
    private boolean haveMag;
    private boolean initialized;
    private long lastEmitMs;
    private float lastEmitted;
    private float lastRaw;
    private long lastRawMs;
    private final Listener listener;
    private final Sensor magnetometer;
    private boolean registered;
    private final Sensor rotationVector;
    private final SensorManager sm;
    private double smoothSin;
    private final float[] accel = new float[3];
    private final float[] mag = new float[3];
    private int accuracy = 0;
    private double smoothCos = 1.0d;
    private int lowAccuracyStreak = 0;
    private int goodAccuracyStreak = 0;
    private boolean accuracyOk = false;

    public interface Listener {
        void onHeading(float f, int i, boolean z);

        void onSensorUnavailable();
    }

    public CompassManager(Context context, Listener listener) {
        this.listener = listener;
        this.sm = (SensorManager) context.getSystemService("sensor");
        this.rotationVector = this.sm.getDefaultSensor(11);
        this.geomagneticRotationVector = this.sm.getDefaultSensor(20);
        this.accelerometer = this.sm.getDefaultSensor(1);
        this.magnetometer = this.sm.getDefaultSensor(2);
    }

    public synchronized void start() {
        if (this.registered) {
            return;
        }
        boolean z = true;
        if (this.rotationVector != null) {
            this.registered = this.sm.registerListener(this, this.rotationVector, 1);
            if (this.magnetometer != null) {
                this.sm.registerListener(this, this.magnetometer, 2);
            }
        } else if (this.geomagneticRotationVector != null) {
            this.registered = this.sm.registerListener(this, this.geomagneticRotationVector, 1);
            if (this.magnetometer != null) {
                this.sm.registerListener(this, this.magnetometer, 2);
            }
        } else if (this.accelerometer != null && this.magnetometer != null) {
            boolean a = this.sm.registerListener(this, this.accelerometer, 1);
            boolean m = this.sm.registerListener(this, this.magnetometer, 1);
            if (!a || !m) {
                z = false;
            }
            this.registered = z;
        }
        boolean a2 = this.registered;
        if (!a2) {
            this.listener.onSensorUnavailable();
        }
    }

    public synchronized void stop() {
        this.sm.unregisterListener(this);
        this.registered = false;
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        float[] r = new float[9];
        int type = event.sensor.getType();
        if (type == 11 || type == 20) {
            SensorManager.getRotationMatrixFromVector(r, event.values);
            if (type == 11 && event.values.length >= 5 && event.values[4] >= 0.0f) {
                float headingAccuracyDeg = (float) Math.toDegrees(event.values[4]);
                if (headingAccuracyDeg <= 10.0f) {
                    this.accuracy = 3;
                } else if (headingAccuracyDeg <= 20.0f) {
                    this.accuracy = 2;
                } else {
                    this.accuracy = 1;
                }
            }
            emit(r);
            return;
        }
        if (type == 1) {
            lowPass(event.values, this.accel);
            this.haveAccel = true;
        } else if (type == 2) {
            lowPass(event.values, this.mag);
            this.haveMag = true;
        }
        if (!this.haveAccel || !this.haveMag || !SensorManager.getRotationMatrix(r, null, this.accel, this.mag)) {
            return;
        }
        emit(r);
    }

    private void emit(float[] r) {
        double alpha;
        float east = -r[2];
        float north = -r[5];
        float horizontal = (float) Math.sqrt((east * east) + (north * north));
        boolean cameraPoseUsable = horizontal > 0.36f;
        long now = SystemClock.elapsedRealtime();
        float speedDegPerSec = 0.0f;

        if (cameraPoseUsable) {
            float raw = VastuDirection.normalize((float) Math.toDegrees(Math.atan2(east, north)));
            float rawDelta = this.lastRawMs == 0 ? 0.0f : angularDifference(this.lastRaw, raw);
            speedDegPerSec = this.lastRawMs == 0 ? 0.0f : (1000.0f * rawDelta) / Math.max(1L, now - this.lastRawMs);
            this.lastRaw = raw;
            this.lastRawMs = now;

            if (speedDegPerSec < 8.0f) {
                alpha = 0.055d;
            } else {
                alpha = speedDegPerSec < 35.0f ? 0.14d : 0.34d;
            }

            double rad = Math.toRadians(raw);
            if (!this.initialized) {
                this.smoothSin = Math.sin(rad);
                this.smoothCos = Math.cos(rad);
                this.filtered = raw;
                this.initialized = true;
            } else {
                this.smoothSin = ((1.0d - alpha) * this.smoothSin) + (Math.sin(rad) * alpha);
                this.smoothCos = ((1.0d - alpha) * this.smoothCos) + (Math.cos(rad) * alpha);
                this.filtered = VastuDirection.normalize((float) Math.toDegrees(Math.atan2(this.smoothSin, this.smoothCos)));
            }
        }
        // else: cameraPoseUsable is false (phone pointed too steeply up/down) — everything
        // above is intentionally skipped. `this.filtered` keeps its last trustworthy value.

        boolean rawAccurate = this.accuracy >= 2;
        if (rawAccurate) {
            this.goodAccuracyStreak++;
            this.lowAccuracyStreak = 0;
            if (this.goodAccuracyStreak >= 3) this.accuracyOk = true;
        } else {
            this.lowAccuracyStreak++;
            this.goodAccuracyStreak = 0;
            if (this.lowAccuracyStreak >= 2) this.accuracyOk = false;
        }
        boolean reliable = cameraPoseUsable && this.accuracyOk && this.initialized;
        float delta = angularDifference(this.lastEmitted, this.filtered);
        float threshold = speedDegPerSec < 8.0f ? 1.2f : 0.45f;
        long maxSilence = reliable ? 1200L : 400L;
        if (reliable && delta < threshold && now - this.lastEmitMs < maxSilence && this.lastEmitMs != 0) {
            return;
        }
        this.lastEmitted = this.filtered;
        this.lastEmitMs = now;
        this.listener.onHeading(this.filtered, this.accuracy, reliable);
    }

    private static float angularDifference(float a, float b) {
        float d = Math.abs(a - b) % 360.0f;
        return d > 180.0f ? 360.0f - d : d;
    }

    private static void lowPass(float[] input, float[] output) {
        for (int i = 0; i < 3; i++) {
            output[i] = output[i] + ((input[i] - output[i]) * 0.16f);
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int value) {
        int type = sensor.getType();
        if (type == 2 || type == 11 || type == 20) {
            this.accuracy = value;
        }
    }
}
