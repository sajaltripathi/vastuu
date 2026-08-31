package com.tiscan.app;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;
import androidx.camera.video.AudioStats;
import java.io.File;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class FloorPlanActivity extends ComponentActivity {
    private JSONArray points = new JSONArray();
    private boolean arMeasured = false;
    private double area = AudioStats.AUDIO_AMPLITUDE_NONE;
    private double autoArea = AudioStats.AUDIO_AMPLITUDE_NONE;

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String propertyId = getIntent().getStringExtra("propertyId");
        try {
            File ar = new ARScanStore(this).latest(propertyId);
            if (ar != null) {
                JSONObject o = new ARScanStore(this).read(ar);
                this.points = o.optJSONArray("corners");
                this.area = o.optDouble("areaM2");
                this.autoArea = o.optDouble("autoPlaneEstimateM2");
                this.arMeasured = this.points != null && this.points.length() >= 3;
            }
            if (!this.arMeasured && getIntent().getStringExtra("sessionFile") != null) {
                this.points = new SurveySessionStore(this).read(new File(getIntent().getStringExtra("sessionFile"))).optJSONArray("floorPath");
            }
            if (this.points == null) {
                this.points = new JSONArray();
            }
        } catch (Exception e) {
        }
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(-15658735);
        TextView title = t(this.arMeasured ? "AR Measured Floor Plan" : "Schematic Floor Plan", 24, -1);
        root.addView(title);
        TextView note = t(this.arMeasured ? String.format(Locale.US, "Measured polygon: %.2f m² • ARCore floor-plane estimate: %.2f m²", Double.valueOf(this.area), Double.valueOf(this.autoArea)) : "No AR polygon found. Showing step/compass path only.", 14, this.arMeasured ? -4589878 : -16121);
        root.addView(note);
        TextView caveat = t(this.arMeasured ? "AR measurements are estimates and should be verified for construction/legal use." : "Step-path plan is orientation-only and is not an architectural survey.", 12, -4473925);
        root.addView(caveat);
        root.addView(new PlanView(), new LinearLayout.LayoutParams(-1, 0, 1.0f));
        setContentView(root);
    }

    class PlanView extends View {
        private final Paint p;

        PlanView() {
            super(FloorPlanActivity.this);
            this.p = new Paint(1);
        }

        @Override // android.view.View
        protected void onDraw(Canvas c) {
            FloorPlanActivity floorPlanActivity;
            Canvas canvas;
            float x;
            int i;
            float y;
            c.drawColor(-14671840);
            if (FloorPlanActivity.this.points.length() < 1) {
                return;
            }
            float minX = Float.MAX_VALUE;
            int i2 = 0;
            float maxX = -3.4028235E38f;
            float minY = Float.MAX_VALUE;
            float maxY = -3.4028235E38f;
            while (true) {
                try {
                    String str = "z";
                    if (i2 >= FloorPlanActivity.this.points.length()) {
                        break;
                    }
                    JSONObject o = FloorPlanActivity.this.points.getJSONObject(i2);
                    float x2 = (float) o.getDouble("x");
                    if (!FloorPlanActivity.this.arMeasured) {
                        str = "y";
                    }
                    float y2 = (float) o.optDouble(str);
                    minX = Math.min(minX, x2);
                    maxX = Math.max(maxX, x2);
                    minY = Math.min(minY, y2);
                    maxY = Math.max(maxY, y2);
                    i2++;
                } catch (Exception e) {
                    return;
                }
            }
            float sx = (getWidth() - 90.0f) / Math.max(1.0f, maxX - minX);
            float sy = (getHeight() - 130.0f) / Math.max(1.0f, maxY - minY);
            float scale = Math.min(sx, sy);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(6.0f);
            this.p.setColor(-10929);
            float firstX = 0.0f;
            float firstY = 0.0f;
            float lastX = 0.0f;
            float lastY = 0.0f;
            int i3 = 0;
            while (true) {
                int length = FloorPlanActivity.this.points.length();
                floorPlanActivity = FloorPlanActivity.this;
                if (i3 >= length) {
                    break;
                }
                try {
                    JSONObject o2 = floorPlanActivity.points.getJSONObject(i3);
                    float lastX2 = lastX;
                    float x3 = ((((float) o2.getDouble("x")) - minX) * scale) + 45.0f;
                    float y3 = ((((float) o2.optDouble(FloorPlanActivity.this.arMeasured ? "z" : "y")) - minY) * scale) + 65.0f;
                    if (i3 == 0) {
                        firstY = y3;
                        y = y3;
                        firstX = x3;
                        i = i3;
                        x = x3;
                    } else {
                        int i4 = i3;
                        x = x3;
                        i = i4;
                        y = y3;
                        c.drawLine(lastX2, lastY, x, y, this.p);
                    }
                    lastX = x;
                    lastY = y;
                    i3 = i + 1;
                } catch (Exception e2) {
                    return;
                }
                return;
            }
            if (!floorPlanActivity.arMeasured || FloorPlanActivity.this.points.length() < 3) {
                canvas = c;
            } else {
                canvas = c;
                canvas.drawLine(lastX, lastY, firstX, firstY, this.p);
            }
            this.p.setStyle(Paint.Style.FILL);
            this.p.setTextSize(28.0f);
            this.p.setColor(-1);
            canvas.drawText("N ↑", getWidth() - 75, 40.0f, this.p);
        }
    }

    private TextView t(String s, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }
}
