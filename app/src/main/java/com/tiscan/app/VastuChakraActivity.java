package com.tiscan.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.ComponentActivity;

/* JADX INFO: loaded from: classes3.dex */
public class VastuChakraActivity extends ComponentActivity {
    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        float h = getIntent().getFloatExtra("heading", 0.0f);
        LifeAspectZones zones = new LifeAspectZones(this);

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(1);
        r.setBackgroundColor(-15658735);
        r.setPadding(12, 12, 12, 12);

        LinearLayout tabs = new LinearLayout(this);
        Button tab32 = new Button(this);
        tab32.setText("32-Pada Chakra");
        tab32.setAllCaps(false);
        Button tab16 = new Button(this);
        tab16.setText("16-Zone Shakti Wheel");
        tab16.setAllCaps(false);
        tabs.addView(tab32, new LinearLayout.LayoutParams(0, -2, 1.0f));
        tabs.addView(tab16, new LinearLayout.LayoutParams(0, -2, 1.0f));
        r.addView(tabs);

        TextView t = new TextView(this);
        t.setTextColor(-1);
        t.setTextSize(20.0f);
        t.setText("Interactive Vastu Chakra • 32 entrance sectors");
        r.addView(t);

        VastuChakraView v = new VastuChakraView(this);
        v.setHeading(h);
        r.addView(v, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        LifeAspectChakraView v16 = new LifeAspectChakraView(this);
        v16.setZones(zones);
        v16.setHeading(h);
        v16.setVisibility(8);
        r.addView(v16, new LinearLayout.LayoutParams(-1, 0, 1.0f));

        final TextView d = new TextView(this);
        d.setTextColor(-10929);
        d.setTextSize(17.0f);
        d.setText(Vastu32.description(h));
        r.addView(d);

        v.setListener(f -> d.setText(Vastu32.description(f)));
        v16.setListener(sector16 -> {
            LifeAspectZones.Zone z = zones.get(sector16);
            if (z != null) {
                d.setText(sector16 + " — " + z.aspectLine() + "\nElement: " + z.element + "  ·  Dosha: " + z.dosha);
            }
        });

        tab32.setOnClickListener(view -> {
            v.setVisibility(0);
            v16.setVisibility(8);
            t.setText("Interactive Vastu Chakra • 32 entrance sectors");
            d.setText(Vastu32.description(h));
        });
        tab16.setOnClickListener(view -> {
            v.setVisibility(8);
            v16.setVisibility(0);
            t.setText("Shakti Wheel • 16 life-aspect zones");
            LifeAspectZones.Zone z = zones.get(VastuDirection.sector(h));
            if (z != null) {
                d.setText(VastuDirection.sector(h) + " — " + z.aspectLine() + "\nElement: " + z.element + "  ·  Dosha: " + z.dosha);
            }
        });

        setContentView(r);
    }
}
