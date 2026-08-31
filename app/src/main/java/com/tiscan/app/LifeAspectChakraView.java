package com.tiscan.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * The Shakti-Chakra-style 16-zone wheel — life aspect (outer ring) + element
 * (inner ring), read live from LifeAspectZones. Same Canvas/drawArc technique
 * as VastuChakraView (no bitmaps, no external assets), deliberately different
 * visual language: flat two-ring wedges with a gold/cyan/obsidian palette, no
 * scalloped ring edges, no pastel fill. See RECONSTRUCTION_NOTES.md for why
 * that distinction is there on purpose.
 */
public class LifeAspectChakraView extends View {
    private static final String[] SECTORS16 = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

    private final Paint p;
    private int selected = -1;
    private LifeAspectZones zones;
    private Listener listener;

    public interface Listener {
        void selected(String sector16);
    }

    public LifeAspectChakraView(Context c) {
        super(c);
        this.p = new Paint(1);
    }

    public void setZones(LifeAspectZones z) {
        this.zones = z;
        invalidate();
    }

    public void setListener(Listener l) {
        this.listener = l;
    }

    public void setHeading(float trueHeadingDeg) {
        this.selected = sectorIndex(trueHeadingDeg);
        invalidate();
    }

    private static int sectorIndex(float deg) {
        float n = VastuDirection.normalize(deg);
        return Math.round(n / 22.5f) % 16;
    }

    @Override
    protected void onDraw(Canvas c) {
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float rOuter = Math.min(getWidth(), getHeight()) * 0.46f;
        float rInner = rOuter * 0.64f;
        float rCore = rOuter * 0.32f;

        for (int i = 0; i < 16; i++) {
            float start = (i * 22.5f) - 11.25f;
            boolean sel = (i == this.selected);
            LifeAspectZones.Zone z = this.zones != null ? this.zones.get(SECTORS16[i]) : null;

            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(sel ? 0xFFD9B36C : (i % 2 == 0 ? 0xFF15251F : 0xFF122019));
            drawRingSegment(c, cx, cy, rInner, rOuter, start, 22.5f);

            this.p.setColor(elementColor(z != null ? z.element : ""));
            drawRingSegment(c, cx, cy, rCore, rInner, start, 22.5f);
        }

        this.p.setStyle(Paint.Style.STROKE);
        this.p.setStrokeWidth(2f);
        this.p.setColor(0xFF0A100D);
        for (int i = 0; i < 16; i++) {
            float bearing = (i * 22.5f) - 11.25f - 90f;
            double a = Math.toRadians(bearing);
            c.drawLine(cx + ((float) Math.cos(a) * rCore), cy + ((float) Math.sin(a) * rCore),
                cx + ((float) Math.cos(a) * rOuter), cy + ((float) Math.sin(a) * rOuter), this.p);
        }
        c.drawCircle(cx, cy, rCore, this.p);
        c.drawCircle(cx, cy, rInner, this.p);
        c.drawCircle(cx, cy, rOuter, this.p);

        this.p.setStyle(Paint.Style.FILL);
        this.p.setTextAlign(Paint.Align.CENTER);
        this.p.setColor(0xFFEDF3EF);
        for (int i = 0; i < 16; i++) {
            LifeAspectZones.Zone z = this.zones != null ? this.zones.get(SECTORS16[i]) : null;
            if (z == null) continue;
            float bearing = (i * 22.5f) - 90f;
            double a = Math.toRadians(bearing);
            float lx = cx + ((float) Math.cos(a) * rOuter * 0.82f);
            float ly = cy + ((float) Math.sin(a) * rOuter * 0.82f);
            this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 9.5f);
            c.drawText(z.aspectEn, lx, ly, this.p);
        }

        this.p.setTextAlign(Paint.Align.CENTER);
        if (this.selected >= 0 && this.zones != null) {
            LifeAspectZones.Zone z = this.zones.get(SECTORS16[this.selected]);
            if (z != null) {
                this.p.setColor(0xFFD9B36C);
                this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 15f);
                c.drawText(SECTORS16[this.selected], cx, cy - 6f, this.p);
                this.p.setColor(0xFF4FE3C8);
                this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 10f);
                c.drawText(z.element + " · " + z.dosha, cx, cy + 12f, this.p);
            }
        } else {
            this.p.setColor(0xFF8A9992);
            this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 12f);
            c.drawText("Tap a zone", cx, cy, this.p);
        }
    }

    private void drawRingSegment(Canvas c, float cx, float cy, float rIn, float rOut, float startBearing, float sweep) {
        RectF outer = new RectF(cx - rOut, cy - rOut, cx + rOut, cy + rOut);
        RectF inner = new RectF(cx - rIn, cy - rIn, cx + rIn, cy + rIn);
        Path path = new Path();
        path.arcTo(outer, startBearing - 90f, sweep);
        path.arcTo(inner, (startBearing - 90f) + sweep, -sweep);
        path.close();
        c.drawPath(path, this.p);
    }

    private static int elementColor(String element) {
        switch (element) {
            case "Water": return 0xFF2E5F8A;
            case "Fire": return 0xFF8A3A2E;
            case "Earth": return 0xFF6B5A2E;
            case "Air": return 0xFF3A6B5A;
            default: return 0xFF223026;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != 0) return true;
        float dx = e.getX() - (getWidth() / 2.0f);
        float dy = e.getY() - (getHeight() / 2.0f);
        float deg = VastuDirection.normalize((float) Math.toDegrees(Math.atan2(dx, -dy)));
        this.selected = sectorIndex(deg);
        invalidate();
        if (this.listener != null) {
            this.listener.selected(SECTORS16[this.selected]);
        }
        return true;
    }
}
