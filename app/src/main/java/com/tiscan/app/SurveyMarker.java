package com.tiscan.app;

import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class SurveyMarker {
    public final String aiSuggestion;
    public final long elapsedMs;
    public final String entrance32;
    public final String label;
    public final Double latitude;
    public final Float locationAccuracyMeters;
    public final Double longitude;
    public final float magneticHeading;
    public final String note;
    public final String sector;
    public final float trueHeading;

    public SurveyMarker(String l, long e, float m, float t, String s, String e32, Double la, Double lo, Float ac, String n, String ai) {
        this.label = l;
        this.elapsedMs = e;
        this.magneticHeading = m;
        this.trueHeading = t;
        this.sector = s;
        this.entrance32 = e32;
        this.latitude = la;
        this.longitude = lo;
        this.locationAccuracyMeters = ac;
        this.note = n;
        this.aiSuggestion = ai;
    }

    public JSONObject json() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("label", this.label);
        o.put("elapsedMs", this.elapsedMs);
        o.put("magneticHeading", this.magneticHeading);
        o.put("trueHeading", this.trueHeading);
        o.put("sector", this.sector);
        o.put("entrance32", this.entrance32);
        o.put("latitude", this.latitude == null ? JSONObject.NULL : this.latitude);
        o.put("longitude", this.longitude == null ? JSONObject.NULL : this.longitude);
        o.put("locationAccuracyMeters", this.locationAccuracyMeters == null ? JSONObject.NULL : this.locationAccuracyMeters);
        o.put("note", this.note);
        o.put("aiSuggestion", this.aiSuggestion);
        return o;
    }
}
