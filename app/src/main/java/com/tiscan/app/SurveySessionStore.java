package com.tiscan.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class SurveySessionStore {
    private final Context c;

    public SurveySessionStore(Context c) {
        this.c = c;
    }

    public File save(String id, long started, PropertyStore.Property p, String rawVideo, String overlayVideo, String screenshot, String surveyNote, List<SurveyMarker> ms, JSONArray floor) throws Exception {
        JSONObject r = new JSONObject();
        r.put("schemaVersion", 2);
        r.put("sessionId", id);
        r.put("startedAtEpochMs", started);
        r.put("savedAtEpochMs", System.currentTimeMillis());
        r.put("propertyId", p.id);
        r.put("propertyName", p.name);
        r.put("propertyAddress", p.address);
        r.put("propertyNotes", p.notes);
        r.put("surveyNote", surveyNote);
        r.put("videoUri", rawVideo == null ? JSONObject.NULL : rawVideo);
        r.put("overlayVideoUri", overlayVideo == null ? JSONObject.NULL : overlayVideo);
        r.put("screenshotUri", screenshot == null ? JSONObject.NULL : screenshot);
        JSONArray a = new JSONArray();
        for (SurveyMarker m : ms) {
            a.put(m.json());
        }
        r.put("markers", a);
        r.put("floorPath", floor == null ? new JSONArray() : floor);
        File d = new File(this.c.getFilesDir(), "sessions");
        if (!d.exists()) {
            d.mkdirs();
        }
        File f = new File(d, id + ".json");
        FileOutputStream o = new FileOutputStream(f);
        try {
            o.write(r.toString(2).getBytes(StandardCharsets.UTF_8));
            o.close();
            return f;
        } catch (Throwable th) {
            try {
                o.close();
                throw th;
            } catch (Throwable th2) {
                th.addSuppressed(th2);
                throw th;
            }
        }
    }

    /* JADX WARN: Code duplicated, block: B:10:0x0035 A[Catch: Exception -> 0x0039, TRY_LEAVE, TryCatch #0 {Exception -> 0x0039, blocks: (B:8:0x0025, B:10:0x0035), top: B:16:0x0025 }] */
    public List<File> list(String pid) {
        List<File> out = new ArrayList<>();
        File d = new File(this.c.getFilesDir(), "sessions");
        File[] fs = d.listFiles(new FilenameFilter() { // from class: com.tiscan.app.SurveySessionStore$$ExternalSyntheticLambda0
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                return str.endsWith(".json");
            }
        });
        if (fs != null) {
            for (File f : fs) {
                if (pid != null) {
                    try {
                        if (pid.equals(read(f).optString("propertyId"))) {
                            out.add(f);
                        }
                    } catch (Exception e) {
                    }
                } else {
                    out.add(f);
                }
            }
        }
        out.sort(new Comparator() { // from class: com.tiscan.app.SurveySessionStore$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return Long.compare(((File) obj2).lastModified(), ((File) obj).lastModified());
            }
        });
        return out;
    }

    public JSONObject read(File f) throws Exception {
        StringBuilder b = new StringBuilder();
        BufferedReader r = new BufferedReader(new FileReader(f));
        while (true) {
            try {
                String l = r.readLine();
                if (l == null) {
                    r.close();
                    return new JSONObject(b.toString());
                }
                b.append(l).append('\n');
            } catch (Throwable th) {
                try {
                    r.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }

    private static String q(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    public Uri csv(JSONObject s) throws Exception {
        ContentValues v = new ContentValues();
        v.put("_display_name", "VastuSurvey_" + s.optString("sessionId") + ".csv");
        v.put("mime_type", "text/csv");
        v.put("relative_path", "Download/VastuSurvey");
        Uri u = this.c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
        if (u == null) {
            throw new Exception("CSV output unavailable");
        }
        StringBuilder b = new StringBuilder("property,label,time_s,magnetic_deg,true_deg,direction16,entrance32,latitude,longitude,gps_accuracy_m,note,ai\n");
        JSONArray a = s.optJSONArray("markers");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject m = a.getJSONObject(i);
                Object objValueOf = "";
                StringBuilder sbAppend = b.append(q(s.optString("propertyName"))).append(',').append(q(m.optString("label"))).append(',').append(String.format(Locale.US, "%.1f", Float.valueOf(m.optLong("elapsedMs") / 1000.0f))).append(',').append(m.optDouble("magneticHeading")).append(',').append(m.optDouble("trueHeading")).append(',').append(q(m.optString("sector"))).append(',').append(q(m.optString("entrance32"))).append(',').append(m.isNull("latitude") ? "" : Double.valueOf(m.optDouble("latitude"))).append(',').append(m.isNull("longitude") ? "" : Double.valueOf(m.optDouble("longitude"))).append(',');
                if (!m.isNull("locationAccuracyMeters")) {
                    objValueOf = Double.valueOf(m.optDouble("locationAccuracyMeters"));
                }
                sbAppend.append(objValueOf).append(',').append(q(m.optString("note"))).append(',').append(q(m.optString("aiSuggestion"))).append('\n');
            }
        }
        OutputStream o = this.c.getContentResolver().openOutputStream(u);
        try {
            o.write(b.toString().getBytes(StandardCharsets.UTF_8));
            if (o != null) {
                o.close();
            }
            return u;
        } catch (Throwable th) {
            if (o != null) {
                try {
                    o.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
            }
            throw th;
        }
    }
}
