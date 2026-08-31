package com.tiscan.app;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.GeomagneticField;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.PendingRecording;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.media3.common.MimeTypes;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

/* JADX INFO: loaded from: classes3.dex */
public class MainActivity extends ComponentActivity implements CompassManager.Listener, LocationTracker.Listener, RoomRecognizer.Listener {
    private TextView aiText;
    private ExecutorService analysisExecutor;
    private ProcessCameraProvider cameraProvider;
    private boolean cameraStarting;
    private TextView cameraText;
    private CompassManager compass;
    private FloorPlanTracker floor;
    private TextView gpsText;
    private boolean headingReliable;
    private TextView headingText;
    private ImageCapture imageCapture;
    private Location lastLocation;
    private LocationTracker locationTracker;
    private float magneticHeading;
    private VastuOverlayView overlay;
    private PreviewView preview;
    private PropertyStore.Property property;
    private TextView propertyText;
    private TextView qualityText;
    private Uri rawVideo;
    private Button recordButton;
    private Recording recording;
    private RoomRecognizer roomRecognizer;
    private int sensorAccuracy;
    private String sessionId;
    private SpeechRecognizer speech;
    private int stableCount;
    private long startedElapsed;
    private long startedEpoch;
    private float trueHeading;
    private VideoCapture<Recorder> videoCapture;
    private String lastScreenshotUri = "";
    private String surveyNote = "";
    private String latestAi = "";
    private Button voiceButton;
    private boolean voiceModeOn;
    private VastuRuleEngine ruleEngine;
    private LifeAspectZones lifeAspectZones;
    private TextView zoneAspectText;
    private TextView zoneMetaText;

