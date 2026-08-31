package com.tiscan.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

/* JADX INFO: loaded from: classes3.dex */
public class VastuChakraView extends View {
    private Listener listener;
    private final Paint p;
    private int selected;

    public interface Listener {
        void selected(float f);
    }

    public VastuChakraView(Context c) {
        super(c);
        this.p = new Paint(1);
        this.selected = -1;
    }

    public void setListener(Listener x) {
        this.listener = x;
    }

    public void setHeading(float d) {
        this.selected = Vastu32.index(d);
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas c) {
        Paint paint;
        int i;
        int i2;
        float cx = getWidth() / 2.0f;
        float cy = getHeight() / 2.0f;
        float r = Math.min(getWidth(), getHeight()) * 0.44f;
        int i3 = 0;
        while (true) {
            paint = this.p;
            i = 32;
            if (i3 >= 32) {
                break;
            }
            if (i3 == this.selected) {
                i2 = -19712;
            } else {
                i2 = i3 % 2 == 0 ? -13421773 : -12303292;
            }
            paint.setColor(i2);
            this.p.setStyle(Paint.Style.FILL);
            float startBearing = (i3 * 11.25f) + 45.0f;
            c.drawArc(cx - r, cy - r, cx + r, cy + r, startBearing - 90.0f, 11.25f, true, this.p);
            i3++;
        }
        paint.setColor(-15658735);
        c.drawCircle(cx, cy, 0.34f * r, this.p);
        this.p.setTextAlign(Paint.Align.CENTER);
        int i4 = -1;
        this.p.setColor(-1);
        this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 17.0f);
        c.drawText(this.selected < 0 ? "Tap sector" : Vastu32.codeAtIndex(this.selected), cx, cy - 4.0f, this.p);
        if (this.selected >= 0) {
            this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 12.0f);
            this.p.setColor(-10929);
            c.drawText(Vastu32.nameAtIndex(this.selected), cx, 20.0f + cy, this.p);
        }
        int i5 = 0;
        while (i5 < i) {
            float d = Vastu32.centerDegreesAtIndex(i5);
            double a = Math.toRadians(d - 90.0f);
            this.p.setTextSize(getResources().getDisplayMetrics().scaledDensity * 10.0f);
            this.p.setColor(i4);
            c.drawText(Vastu32.codeAtIndex(i5), (((float) Math.cos(a)) * r * 0.72f) + cx, (((float) Math.sin(a)) * r * 0.72f) + cy, this.p);
            i5 += 4;
            i = 32;
            i4 = -1;
        }
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != 0) {
            return true;
        }
        float dx = e.getX() - (getWidth() / 2.0f);
        float dy = e.getY() - (getHeight() / 2.0f);
        float d = VastuDirection.normalize((float) Math.toDegrees(Math.atan2(dx, -dy)));
        this.selected = Vastu32.index(d);
        invalidate();
        if (this.listener != null) {
            this.listener.selected(d);
        }
        return true;
    }
}
