package com.tiscan.app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;

/**
 * The Shakti-Chakra-style life-aspect layer -- a second, more modern reading
 * layered on top of Vastu32's classical 32-pada system. Keyed on the same
 * 16-point sector string VastuDirection.sector() already produces (N, NNE,
 * NE...) so no new heading math is needed -- pass it whatever sector() gives
 * you for the current trueHeading.
 *
 * Wording in life_aspect_zones.json is original, inspired by the general
 * direction-to-life-theme idea used by some modern Vastu schools, not copied
 * from any specific app.
 */
public final class LifeAspectZones {

    public static final class Zone {
        public final String sector;
        public final String aspectEn;
        public final String aspectHi;
        public final String element;
        public final String dosha;

        Zone(String sector, String aspectEn, String aspectHi, String element, String dosha) {
            this.sector = sector;
            this.aspectEn = aspectEn;
            this.aspectHi = aspectHi;
            this.element = element;
            this.dosha = dosha;
        }

        public String aspectLine() {
            return aspectEn + " / " + aspectHi;
        }
    }

    private final Map<String, Zone> zones = new HashMap<>();
    private boolean loaded = false;

    public LifeAspectZones(Context context) {
        try {
            InputStream is = context.getAssets().open("life_aspect_zones.json");
            BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            is.close();
            JSONObject root = new JSONObject(sb.toString());
            Iterator<String> keys = root.keys();
            while (keys.hasNext()) {
                String sector = keys.next();
                if (sector.startsWith("_")) continue;
                JSONObject z = root.getJSONObject(sector);
                zones.put(sector, new Zone(sector, z.getString("aspectEn"), z.getString("aspectHi"),
                    z.getString("element"), z.getString("dosha")));
            }
            loaded = true;
        } catch (Exception e) {
            loaded = false;
        }
    }

    /** @param sector16 one of N/NNE/NE/ENE/E/ESE/SE/SSE/S/SSW/SW/WSW/W/WNW/NW/NNW */
    public Zone get(String sector16) {
        return loaded ? zones.get(sector16) : null;
    }
}
