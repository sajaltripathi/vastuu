package com.tiscan.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.ComponentActivity;

/* JADX INFO: loaded from: classes3.dex */
public class PropertyActivity extends ComponentActivity {
    private LinearLayout list;
    private PropertyStore store;

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View v) {
        add();
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        this.store = new PropertyStore(this);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(1);
        r.setPadding(dp(14), dp(14), dp(14), dp(14));
        r.setBackgroundColor(-15658735);
        TextView h = t("Vastu Survey Properties", 26, -1);
        h.setTypeface(null, 1);
        r.addView(h);
        r.addView(t("Keep every property's surveys, videos, notes and reports separate.", 14, -3355444));
        Button add = new Button(this);
        add.setText("+ ADD PROPERTY");
        add.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.PropertyActivity$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$onCreate$0(view);
            }
        });
        r.addView(add);
        ScrollView sv = new ScrollView(this);
        this.list = new LinearLayout(this);
        this.list.setOrientation(1);
        sv.addView(this.list);
        r.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        setContentView(r);
        refresh();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refresh$1(PropertyStore.Property p, View v) {
        Intent i = new Intent(this, (Class<?>) MainActivity.class);
        i.putExtra("propertyId", p.id);
        startActivity(i);
    }

    private void refresh() {
        this.list.removeAllViews();
        for (final PropertyStore.Property p : this.store.all()) {
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(1);
            c.setPadding(dp(10), dp(8), dp(10), dp(8));
            c.setBackgroundColor(-14079703);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.setMargins(0, dp(8), 0, 0);
            c.setLayoutParams(cp);
            TextView n = t(p.name, 19, -1);
            n.setTypeface(null, 1);
            c.addView(n);
            if (!p.address.isEmpty()) {
                c.addView(t(p.address, 13, -3355444));
            }
            Button o = new Button(this);
            o.setText("OPEN PROPERTY");
            o.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.PropertyActivity$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    this.f$0.lambda$refresh$1(p, view);
                }
            });
            c.addView(o);
            this.list.addView(c);
        }
        if (this.store.all().isEmpty()) {
            this.list.addView(t("Add your first property to begin.", 16, -4473925));
        }
    }

    private void add() {
        LinearLayout f = new LinearLayout(this);
        f.setOrientation(1);
        f.setPadding(dp(16), 0, dp(16), 0);
        final EditText n = new EditText(this);
        n.setHint("Property name");
        final EditText a = new EditText(this);
        a.setHint("Address / locality");
        final EditText no = new EditText(this);
        no.setHint("Property notes");
        f.addView(n);
        f.addView(a);
        f.addView(no);
        new AlertDialog.Builder(this).setTitle("New property").setView(f).setPositiveButton("Create", new DialogInterface.OnClickListener() { // from class: com.tiscan.app.PropertyActivity$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$add$2(n, a, no, dialogInterface, i);
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$add$2(EditText n, EditText a, EditText no, DialogInterface d, int w) {
        String s = n.getText().toString().trim();
        if (s.isEmpty()) {
            s = "Property " + (this.store.all().size() + 1);
        }
        this.store.add(s, a.getText().toString().trim(), no.getText().toString().trim());
        refresh();
    }

    private int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    private TextView t(String s, int z, int c) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(z);
        v.setTextColor(c);
        return v;
    }
}
