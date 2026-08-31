package com.tiscan.app;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import java.io.File;
import java.io.OutputStream;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ReportActivity extends ComponentActivity {
    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            make(new SurveySessionStore(this).read(new File(getIntent().getStringExtra("sessionFile"))));
        } catch (Exception e) {
            Toast.makeText(this, "PDF failed: " + e.getMessage(), 1).show();
            finish();
        }
    }

    private void make(JSONObject s) throws Exception {
        String str;
        char c;
        PdfDocument pdf = new PdfDocument();
        Paint p = new Paint(1);
        int pageNo = 1;
        int i = 842;
        PdfDocument.Page page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas c2 = page.getCanvas();
        p.setTextSize(22.0f);
        p.setFakeBoldText(true);
        c2.drawText("Vastu Property Survey Report", 40.0f, 55, p);
        int y = 55 + 34;
        p.setTextSize(12.0f);
        p.setFakeBoldText(false);
        c2.drawText("Property: " + s.optString("propertyName"), 40.0f, y, p);
        int y2 = y + 18;
        c2.drawText("Address: " + s.optString("propertyAddress"), 40.0f, y2, p);
        int y3 = y2 + 18;
        String str2 = "sessionId";
        c2.drawText("Session: " + s.optString("sessionId"), 40.0f, y3, p);
        int y4 = y3 + 22;
        JSONArray markersForScore = s.optJSONArray("markers");
        VastuRuleEngine.ScoreResult scoreResult = new VastuRuleEngine(this).scoreMarkers(markersForScore);
        p.setFakeBoldText(true);
        p.setTextSize(14.0f);
        c2.drawText(scoreResult.summaryLine(), 40.0f, y4, p);
        p.setFakeBoldText(false);
        p.setTextSize(12.0f);
        y4 += 26;
        File arFile = new ARScanStore(this).latest(s.optString("propertyId", null));
        if (arFile != null) {
            try {
                JSONObject ar = new ARScanStore(this).read(arFile);
                p.setFakeBoldText(true);
                c2.drawText("AR measured floor", 40.0f, y4, p);
                p.setFakeBoldText(false);
                int y5 = y4 + 18;
                c2.drawText(String.format(Locale.US, "Measured polygon area: %.2f m²", Double.valueOf(ar.optDouble("areaM2"))), 40.0f, y5, p);
                y4 = y5 + 16;
                c2.drawText(String.format(Locale.US, "ARCore plane estimate: %.2f m²", Double.valueOf(ar.optDouble("autoPlaneEstimateM2"))), 40.0f, y4, p);
                y4 += 22;
            } catch (Exception e) {
            }
        }
        c2.drawText("Compass/GPS/AR readings are measurements. AI room labels are confidence-based suggestions.", 40.0f, y4, p);
        int y6 = y4 + 25;
        JSONArray a = s.optJSONArray("markers");
        if (a != null) {
            int i2 = 0;
            while (i2 < a.length()) {
                if (y6 > 790) {
                    pdf.finishPage(page);
                    pageNo++;
                    page = pdf.startPage(new PdfDocument.PageInfo.Builder(595, i, pageNo).create());
                    c2 = page.getCanvas();
                    y6 = 55;
                }
                JSONObject m = a.getJSONObject(i2);
                String str3 = str2;
                float deg = (float) m.optDouble("magneticHeading");
                int pageNo2 = pageNo;
                PdfDocument.Page page2 = page;
                String z = String.format(Locale.US, "%d. %s • %.1f° %s • %s • %.1fs", Integer.valueOf(i2 + 1), m.optString("label"), Float.valueOf(deg), m.optString("sector"), Vastu32.label(deg), Float.valueOf(m.optLong("elapsedMs") / 1000.0f));
                c2.drawText(z, 40.0f, y6, p);
                y6 += 18;
                if (!m.optString("note").isEmpty()) {
                    c2.drawText("   Note: " + m.optString("note"), 40.0f, y6, p);
                    y6 += 18;
                }
                if (m.optString("aiSuggestion").isEmpty()) {
                    c = 0;
                } else {
                    c = 0;
                    c2.drawText("   AI: " + m.optString("aiSuggestion"), 40.0f, y6, p);
                    y6 += 18;
                }
                i2++;
                str2 = str3;
                pageNo = pageNo2;
                page = page2;
                i = 842;
            }
            str = str2;
        } else {
            str = "sessionId";
        }
        pdf.finishPage(page);
        ContentValues v = new ContentValues();
        v.put("_display_name", "VastuReport_" + s.optString(str) + ".pdf");
        v.put("mime_type", "application/pdf");
        v.put("relative_path", "Documents/VastuSurvey");
        Uri u = getContentResolver().insert(MediaStore.Files.getContentUri("external"), v);
        if (u == null) {
            throw new Exception("PDF output unavailable");
        }
        OutputStream out = getContentResolver().openOutputStream(u);
        try {
            pdf.writeTo(out);
            if (out != null) {
                out.close();
            }
            pdf.close();
            Toast.makeText(this, "PDF saved to Documents/VastuSurvey", 1).show();
            Intent sh = new Intent("android.intent.action.SEND");
            sh.setType("application/pdf");
            sh.putExtra("android.intent.extra.STREAM", u);
            sh.addFlags(1);
            startActivity(Intent.createChooser(sh, "Share Vastu report"));
            finish();
        } catch (Throwable th) {
            if (out == null) {
                throw th;
            }
            try {
                out.close();
                throw th;
            } catch (Throwable th2) {
                th.addSuppressed(th2);
                throw th;
            }
        }
    }
}
