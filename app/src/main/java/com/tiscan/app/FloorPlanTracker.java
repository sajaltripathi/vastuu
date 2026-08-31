package com.tiscan.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class FloorPlanTracker implements SensorEventListener {
    private float heading;
    private final List<Pt> pts = new ArrayList();
    private boolean run;
    private final SensorManager sm;
    private final Sensor step;
    private float x;
    private float y;

    public static class Pt {
        float x;
        float y;

        Pt(float a, float b) {
            this.x = a;
            this.y = b;
        }
    }

    public FloorPlanTracker(Context c) {
        this.sm = (SensorManager) c.getSystemService("sensor");
        this.step = this.sm.getDefaultSensor(18);
        this.pts.add(new Pt(0.0f, 0.0f));
    }

    public void setHeading(float h) {
        this.heading = h;
    }

    public void start() {
        this.run = true;
        if (this.step != null) {
            this.sm.registerListener(this, this.step, 3);
        }
    }

    public void stop() {
        this.run = false;
        this.sm.unregisterListener(this);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor s, int a) {
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent e) {
        if (this.run && e.sensor.getType() == 18) {
            double r = Math.toRadians(this.heading);
            this.x += (float) (Math.sin(r) * 0.72d);
            this.y += (float) ((-Math.cos(r)) * 0.72d);
            this.pts.add(new Pt(this.x, this.y));
        }
    }

    public JSONArray json() {
        JSONArray a = new JSONArray();
        try {
            for (Pt p : this.pts) {
                JSONObject o = new JSONObject();
                o.put("x", p.x);
                o.put("y", p.y);
                a.put(o);
            }
        } catch (Exception e) {
        }
        return a;
    }
}
