package com.tiscan.app;

import android.content.Context;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ARScanStore {
    private final Context context;

    public ARScanStore(Context c) {
        this.context = c;
    }

    public File save(String propertyId, JSONObject data) throws Exception {
        File d = new File(this.context.getFilesDir(), "ar_scans");
        if (!d.exists() && !d.mkdirs()) {
            throw new IOException("Cannot create AR scan folder");
        }
        File f = new File(d, (propertyId == null ? "property" : propertyId) + "_" + System.currentTimeMillis() + ".json");
        FileOutputStream out = new FileOutputStream(f);
        try {
            out.write(data.toString(2).getBytes(StandardCharsets.UTF_8));
            out.close();
            return f;
        } catch (Throwable th) {
            try {
                out.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public File latest(final String propertyId) {
        File d = new File(this.context.getFilesDir(), "ar_scans");
        File[] fs = d.listFiles(new FilenameFilter() { // from class: com.tiscan.app.ARScanStore$$ExternalSyntheticLambda0
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                return ARScanStore.lambda$latest$0(propertyId, file, str);
            }
        });
        if (fs == null || fs.length == 0) {
            return null;
        }
        Arrays.sort(fs, new Comparator() { // from class: com.tiscan.app.ARScanStore$$ExternalSyntheticLambda1
            @Override // java.util.Comparator
            public final int compare(Object obj, Object obj2) {
                return Long.compare(((File) obj2).lastModified(), ((File) obj).lastModified());
            }
        });
        return fs[0];
    }

    static /* synthetic */ boolean lambda$latest$0(String propertyId, File dir, String name) {
        return name.endsWith(".json") && (propertyId == null || name.startsWith(new StringBuilder().append(propertyId).append("_").toString()));
    }

    public JSONObject read(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        while (true) {
            try {
                String l = br.readLine();
                if (l == null) {
                    br.close();
                    return new JSONObject(sb.toString());
                }
                sb.append(l);
            } catch (Throwable th) {
                try {
                    br.close();
                } catch (Throwable th2) {
                    th.addSuppressed(th2);
                }
                throw th;
            }
        }
    }
}
