package com.tiscan.app;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.media3.extractor.text.ttml.TtmlNode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class PropertyStore {
    private final SharedPreferences p;

    public static class Property {
        public String address;
        public String id;
        public String name;
        public String notes;

        Property(String i, String n, String a, String no) {
            this.id = i;
            this.name = n;
            this.address = a;
            this.notes = no;
        }
    }

    public PropertyStore(Context c) {
        this.p = c.getSharedPreferences("properties", 0);
    }

    public List<Property> all() {
        List<Property> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(this.p.getString("items", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                out.add(new Property(o.getString(TtmlNode.ATTR_ID), o.optString("name"), o.optString("address"), o.optString("notes")));
            }
        } catch (Exception e) {
        }
        return out;
    }

    public Property get(String id) {
        for (Property x : all()) {
            if (x.id.equals(id)) {
                return x;
            }
        }
        return null;
    }

    public Property add(String n, String a, String no) {
        Property x = new Property(UUID.randomUUID().toString(), n, a, no);
        List<Property> l = all();
        l.add(x);
        save(l);
        return x;
    }

    private void save(List<Property> l) {
        JSONArray a = new JSONArray();
        try {
            for (Property x : l) {
                JSONObject o = new JSONObject();
                o.put(TtmlNode.ATTR_ID, x.id);
                o.put("name", x.name);
                o.put("address", x.address);
                o.put("notes", x.notes);
                a.put(o);
            }
        } catch (Exception e) {
        }
        this.p.edit().putString("items", a.toString()).apply();
    }
}