    private static final int OBSIDIAN = 0xFF0A100D;
    private static final int HEADER_BG = 0xE60A100D;
    private static final int CARD_BG = 0xFF121C17;
    private static final int CARD_BORDER = 0xFF223026;
    private static final int TEXT_PRIMARY = 0xFFEDF3EF;
    private static final int TEXT_MUTED = 0xFF8A9992;
    private static final int GOLD = 0xFFD9B36C;
    private static final int CYAN = 0xFF4FE3C8;
    private static final int EMERALD_FILL = 0xFF1F8A5F;
    private static final int EMERALD_TEXT = 0xFF5FD79A;
    private static final int EMERALD_BG = 0xFF173425;
    private static final int EMERALD_BORDER = 0xFF245A3C;
    private static final int WARN = 0xFFE8A33D;
    private String stableRoom = "";
    private final List<SurveyMarker> markers = new ArrayList();
    private final List<VideoOverlayExporter.Sample> samples = new ArrayList();
    private final Set<String> autoTagged = new HashSet();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sampler = new Runnable() { // from class: com.tiscan.app.MainActivity.1
        @Override // java.lang.Runnable
        public void run() {
            if (MainActivity.this.recording != null) {
                MainActivity.this.samples.add(new VideoOverlayExporter.Sample(SystemClock.elapsedRealtime() - MainActivity.this.startedElapsed, MainActivity.this.magneticHeading, MainActivity.this.latestAi));
                MainActivity.this.handler.postDelayed(this, 500L);
            }
        }
    };
    private final ActivityResultLauncher<String[]> permissions = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda3
        @Override // androidx.activity.result.ActivityResultCallback
        public final void onActivityResult(Object obj) {
            this.f$0.lambda$new$0((Map) obj);
        }
    });

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$new$0(Map result) {
        if (has("android.permission.CAMERA")) {
            startCamera();
        } else {
            this.cameraText.setText("Camera permission required");
        }
        this.locationTracker.start();
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ViewCompat.MEASURED_STATE_MASK);
        getWindow().setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK);
        String propertyId = getIntent().getStringExtra("propertyId");
        this.property = new PropertyStore(this).get(propertyId);
        if (this.property == null) {
            finish();
            return;
        }
        this.compass = new CompassManager(this, this);
        this.locationTracker = new LocationTracker(this, this);
        this.floor = new FloorPlanTracker(this);
        this.roomRecognizer = new RoomRecognizer(this);
        this.ruleEngine = new VastuRuleEngine(this);
        this.lifeAspectZones = new LifeAspectZones(this);
        this.analysisExecutor = Executors.newSingleThreadExecutor();
        buildUi();
        requestPermissionsAndStart();
    }

    private void buildUi() {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(OBSIDIAN);
        this.preview = new PreviewView(this);
        this.preview.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);
        this.preview.setScaleType(PreviewView.ScaleType.FILL_CENTER);
        frameLayout.addView(this.preview, new FrameLayout.LayoutParams(-1, -1));
        this.overlay = new VastuOverlayView(this);
        frameLayout.addView(this.overlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(1);
        top.setPadding(dp(14), dp(10), dp(14), dp(12));
        top.setBackgroundColor(HEADER_BG);
        this.propertyText = text(this.property.name, 14, true);
        this.propertyText.setTextColor(TEXT_MUTED);
        top.addView(this.propertyText);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(16);
        row.setPadding(0, dp(2), 0, 0);
        this.headingText = mono("Compass —", 24, GOLD);
        row.addView(this.headingText, new LinearLayout.LayoutParams(0, -2, 1.0f));
        this.cameraText = text("CAMERA STARTING", 10, true);
        this.cameraText.setTextColor(WARN);
        row.addView(this.cameraText);
        top.addView(row);

        this.gpsText = text("GPS: acquiring high-accuracy fix…", 12, false);
        this.gpsText.setTextColor(TEXT_MUTED);
        top.addView(this.gpsText);
        this.qualityText = text("Compass: stabilizing…", 12, false);
        this.qualityText.setTextColor(TEXT_MUTED);
        top.addView(this.qualityText);
        this.aiText = text("AI room: scanning…", 12, false);
        this.aiText.setTextColor(CYAN);
        top.addView(this.aiText);

        LinearLayout zoneCard = new LinearLayout(this);
        zoneCard.setOrientation(1);
        zoneCard.setBackground(cardBg(CARD_BG, CARD_BORDER, 10));
        zoneCard.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams zoneCardLp = new LinearLayout.LayoutParams(-1, -2);
        zoneCardLp.topMargin = dp(8);
        this.zoneAspectText = text("Zone —", 13, true);
        this.zoneAspectText.setTextColor(TEXT_PRIMARY);
        zoneCard.addView(this.zoneAspectText);
        this.zoneMetaText = text("Element — · Dosha —", 12, false);
        this.zoneMetaText.setTextColor(CYAN);
        LinearLayout.LayoutParams metaLp = new LinearLayout.LayoutParams(-2, -2);
        metaLp.topMargin = dp(3);
        zoneCard.addView(this.zoneMetaText, metaLp);
        top.addView(zoneCard, zoneCardLp);

        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2);
        tp.gravity = 48;
        frameLayout.addView(top, tp);
        ViewCompat.setOnApplyWindowInsetsListener(top, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(dp(14), bars.top + dp(10), dp(14), dp(12));
            return insets;
        });

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(1);
        side.setGravity(17);
        Button ar = railButton("AR\nMeasure", false);
        ar.setOnClickListener(v -> openArMeasure());
        side.addView(ar, railLp(64));
        Button chakra = railButton("Vastu\nChakra", false);
        chakra.setOnClickListener(v -> openChakra());
        side.addView(chakra, railLp(64));
        Button map = railButton("Floor\nPlan", false);
        map.setOnClickListener(v -> openFloorPlan());
        side.addView(map, railLp(62));
        Button hist = railButton("DataVault", true);
        hist.setOnClickListener(v -> openHistory());
        side.addView(hist, railLp(54));
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(dp(84), -2);
        sp.gravity = 8388629;
        sp.setMargins(0, 0, dp(6), 0);
        frameLayout.addView(side, sp);

        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(10), dp(7), dp(10), dp(12));
        linearLayout.setBackgroundColor(HEADER_BG);
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tags = new LinearLayout(this);
        String[] labels = {"Entrance", "Kitchen", "Washroom", "Bedroom", "Master Bedroom",
            "Kids Bedroom", "Guest Room", "Puja", "Living", "Dining", "Study",
            "Staircase", "Store Room", "Balcony"};
        for (String l : labels) {
            tags.addView(tagButton(l));
        }
        scroll.addView(tags);
        linearLayout.addView(scroll, new LinearLayout.LayoutParams(-1, dp(46)));

        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(17);
        Button voice = toolButton("🎤 Voice");
        this.voiceButton = voice;
        voice.setOnClickListener(v -> toggleVoice());
        tools.addView(voice, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button shot = toolButton("📷 Shot");
        shot.setOnClickListener(v -> takePhoto());
        tools.addView(shot, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button note = toolButton("📝 Note");
        note.setOnClickListener(v -> note());
        tools.addView(note, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button report = toolButton("📄 PDF");
        report.setOnClickListener(v -> openLatestReport());
        tools.addView(report, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        linearLayout.addView(tools);

        this.recordButton = new Button(this);
        this.recordButton.setText("●  Start recording");
        this.recordButton.setTextSize(17.0f);
        this.recordButton.setAllCaps(false);
        this.recordButton.setTextColor(0xFFF3FBF6);
        this.recordButton.setBackground(cardBg(EMERALD_FILL, 0, 10));
        this.recordButton.setOnClickListener(v -> toggleRecording());
        LinearLayout.LayoutParams rbLp = new LinearLayout.LayoutParams(-1, dp(58));
        rbLp.topMargin = dp(10);
        linearLayout.addView(this.recordButton, rbLp);

        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2);
        bp.gravity = 80;
        bp.setMargins(0, 0, 0, dp(24));
        frameLayout.addView(linearLayout, bp);
        setContentView(frameLayout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$1(View v) {
        openArMeasure();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$2(View v) {
        openChakra();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$3(View v) {
        openFloorPlan();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$4(View v) {
        openHistory();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$5(View v) {
        voice();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$6(View v) {
        takePhoto();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$7(View v) {
        note();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$8(View v) {
        openLatestReport();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$9(View v) {
        toggleRecording();
    }

    private void requestPermissionsAndStart() {
        List<String> needed = new ArrayList<>();
        if (!has("android.permission.CAMERA")) {
            needed.add("android.permission.CAMERA");
        }
        if (!has("android.permission.RECORD_AUDIO")) {
            needed.add("android.permission.RECORD_AUDIO");
        }
        if (!has("android.permission.ACCESS_FINE_LOCATION")) {
            needed.add("android.permission.ACCESS_FINE_LOCATION");
            if (!has("android.permission.ACCESS_COARSE_LOCATION")) {
                needed.add("android.permission.ACCESS_COARSE_LOCATION");
            }
        }
        if (needed.isEmpty()) {
            startCamera();
            this.locationTracker.start();
        } else {
            this.permissions.launch((String[]) needed.toArray(new String[0]));
        }
    }

    private boolean has(String p) {
        return ContextCompat.checkSelfPermission(this, p) == 0;
    }

    private void startCamera() {
        if (this.cameraStarting || this.videoCapture != null) {
            return;
        }
        this.cameraStarting = true;
        this.cameraText.setText("CAMERA STARTING");
        final ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(new Runnable() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$startCamera$10(future);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    public /* synthetic */ void lambda$startCamera$10(ListenableFuture future) {
        try {
            this.cameraProvider = (ProcessCameraProvider) future.get();
            Preview p = new Preview.Builder().build();
            p.setSurfaceProvider(this.preview.getSurfaceProvider());
            Recorder recorder = new Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.FHD)).build();
            this.videoCapture = VideoCapture.withOutput(recorder);
            this.imageCapture = new ImageCapture.Builder().setCaptureMode(1).build();
            ImageAnalysis analysis = new ImageAnalysis.Builder().setBackpressureStrategy(0).build();
            analysis.setAnalyzer(this.analysisExecutor, this.roomRecognizer);
            this.cameraProvider.unbindAll();
            this.cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, p, this.imageCapture, analysis, this.videoCapture);
            this.cameraStarting = false;
            this.cameraText.setText("● CAMERA LIVE");
            this.cameraText.setTextColor(-4589878);
            this.overlay.setScanStatus("LIVE VASTU SCAN");
        } catch (Exception e) {
            this.cameraStarting = false;
            this.videoCapture = null;
            this.imageCapture = null;
            this.cameraText.setText("CAMERA ERROR");
            this.cameraText.setTextColor(-44462);
            toast("Camera failed: " + e.getMessage());
        }
    }

    private void toggleRecording() {
        if (this.recording == null) {
            startRecording();
        } else {
            this.recordButton.setEnabled(false);
            this.recording.stop();
        }
    }

    private void startRecording() {
        if (this.videoCapture == null) {
            toast("Camera is still starting. Try again in a moment.");
            return;
        }
        this.sessionId = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        this.startedEpoch = System.currentTimeMillis();
        this.startedElapsed = SystemClock.elapsedRealtime();
        this.markers.clear();
        this.samples.clear();
        this.autoTagged.clear();
        this.lastScreenshotUri = "";
        this.surveyNote = "";
        this.floor = new FloorPlanTracker(this);
        this.floor.start();
        ContentValues values = new ContentValues();
        values.put("_display_name", "VastuRaw_" + this.sessionId + ".mp4");
        values.put("mime_type", MimeTypes.VIDEO_MP4);
        values.put("relative_path", "Movies/VastuSurvey");
        MediaStoreOutputOptions out = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build();
        PendingRecording pending = ((Recorder) this.videoCapture.getOutput()).prepareRecording(this, out);
        if (has("android.permission.RECORD_AUDIO")) {
            pending = pending.withAudioEnabled();
        }
        this.recording = pending.start(ContextCompat.getMainExecutor(this), new Consumer() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda14
            @Override // androidx.core.util.Consumer
            public final void accept(Object obj) {
                this.f$0.lambda$startRecording$11((VideoRecordEvent) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$startRecording$11(VideoRecordEvent event) {
        if (!(event instanceof VideoRecordEvent.Start)) {
            if (event instanceof VideoRecordEvent.Finalize) {
                VideoRecordEvent.Finalize f = (VideoRecordEvent.Finalize) event;
                this.rawVideo = f.getOutputResults().getOutputUri();
                this.recording = null;
                this.handler.removeCallbacks(this.sampler);
                this.floor.stop();
                this.recordButton.setEnabled(true);
                this.recordButton.setText("●  START RECORDING");
                this.overlay.setScanStatus("LIVE VASTU SCAN");
                if (f.hasError()) {
                    saveSession(null);
                    toast("Recording ended with an error; measurements were preserved.");
                    return;
                } else {
                    burnOverlay();
                    return;
                }
            }
            return;
        }
        this.recordButton.setText("■  STOP & SAVE");
        this.overlay.setScanStatus("● RECORDING + MAPPING");
        this.handler.post(this.sampler);
    }

    private void burnOverlay() {
        this.overlay.setScanStatus("EXPORTING VASTU VIDEO");
        toast("Encoding permanent compass overlay…");
        File dir = getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        if (dir == null) {
            saveSession(null);
        } else {
            File temp = new File(dir, "VastuOverlay_" + this.sessionId + ".mp4");
            VideoOverlayExporter.export(this, this.rawVideo, this.samples, temp, new VideoOverlayExporter.Listener() { // from class: com.tiscan.app.MainActivity.2
                @Override // com.tiscan.app.VideoOverlayExporter.Listener
                public void done(Uri uri) {
                    MainActivity.this.saveSession(uri.toString());
                    MainActivity.this.overlay.setScanStatus("LIVE VASTU SCAN");
                    MainActivity.this.toast("Survey saved with permanent Vastu overlay.");
                }

                @Override // com.tiscan.app.VideoOverlayExporter.Listener
                public void error(String m) {
                    MainActivity.this.saveSession(null);
                    MainActivity.this.overlay.setScanStatus("LIVE VASTU SCAN");
                    MainActivity.this.toast("Survey saved; overlay export failed: " + m);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void saveSession(String overlayUri) {
        Exception e;
        try {
            try {
                new SurveySessionStore(this).save(this.sessionId, this.startedEpoch, this.property, this.rawVideo == null ? null : this.rawVideo.toString(), overlayUri, this.lastScreenshotUri, this.surveyNote, this.markers, this.floor.json());
            } catch (Exception e2) {
                e = e2;
                toast("Survey metadata save failed: " + e.getMessage());
            }
        } catch (Exception e3) {
            e = e3;
        }
    }

    private void addMarker(String label, String evidence) {
        if (this.recording == null) {
            toast("Start recording, then tag the room/feature.");
            return;
        }
        if (!this.headingReliable) {
            toast("Hold the phone upright and steady before tagging.");
            return;
        }
        long ms = SystemClock.elapsedRealtime() - this.startedElapsed;
        Double lat = this.lastLocation == null ? null : Double.valueOf(this.lastLocation.getLatitude());
        Double lon = this.lastLocation == null ? null : Double.valueOf(this.lastLocation.getLongitude());
        Float accuracy = this.lastLocation != null ? Float.valueOf(this.lastLocation.getAccuracy()) : null;
        String padaCode = Vastu32.code(this.trueHeading);
        this.markers.add(new SurveyMarker(label, ms, this.magneticHeading, this.trueHeading, VastuDirection.sector(this.trueHeading), padaCode, lat, lon, accuracy, this.surveyNote, evidence));
        VastuRuleEngine.Verdict verdict = this.ruleEngine.evaluate(label, padaCode);
        toast(label + " • " + VastuDirection.formatted(this.trueHeading) + " • " + Vastu32.label(this.trueHeading) + "\n" + verdict.summary());
    }

    @Override // com.tiscan.app.RoomRecognizer.Listener
    public void onRoomSuggestion(final String room, final float confidence, final String evidence) {
        runOnUiThread(new Runnable() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$onRoomSuggestion$12(room, confidence, evidence);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onRoomSuggestion$12(String room, float confidence, String evidence) {
        this.latestAi = room;
        String display = String.format(Locale.US, "AI room: %s • %.0f%%", room, Float.valueOf(confidence * 100.0f));
        this.aiText.setText(display);
        this.overlay.setAi(display);
        if (room.equals(this.stableRoom)) {
            this.stableCount++;
        } else {
            this.stableRoom = room;
            this.stableCount = 1;
        }
        if (this.recording == null || this.stableCount < 4) {
            return;
        }
        if ((room.equals("Kitchen") || room.equals("Washroom")) && !this.autoTagged.contains(room)) {
            this.autoTagged.add(room);
            addMarker(room, "AI suggestion from " + evidence + " (" + Math.round(100.0f * confidence) + "%)");
        }
    }

    private void toggleVoice() {
        if (this.voiceModeOn) {
            stopVoice();
        } else {
            startVoiceLoop();
        }
    }

    private void startVoiceLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Speech recognition is unavailable on this phone.");
            return;
        }
        if (!has("android.permission.RECORD_AUDIO")) {
            toast("Microphone permission is required for voice tagging.");
            return;
        }
        this.voiceModeOn = true;
        this.voiceButton.setText("🎤 Listening…");
        listenOnceMain();
    }

    private void listenOnceMain() {
        if (!this.voiceModeOn) {
            return;
        }
        if (this.speech != null) {
            this.speech.destroy();
        }
        this.speech = SpeechRecognizer.createSpeechRecognizer(this);
        this.speech.setRecognitionListener(new RecognitionListener() { // from class: com.tiscan.app.MainActivity.3
            @Override // android.speech.RecognitionListener
            public void onResults(Bundle b) {
                ArrayList<String> a = b.getStringArrayList("results_recognition");
                if (a != null && !a.isEmpty()) {
                    MainActivity.this.parseVoice(a.get(0));
                }
                relisten();
            }

            @Override // android.speech.RecognitionListener
            public void onBeginningOfSpeech() {
            }

            @Override // android.speech.RecognitionListener
            public void onBufferReceived(byte[] b) {
            }

            @Override // android.speech.RecognitionListener
            public void onEndOfSpeech() {
            }

            @Override // android.speech.RecognitionListener
            public void onError(int e) {
                relisten();
            }

            @Override // android.speech.RecognitionListener
            public void onEvent(int t, Bundle b) {
            }

            @Override // android.speech.RecognitionListener
            public void onPartialResults(Bundle b) {
            }

            @Override // android.speech.RecognitionListener
            public void onReadyForSpeech(Bundle b) {
            }

            @Override // android.speech.RecognitionListener
            public void onRmsChanged(float r) {
            }

            private void relisten() {
                if (MainActivity.this.voiceModeOn) {
                    new Handler(Looper.getMainLooper()).postDelayed(MainActivity.this::listenOnceMain, 300);
                }
            }
        });
        Intent i = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        i.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        i.putExtra("android.speech.extra.PREFER_OFFLINE", true);
        i.putExtra("android.speech.extra.PROMPT", "Say a room name, or say note followed by your note");
        this.speech.startListening(i);
    }

    private void stopVoice() {
        this.voiceModeOn = false;
        if (this.speech != null) {
            this.speech.stopListening();
            this.speech.destroy();
            this.speech = null;
        }
        this.voiceButton.setText("🎤 Voice");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void parseVoice(String raw) {
        String s = raw.toLowerCase(Locale.US);
        String[] names = {"master bedroom", "kids bedroom", "children bedroom", "guest room",
            "main door", "store room", "storage", "prayer room", "kitchen", "washroom",
            "bathroom", "toilet", "entrance", "bedroom", "puja", "pooja", "living", "hall",
            "dining", "study", "staircase", "stairs", "balcony"};
        for (String n : names) {
            if (s.contains(n)) {
                String label = voiceLabelFor(n);
                addMarker(label, "Voice tag: " + raw);
                return;
            }
        }
        this.surveyNote = s.startsWith("note") ? raw.substring(Math.min(4, raw.length())).trim() : raw;
        toast("Voice note saved.");
    }

    private static String voiceLabelFor(String n) {
        switch (n) {
            case "bathroom": case "toilet": return "Washroom";
            case "main door": return "Entrance";
            case "children bedroom": return "Kids Bedroom";
            case "storage": return "Store Room";
            case "prayer room": case "pooja": return "Puja";
            case "hall": return "Living";
            case "stairs": return "Staircase";
            default:
                String[] words = n.split(" ");
                StringBuilder out = new StringBuilder();
                for (String w : words) {
                    if (out.length() > 0) out.append(' ');
                    out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
                }
                return out.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$note$13(EditText e, DialogInterface d, int w) {
        this.surveyNote = e.getText().toString().trim();
        toast("Note saved.");
    }

    private void note() {
        final EditText e = new EditText(this);
        e.setText(this.surveyNote);
        e.setHint("Property/survey observation");
        new AlertDialog.Builder(this).setTitle("Survey note").setView(e).setPositiveButton("Save", new DialogInterface.OnClickListener() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda13
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$note$13(e, dialogInterface, i);
            }
        }).setNegativeButton("Cancel", (DialogInterface.OnClickListener) null).show();
    }

    private void takePhoto() {
        if (this.videoCapture == null) {
            toast("Camera is not ready yet.");
        } else {
            final Bitmap bitmap = Bitmap.createBitmap(Math.max(1, getWindow().getDecorView().getWidth()), Math.max(1, getWindow().getDecorView().getHeight()), Bitmap.Config.ARGB_8888);
            PixelCopy.request(getWindow(), bitmap, new PixelCopy.OnPixelCopyFinishedListener() { // from class: com.tiscan.app.MainActivity$$ExternalSyntheticLambda2
                @Override // android.view.PixelCopy.OnPixelCopyFinishedListener
                public final void onPixelCopyFinished(int i) {
                    this.f$0.lambda$takePhoto$14(bitmap, i);
                }
            }, this.handler);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$takePhoto$14(Bitmap bitmap, int result) {
        if (result != 0) {
            bitmap.recycle();
            toast("Screenshot failed. Try again after the camera is fully live.");
            return;
        }
        try {
            ContentValues v = new ContentValues();
            v.put("_display_name", "VastuShot_" + System.currentTimeMillis() + ".jpg");
            v.put("mime_type", MimeTypes.IMAGE_JPEG);
            v.put("relative_path", "Pictures/VastuSurvey");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                throw new IllegalStateException("Could not create screenshot");
            }
            OutputStream out = getContentResolver().openOutputStream(uri);
            try {
                if (out == null) {
                    throw new IllegalStateException("Could not open screenshot");
                }
                bitmap.compress(Bitmap.CompressFormat.JPEG, 94, out);
                if (out != null) {
                    out.close();
                }
                bitmap.recycle();
                this.lastScreenshotUri = uri.toString();
                toast("Vastu screenshot saved with live overlay.");
            } catch (Throwable th) {
                if (out != null) {
                    try {
                        out.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            bitmap.recycle();
            toast("Screenshot save failed: " + e.getMessage());
        }
    }

    private void openArMeasure() {
        Intent i = new Intent(this, (Class<?>) ARScanActivity.class);
        i.putExtra("propertyId", this.property.id);
        startActivity(i);
    }

    private void openChakra() {
        Intent i = new Intent(this, (Class<?>) VastuChakraActivity.class);
        i.putExtra("heading", this.magneticHeading);
        startActivity(i);
    }

    private void openFloorPlan() {
        Intent i = new Intent(this, (Class<?>) FloorPlanActivity.class);
        i.putExtra("propertyId", this.property.id);
        List<File> fs = new SurveySessionStore(this).list(this.property.id);
        if (!fs.isEmpty()) {
            i.putExtra("sessionFile", fs.get(0).getAbsolutePath());
        }
        startActivity(i);
    }

    private void openHistory() {
        Intent i = new Intent(this, (Class<?>) HistoryActivity.class);
        i.putExtra("propertyId", this.property.id);
        startActivity(i);
    }

    private void openLatestReport() {
        List<File> fs = new SurveySessionStore(this).list(this.property.id);
        if (fs.isEmpty()) {
            toast("Record and save a survey first.");
            return;
        }
        Intent i = new Intent(this, (Class<?>) ReportActivity.class);
        i.putExtra("sessionFile", fs.get(0).getAbsolutePath());
        startActivity(i);
    }

    @Override // com.tiscan.app.CompassManager.Listener
    public void onHeading(float heading, int accuracy, boolean reliable) {
        TextView textView;
        int i;
        this.magneticHeading = heading;
        this.sensorAccuracy = accuracy;
        this.headingReliable = reliable;

        if (reliable) {
            float declination = 0.0f;
            if (this.lastLocation != null) {
                GeomagneticField g = new GeomagneticField((float) this.lastLocation.getLatitude(), (float) this.lastLocation.getLongitude(), (float) this.lastLocation.getAltitude(), System.currentTimeMillis());
                declination = g.getDeclination();
            }
            this.trueHeading = VastuDirection.normalize(heading + declination);
            if (this.floor != null) {
                this.floor.setHeading(this.trueHeading);
            }
            this.overlay.setHeading(this.trueHeading);
            this.headingText.setText(String.format(Locale.US, "%.1f° %s  •  %s", Float.valueOf(this.trueHeading), VastuDirection.sector(this.trueHeading), Vastu32.label(this.trueHeading)));

            String sector16 = VastuDirection.sector(this.trueHeading);
            LifeAspectZones.Zone zone = this.lifeAspectZones.get(sector16);
            if (zone != null) {
                this.zoneAspectText.setText(zone.aspectLine());
                this.zoneMetaText.setText("Element: " + zone.element + "  ·  Dosha: " + zone.dosha);
            }
        }
        // Below stays unconditional — the user should always see *why* the reading is frozen.

        TextView textView2 = this.qualityText;
        if (!reliable) {
            textView2.setText("Compass: point camera toward the room, not floor/ceiling");
            textView = this.qualityText;
            i = -16121;
        } else if (accuracy <= 1) {
            textView2.setText("Compass: LOW accuracy — calibrate / move away from metal");
            textView = this.qualityText;
            i = -36797;
        } else {
            textView2.setText("Compass: STABLE");
            textView = this.qualityText;
            i = -4589878;
        }
        textView.setTextColor(i);
    }

    @Override // com.tiscan.app.CompassManager.Listener
    public void onSensorUnavailable() {
        this.qualityText.setText("Compass sensor unavailable on this phone");
        this.qualityText.setTextColor(-44462);
    }

    @Override // com.tiscan.app.LocationTracker.Listener
    public void onLocation(Location l) {
        String q;
        int i;
        this.lastLocation = l;
        float a = l.getAccuracy();
        if (a <= 10.0f) {
            q = "EXCELLENT";
        } else if (a <= 25.0f) {
            q = "GOOD";
        } else {
            q = a <= 50.0f ? "FAIR" : "LOW";
        }
        this.gpsText.setText(String.format(Locale.US, "GPS %s: %.6f, %.6f  ±%.0fm", q, Double.valueOf(l.getLatitude()), Double.valueOf(l.getLongitude()), Float.valueOf(a)));
        TextView textView = this.gpsText;
        if (a <= 25.0f) {
            i = -4589878;
        } else {
            i = a <= 50.0f ? -10929 : -30107;
        }
        textView.setTextColor(i);
    }

    @Override // com.tiscan.app.LocationTracker.Listener
    public void onLocationStatus(String s) {
        if (this.lastLocation == null) {
            this.gpsText.setText("GPS: " + s);
        }
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.compass != null) {
            this.compass.start();
        }
        if (this.locationTracker != null) {
            this.locationTracker.start();
        }
        if (has("android.permission.CAMERA") && this.videoCapture == null) {
            startCamera();
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        super.onPause();
        if (this.compass != null) {
            this.compass.stop();
        }
        if (this.locationTracker != null) {
            this.locationTracker.stop();
        }
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        super.onDestroy();
        if (this.roomRecognizer != null) {
            this.roomRecognizer.close();
        }
        if (this.speech != null) {
            this.speech.destroy();
        }
        if (this.analysisExecutor != null) {
            this.analysisExecutor.shutdownNow();
        }
        this.handler.removeCallbacks(this.sampler);
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(-1);
        if (bold) {
            t.setTypeface(null, 1);
        }
        return t;
    }

    private Button smallButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(12.0f);
        b.setAlpha(0.92f);
        return b;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$tagButton$15(String s, View v) {
        addMarker(s, "Manual tag");
    }

    private Button tagButton(final String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(12f);
        b.setTextColor(EMERALD_TEXT);
        b.setBackground(cardBg(EMERALD_BG, EMERALD_BORDER, 20));
        b.setOnClickListener(v -> addMarker(s, "Manual tag"));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, dp(40));
        p.setMargins(dp(3), dp(3), dp(3), dp(3));
        b.setLayoutParams(p);
        return b;
    }

    private TextView mono(String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        return t;
    }

    private GradientDrawable cardBg(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }

    private Button railButton(String label, boolean accent) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(10.5f);
        b.setTextColor(accent ? GOLD : CYAN);
        b.setBackground(cardBg(CARD_BG, accent ? GOLD : CARD_BORDER, 9));
        return b;
    }

    private LinearLayout.LayoutParams railLp(int heightDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(72), dp(heightDp));
        p.bottomMargin = dp(6);
        return p;
    }

    private Button toolButton(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(11f);
        b.setTextColor(TEXT_PRIMARY);
        b.setBackground(cardBg(CARD_BG, CARD_BORDER, 8));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
        p.setMargins(dp(3), 0, dp(3), 0);
        b.setLayoutParams(p);
        return b;
    }

    private int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String s) {
        Toast.makeText(this, s, 1).show();
    }
}
