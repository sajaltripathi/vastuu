package com.tiscan.app;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns a (roomType, padaCode) pair into an actual verdict — "Ideal" / "Acceptable" /
 * "Vastu Dosh" / "Neutral" — with a plain-language reason and, if it's a dosh, a
 * standard remedy suggestion. This is the piece the app was missing: Vastu32 can already
 * tell you *which* pada a room sits in, but nothing previously told you whether that was
 * good or bad.
 *
 * Rules live in assets/vastu_rules.json on purpose, not hardcoded here. Vastu placement
 * conventions vary by tradition and text (Mayamatam, Manasara, and regional practice don't
 * always agree on secondary rooms) — treat the shipped file as a reasonable, commonly-cited
 * default, review it against whichever source the product wants to stand behind, and edit
 * freely. Entrance rules in particular are simplified here: real practice weighs entrance
 * placement against the plot's overall facing direction, which this simple pada-lookup
 * model doesn't capture.
 */
public final class VastuRuleEngine {

    public static final class Verdict {
        public final String roomType;
        public final String padaCode;
        public final String rating;       // "Ideal" | "Acceptable" | "Vastu Dosh" | "Neutral" | "Unrated"
        public final String explanation;
        public final String remedy;       // populated only when rating is "Vastu Dosh"

        Verdict(String roomType, String padaCode, String rating, String explanation, String remedy) {
            this.roomType = roomType;
            this.padaCode = padaCode;
            this.rating = rating;
            this.explanation = explanation;
            this.remedy = remedy;
        }

        /** One line, ready to drop into a toast or marker note. */
        public String summary() {
            if ("Vastu Dosh".equals(rating) && remedy != null && !remedy.isEmpty()) {
                return rating + " — " + explanation + " Suggested remedy: " + remedy;
            }
            return rating + " — " + explanation;
        }
    }

    private static final class Rule {
        Set<String> ideal = new HashSet<>();
        Set<String> acceptable = new HashSet<>();
        Set<String> avoid = new HashSet<>();
        String idealNote = "";
        String acceptableNote = "";
        String avoidNote = "";
        String remedy = "";
    }

    private final Map<String, Rule> rules = new HashMap<>();
    private boolean loaded = false;
    private String loadError = null;

    public VastuRuleEngine(Context context) {
        try {
            rules.putAll(load(context));
            loaded = true;
        } catch (Exception e) {
            loaded = false;
            loadError = e.getMessage();
            // Deliberately swallowed beyond storing the message: a missing/broken rules
            // file should degrade to "Unrated" verdicts, not crash a live survey.
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public String getLoadError() {
        return loadError;
    }

    private static Map<String, Rule> load(Context context) throws IOException, JSONException {
        Map<String, Rule> out = new HashMap<>();
        InputStream is = context.getAssets().open("vastu_rules.json");
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line);
        is.close();

        JSONObject root = new JSONObject(sb.toString());
        JSONArray roomsArr = root.getJSONArray("rooms");
        for (int i = 0; i < roomsArr.length(); i++) {
            JSONObject ro = roomsArr.getJSONObject(i);
            Rule rule = new Rule();
            rule.ideal = toSet(ro.optJSONArray("idealPadaCodes"));
            rule.acceptable = toSet(ro.optJSONArray("acceptablePadaCodes"));
            rule.avoid = toSet(ro.optJSONArray("avoidPadaCodes"));
            rule.idealNote = ro.optString("idealNote", "");
            rule.acceptableNote = ro.optString("acceptableNote", "");
            rule.avoidNote = ro.optString("avoidNote", "");
            rule.remedy = ro.optString("remedy", "");
            out.put(ro.getString("roomType"), rule);
        }
        return out;
    }

    private static Set<String> toSet(JSONArray arr) throws JSONException {
        Set<String> s = new HashSet<>();
        if (arr == null) return s;
        for (int i = 0; i < arr.length(); i++) s.add(arr.getString(i));
        return s;
    }

    /**
     * @param roomType must match a "roomType" entry in vastu_rules.json (the same labels
     *                 already used by the tag buttons / voice parser).
     * @param padaCode one of Vastu32's 32 codes (e.g. "S1"), computed from TRUE heading —
     *                 pass Vastu32.code(trueHeading), not Vastu32.code(magneticHeading).
     */
    public static final class ScoreResult {
        public final int score; // 0-100, or -1 if nothing could be scored
        public final int ideal;
        public final int acceptable;
        public final int neutral;
        public final int dosh;
        public final int unrated;

        ScoreResult(int score, int ideal, int acceptable, int neutral, int dosh, int unrated) {
            this.score = score;
            this.ideal = ideal;
            this.acceptable = acceptable;
            this.neutral = neutral;
            this.dosh = dosh;
            this.unrated = unrated;
        }

        public String summaryLine() {
            if (score < 0) return "Vastu Score: not enough tagged rooms yet";
            return String.format(java.util.Locale.US,
                "Vastu Score: %d/100  (Ideal %d · Acceptable %d · Neutral %d · Dosh %d%s)",
                score, ideal, acceptable, neutral, dosh,
                unrated > 0 ? " · " + unrated + " unrated" : "");
        }
    }

    /**
     * Rolls every tagged marker up into one score. Ideal=100, Acceptable=70,
     * Neutral=50, Vastu Dosh=20 points; Unrated markers (no rule on file for
     * that room type) are excluded from the average rather than counted
     * against the property — an unrated room isn't a known problem.
     */
    public ScoreResult scoreMarkers(JSONArray markers) {
        int ideal = 0, acceptable = 0, neutral = 0, dosh = 0, unrated = 0;
        double pointsSum = 0;
        int counted = 0;
        if (markers != null) {
            for (int i = 0; i < markers.length(); i++) {
                JSONObject m = markers.optJSONObject(i);
                if (m == null) continue;
                Verdict v = evaluate(m.optString("label"), m.optString("entrance32"));
                switch (v.rating) {
                    case "Ideal": ideal++; pointsSum += 100; counted++; break;
                    case "Acceptable": acceptable++; pointsSum += 70; counted++; break;
                    case "Neutral": neutral++; pointsSum += 50; counted++; break;
                    case "Vastu Dosh": dosh++; pointsSum += 20; counted++; break;
                    default: unrated++; break;
                }
            }
        }
        int score = counted == 0 ? -1 : (int) Math.round(pointsSum / counted);
        return new ScoreResult(score, ideal, acceptable, neutral, dosh, unrated);
    }

    public Verdict evaluate(String roomType, String padaCode) {
        Rule rule = rules.get(roomType);
        if (!loaded || rule == null) {
            return new Verdict(roomType, padaCode, "Unrated",
                "No rule on file yet for \"" + roomType + "\" — add one to assets/vastu_rules.json.", null);
        }
        if (rule.ideal.contains(padaCode)) {
            return new Verdict(roomType, padaCode, "Ideal", rule.idealNote, null);
        }
        if (rule.acceptable.contains(padaCode)) {
            return new Verdict(roomType, padaCode, "Acceptable", rule.acceptableNote, null);
        }
        if (rule.avoid.contains(padaCode)) {
            return new Verdict(roomType, padaCode, "Vastu Dosh", rule.avoidNote, rule.remedy);
        }
        return new Verdict(roomType, padaCode, "Neutral",
            "Not specifically flagged ideal or avoid for " + roomType + " — no strong classical guidance either way for this pada.", null);
    }
}
