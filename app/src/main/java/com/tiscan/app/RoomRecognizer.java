package com.tiscan.app;

import android.os.SystemClock;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import java.util.List;
import java.util.Locale;

/* JADX INFO: loaded from: classes3.dex */
public class RoomRecognizer implements ImageAnalysis.Analyzer {
    private boolean busy;
    private final ImageLabeler lab = ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(0.55f).build());
    private long last;
    private final Listener listener;

    public interface Listener {
        void onRoomSuggestion(String str, float f, String str2);
    }

    public RoomRecognizer(Listener l) {
        this.listener = l;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$analyze$0(ImageProxy p, Task t) {
        this.busy = false;
        p.close();
    }

    @Override // androidx.camera.core.ImageAnalysis.Analyzer
    public void analyze(final ImageProxy p) {
        long now = SystemClock.elapsedRealtime();
        if (this.busy || now - this.last < 1200 || p.getImage() == null) {
            p.close();
            return;
        }
        this.last = now;
        this.busy = true;
        InputImage im = InputImage.fromMediaImage(p.getImage(), p.getImageInfo().getRotationDegrees());
        this.lab.process(im).addOnSuccessListener(new OnSuccessListener() { // from class: com.tiscan.app.RoomRecognizer$$ExternalSyntheticLambda0
            @Override // com.google.android.gms.tasks.OnSuccessListener
            public final void onSuccess(Object obj) {
                this.f$0.interpret((List) obj);
            }
        }).addOnCompleteListener(new OnCompleteListener() { // from class: com.tiscan.app.RoomRecognizer$$ExternalSyntheticLambda1
            @Override // com.google.android.gms.tasks.OnCompleteListener
            public final void onComplete(Task task) {
                this.f$0.lambda$analyze$0(p, task);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void interpret(List<ImageLabel> ls) {
        String br = null;
        String ev = null;
        float bc = 0.0f;
        for (ImageLabel l : ls) {
            String s = l.getText().toLowerCase(Locale.US);
            String r = map(s);
            if (r != null && l.getConfidence() > bc) {
                br = r;
                bc = l.getConfidence();
                ev = l.getText();
            }
        }
        if (br != null) {
            this.listener.onRoomSuggestion(br, bc, ev);
        }
    }

    private boolean any(String s, String... a) {
        for (String x : a) {
            if (s.contains(x)) {
                return true;
            }
        }
        return false;
    }

    private String map(String s) {
        if (any(s, "toilet", "bathroom", "shower", "bathtub", "washbasin", "towel")) {
            return "Washroom";
        }
        if (any(s, "kitchen", "stove", "oven", "refrigerator", "cookware", "cooking", "countertop")) {
            return "Kitchen";
        }
        if (any(s, "bed", "bedroom", "mattress", "pillow")) {
            return "Bedroom";
        }
        if (any(s, "sofa", "couch", "living room", "television")) {
            return "Living";
        }
        if (any(s, "stairs", "staircase")) {
            return "Staircase";
        }
        return null;
    }

    public void close() {
        try {
            this.lab.close();
        } catch (Exception e) {
        }
    }
}
