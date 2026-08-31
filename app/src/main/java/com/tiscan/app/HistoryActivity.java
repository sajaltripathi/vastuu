package com.tiscan.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.media3.common.MimeTypes;
import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class HistoryActivity extends ComponentActivity {
    private LinearLayout list;
    private String pid;
    private SurveySessionStore store;

    private Button b(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(10.0f);
        return b;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refresh$0(JSONObject s, View x) {
        openVideo(s);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refresh$1(JSONObject s, View x) {
        try {
            Uri u = this.store.csv(s);
            Intent sh = new Intent("android.intent.action.SEND");
            sh.setType("text/csv");
            sh.putExtra("android.intent.extra.STREAM", u);
            sh.addFlags(1);
            startActivity(Intent.createChooser(sh, "Share CSV"));
        } catch (Exception e) {
            toast(e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refresh$2(File f, View x) {
        Intent i = new Intent(this, (Class<?>) FloorPlanActivity.class);
        i.putExtra("sessionFile", f.getAbsolutePath());
        startActivity(i);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$refresh$3(File f, View x) {
        Intent i = new Intent(this, (Class<?>) ReportActivity.class);
        i.putExtra("sessionFile", f.getAbsolutePath());
        startActivity(i);
    }

    private void openVideo(JSONObject s) {
        String u = s.optString("overlayVideoUri", "");
        if (u.isEmpty() || u.equals("null")) {
            u = s.optString("videoUri", "");
        }
        if (u.isEmpty() || u.equals("null")) {
            toast("No video");
            return;
        }
        try {
            Intent i = new Intent("android.intent.action.VIEW");
            i.setDataAndType(Uri.parse(u), MimeTypes.VIDEO_MP4);
            i.addFlags(1);
            startActivity(i);
        } catch (Exception e) {
            toast("No video player available");
        }
    }

    private void refresh() {
        this.list.removeAllViews();
        for (final File f : this.store.list(this.pid)) {
            try {
                final JSONObject s = this.store.read(f);
                LinearLayout c = new LinearLayout(this);
                c.setOrientation(1);
                c.setPadding(10, 8, 10, 8);
                c.setBackgroundColor(-14079703);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
                int length = 0;
                cp.setMargins(0, 8, 0, 0);
                c.setLayoutParams(cp);
                c.addView(t(s.optString("propertyName") + " • " + DateFormat.getDateTimeInstance().format(new Date(s.optLong("startedAtEpochMs"))), 16, -1));
                JSONArray a = s.optJSONArray("markers");
                StringBuilder sb = new StringBuilder();
                if (a != null) {
                    length = a.length();
                }
                c.addView(t(sb.append(length).append(" markers").toString(), 13, -3355444));
                LinearLayout actions = new LinearLayout(this);
                Button v = b("VIDEO");
                v.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.HistoryActivity$$ExternalSyntheticLambda0
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.lambda$refresh$0(s, view);
                    }
                });
                actions.addView(v);
                Button csv = b("CSV");
                csv.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.HistoryActivity$$ExternalSyntheticLambda1
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.lambda$refresh$1(s, view);
                    }
                });
                actions.addView(csv);
                Button fl = b("PLAN");
                fl.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.HistoryActivity$$ExternalSyntheticLambda2
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.lambda$refresh$2(f, view);
                    }
                });
                actions.addView(fl);
                Button pdf = b("PDF");
                pdf.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.HistoryActivity$$ExternalSyntheticLambda3
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        this.f$0.lambda$refresh$3(f, view);
                    }
                });
                actions.addView(pdf);
                c.addView(actions);
                this.list.addView(c);
            } catch (Exception e) {
            }
        }
    }

    private TextView t(String s, int z, int c) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(z);
        t.setTextColor(c);
        return t;
    }

    private void toast(String s) {
        Toast.makeText(this, s, 1).show();
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        this.pid = getIntent().getStringExtra("propertyId");
        this.store = new SurveySessionStore(this);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(1);
        r.setPadding(12, 12, 12, 12);
        r.setBackgroundColor(-15658735);
        TextView h = t("Survey History", 24, -1);
        h.setTypeface(null, 1);
        r.addView(h);
        ScrollView sv = new ScrollView(this);
        this.list = new LinearLayout(this);
        this.list.setOrientation(1);
        sv.addView(this.list);
        r.addView(sv, new LinearLayout.LayoutParams(-1, 0, 1.0f));
        setContentView(r);
        refresh();
    }
}
