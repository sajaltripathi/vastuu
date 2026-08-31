package com.tiscan.app;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.effect.OverlayEffect;
import androidx.media3.effect.TextOverlay;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.EditedMediaItem;
import androidx.media3.transformer.Effects;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class VideoOverlayExporter {

    public interface Listener {
        void done(Uri uri);

        void error(String str);
    }

    public static class Sample {
        public float deg;
        public long ms;
        public String room;

        public Sample(long m, float d, String r) {
            this.ms = m;
            this.deg = d;
            this.room = r;
        }
    }

    public static void export(final Context c, Uri src, final List<Sample> ss, final File tmp, final Listener l) {
        try {
            TextOverlay txt = new TextOverlay() { // from class: com.tiscan.app.VideoOverlayExporter.1
                @Override // androidx.media3.effect.TextOverlay
                public SpannableString getText(long us) {
                    String z;
                    Sample s = VideoOverlayExporter.at(ss, us / 1000);
                    if (s == null) {
                        z = "VASTU SURVEY";
                    } else {
                        z = String.format(Locale.US, "VASTU • %.1f° %s • %s%s", Float.valueOf(s.deg), VastuDirection.sector(s.deg), Vastu32.label(s.deg), (s.room == null || s.room.isEmpty()) ? "" : " • " + s.room);
                    }
                    SpannableString x = new SpannableString(z);
                    x.setSpan(new ForegroundColorSpan(-1), 0, x.length(), 33);
                    x.setSpan(new BackgroundColorSpan(-1342177280), 0, x.length(), 33);
                    x.setSpan(new RelativeSizeSpan(0.55f), 0, x.length(), 33);
                    return x;
                }
            };
            OverlayEffect oe = new OverlayEffect(Collections.singletonList(txt));
            Effects ef = new Effects(Collections.emptyList(), Collections.singletonList(oe));
            EditedMediaItem item = new EditedMediaItem.Builder(MediaItem.fromUri(src)).setEffects(ef).build();
            if (tmp.exists()) {
                tmp.delete();
            }
            Transformer t = new Transformer.Builder(c).addListener(new Transformer.Listener() { // from class: com.tiscan.app.VideoOverlayExporter.2
                @Override // androidx.media3.transformer.Transformer.Listener
                public void onCompleted(Composition co, ExportResult r) {
                    try {
                        l.done(VideoOverlayExporter.publish(c, tmp));
                    } catch (Exception e) {
                        l.error(e.getMessage());
                    }
                }

                @Override // androidx.media3.transformer.Transformer.Listener
                public void onError(Composition co, ExportResult r, ExportException e) {
                    l.error(e.getMessage());
                }
            }).build();
            t.start(item, tmp.getAbsolutePath());
        } catch (Exception e) {
            l.error(e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Sample at(List<Sample> a, long m) {
        if (a == null || a.isEmpty()) {
            return null;
        }
        Sample b = a.get(0);
        for (Sample s : a) {
            if (s.ms > m) {
                break;
            }
            b = s;
        }
        return b;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static Uri publish(Context c, File f) throws Exception {
        ContentValues v = new ContentValues();
        v.put("_display_name", f.getName());
        v.put("mime_type", MimeTypes.VIDEO_MP4);
        v.put("relative_path", "Movies/VastuSurvey");
        Uri u = c.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (u == null) {
            throw new Exception("Cannot publish overlay video");
        }
        InputStream in = new FileInputStream(f);
        try {
            OutputStream out = c.getContentResolver().openOutputStream(u);
            try {
                byte[] b = new byte[65536];
                while (true) {
                    int n = in.read(b);
                    if (n <= 0) {
                        break;
                    }
                    out.write(b, 0, n);
                    try {
                        in.close();
                    } catch (Throwable th) {
                        th.addSuppressed(th);
                    }
                    throw th;
                }
                if (out != null) {
                    out.close();
                }
                in.close();
                f.delete();
                return u;
            } catch (Throwable th2) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable th3) {
                        th2.addSuppressed(th3);
                    }
                }
                throw th2;
            }
        } catch (Throwable th4) {
            in.close();
            throw th4;
        }
    }
}
