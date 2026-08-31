package com.tiscan.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import androidx.exifinterface.media.ExifInterface;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class VastuOverlayView extends View {
    private String ai;
    private float heading;
    private final Paint p;
    private String scan;

    public VastuOverlayView(Context context) {
        super(context);
        this.p = new Paint(1);
        this.ai = "";
        this.scan = "SCANNING";
        setWillNotDraw(false);
    }

    public void setHeading(float h) {
        this.heading = VastuDirection.normalize(h);
        invalidate();
    }

    public void setAi(String value) {
        this.ai = value == null ? "" : value;
        invalidate();
    }

    public void setScanStatus(String value) {
        this.scan = value == null ? "" : value;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas c) {
        float fDp;
        Canvas canvas = c;
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2.0f;
        float cy = Math.max(dp(270.0f), Math.min(0.42f * h, h - dp(360.0f)));
        float r = Math.min(0.29f * w, dp(132.0f));
        this.p.setStyle(Paint.Style.FILL);
        this.p.setColor(-1476395008);
        canvas.drawCircle(cx, cy, dp(10.0f) + r, this.p);
        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(dp(3.0f));
        this.p.setColor(-218103809);
        canvas.drawCircle(cx, cy, r, this.p);
        int i = 0;
        while (i < 32) {
            double a = Math.toRadians(((((double) i) * 11.25d) - ((double) this.heading)) - 90.0d);
            if (i % 4 == 0) {
                fDp = dp(18.0f);
            } else {
                fDp = dp(i % 2 == 0 ? 12.0f : 7.0f);
            }
            float inner = r - fDp;
            canvas.drawLine((((float) Math.cos(a)) * inner) + cx, (((float) Math.sin(a)) * inner) + cy, (((float) Math.cos(a)) * r) + cx, (((float) Math.sin(a)) * r) + cy, this.p);
            i++;
            canvas = c;
        }
        this.p.setStyle(Paint.Style.FILL);
        this.p.setTypeface(Typeface.DEFAULT_BOLD);
        this.p.setTextAlign(Paint.Align.CENTER);
        this.p.setTextSize(dp(18.0f));
        this.p.setColor(-1);
        drawLabel(c, "N", 0.0f, cx, cy, r - dp(30.0f));
        drawLabel(c, ExifInterface.LONGITUDE_EAST, 90.0f, cx, cy, r - dp(30.0f));
        drawLabel(c, ExifInterface.LATITUDE_SOUTH, 180.0f, cx, cy, r - dp(30.0f));
        drawLabel(c, ExifInterface.LONGITUDE_WEST, 270.0f, cx, cy, r - dp(30.0f));
        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(dp(2.0f));
        this.p.setColor(-218103809);
        c.drawCircle(cx, cy, dp(34.0f), this.p);
        c.drawLine(cx - dp(52.0f), cy, cx - dp(17.0f), cy, this.p);
        c.drawLine(cx + dp(17.0f), cy, cx + dp(52.0f), cy, this.p);
        c.drawLine(cx, cy - dp(52.0f), cx, cy - dp(17.0f), this.p);
        c.drawLine(cx, cy + dp(17.0f), cx, cy + dp(52.0f), this.p);
        this.p.setStyle(Paint.Style.FILL);
        this.p.setTypeface(Typeface.DEFAULT_BOLD);
        this.p.setTextSize(dp(27.0f));
        this.p.setColor(-1);
        c.drawText(String.format(Locale.US, "%.0f° %s", Float.valueOf(this.heading), VastuDirection.sector(this.heading)), cx, cy + dp(72.0f), this.p);
        this.p.setTextSize(dp(15.0f));
        this.p.setColor(-10929);
        c.drawText(Vastu32.label(this.heading), cx, cy + dp(96.0f), this.p);
        this.p.setTextSize(dp(13.0f));
        this.p.setColor(-285212673);
        c.drawText(this.scan, cx, (cy - r) - dp(18.0f), this.p);
        if (!this.ai.isEmpty()) {
            this.p.setTextSize(dp(13.0f));
            this.p.setColor(-4589878);
            c.drawText(this.ai, cx, cy + r + dp(30.0f), this.p);
        }
    }

    private void drawLabel(Canvas c, String label, float bearing, float cx, float cy, float rr) {
        double a = Math.toRadians((bearing - this.heading) - 90.0f);
        c.drawText(label, (((float) Math.cos(a)) * rr) + cx, (((float) Math.sin(a)) * rr) + cy + dp(6.0f), this.p);
    }

    private float dp(float v) {
        return getResources().getDisplayMetrics().density * v;
    }
}
