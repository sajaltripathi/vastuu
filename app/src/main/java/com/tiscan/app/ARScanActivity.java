package com.tiscan.app;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.camera.video.AudioStats;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.media3.common.MimeTypes;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes3.dex */
public class ARScanActivity extends ComponentActivity implements GLSurfaceView.Renderer, SensorEventListener {
    private float autoAreaM2;
    private TextView autoEstimate;
    private ARBackgroundRenderer bg;
    private boolean depthEnabled;
    private TextView depthText;
    private GLSurfaceView gl;
    private int height;
    private boolean installRequested;
    private long lastProjectionUiMs;
    private TextView levelText;
    private TextView measure;
    private TextView modeText;
    private MeasureOverlay overlay;
    private float pitchDeg;
    private String propertyId;
    private float rollDeg;
    private Sensor rotationSensor;
    private SensorManager sensorManager;
    private Session session;
    private TextView status;
    private int trackedPlanes;
    private int width;
    private final List<Anchor> anchors = new ArrayList();
    private volatile float tapX = -1.0f;
    private volatile float tapY = -1.0f;
    private Mode mode = Mode.LINE;
    private int unit = 0;

    private boolean walking = false;
    private Plane walkNearestPlane = null;
    private int walkNearestStreak = 0;
    private float[] walkLastPoint = null;
    private final List<float[]> walkPath = new ArrayList<>();
    private static final int WALK_DEBOUNCE_FRAMES = 15;
    private static final float WALK_MIN_STEP_M = 0.4f;

    private SpeechRecognizer speech;
    private boolean voiceModeOn = false;
    private Button arVoiceButton;

    private enum Mode {
        LINE,
        RECT,
        POLYGON,
        WALK
    }

    @Override // androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        getWindow().setStatusBarColor(ViewCompat.MEASURED_STATE_MASK);
        getWindow().setNavigationBarColor(ViewCompat.MEASURED_STATE_MASK);
        this.propertyId = getIntent().getStringExtra("propertyId");
        this.sensorManager = (SensorManager) getSystemService("sensor");
        this.rotationSensor = this.sensorManager.getDefaultSensor(11);
        buildUi();
    }

    private void buildUi() {
        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setBackgroundColor(ViewCompat.MEASURED_STATE_MASK);
        this.gl = new GLSurfaceView(this);
        this.gl.setPreserveEGLContextOnPause(true);
        this.gl.setEGLContextClientVersion(2);
        this.gl.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.gl.setRenderer(this);
        this.gl.setRenderMode(1);
        this.gl.setOnTouchListener(new View.OnTouchListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda6
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view, MotionEvent motionEvent) {
                return this.f$0.lambda$buildUi$0(view, motionEvent);
            }
        });
        frameLayout.addView(this.gl, new FrameLayout.LayoutParams(-1, -1));
        this.overlay = new MeasureOverlay();
        this.overlay.setLayerType(1, null);
        frameLayout.addView(this.overlay, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(1);
        top.setPadding(dp(14), dp(10), dp(14), dp(10));
        top.setBackgroundColor(-1476395008);
        TextView title = text("AR MEASURE + VASTU MAPPING", 19, true);
        top.addView(title);
        this.status = text("Move slowly so AR can lock surfaces…", 13, false);
        top.addView(this.status);
        this.modeText = text("Mode: LINE — aim crosshair and tap + for start/end", 14, true);
        this.modeText.setTextColor(-10929);
        top.addView(this.modeText);
        this.measure = text("No measurement yet", 14, true);
        top.addView(this.measure);
        this.autoEstimate = text("Detected surface estimate: scanning…", 12, false);
        top.addView(this.autoEstimate);
        this.depthText = text("Depth: checking device…", 12, false);
        top.addView(this.depthText);
        this.levelText = text("Level: stabilizing…", 12, false);
        top.addView(this.levelText);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2);
        tp.gravity = 48;
        frameLayout.addView(top, tp);
        TextView cross = text("＋", 44, false);
        cross.setGravity(17);
        cross.setTextColor(-285212673);
        cross.setBackgroundColor(855638016);
        cross.setOnClickListener(v -> {
            if (this.mode == Mode.WALK) {
                toggleWalking();
            } else {
                requestCenterMeasurement();
            }
        });
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(72), dp(72));
        cp.gravity = 17;
        frameLayout.addView(cross, cp);
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(1);
        linearLayout.setPadding(dp(8), dp(6), dp(8), dp(8));
        linearLayout.setBackgroundColor(-1207959552);
        LinearLayout modes = new LinearLayout(this);
        modes.setGravity(17);
        Button line = button("LINE");
        line.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda8
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$2(view);
            }
        });
        modes.addView(line, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button rect = button("AUTO RECT");
        rect.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$3(view);
            }
        });
        modes.addView(rect, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button poly = button("POLYGON");
        poly.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$4(view);
            }
        });
        modes.addView(poly, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        Button walk = button("WALK");
        walk.setOnClickListener(v -> setMode(Mode.WALK));
        modes.addView(walk, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        final Button units = button("UNIT: m");
        units.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda11
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$5(units, view);
            }
        });
        modes.addView(units, new LinearLayout.LayoutParams(0, dp(48), 1.0f));
        linearLayout.addView(modes);
        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(17);
        Button undo = button("UNDO");
        undo.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$6(view);
            }
        });
        actions.addView(undo, new LinearLayout.LayoutParams(0, dp(50), 1.0f));
        Button clear = button("CLEAR");
        clear.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$7(view);
            }
        });
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(50), 1.0f));
        Button shot = button("SHOT");
        shot.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda14
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$8(view);
            }
        });
        actions.addView(shot, new LinearLayout.LayoutParams(0, dp(50), 1.0f));
        Button save = button("SAVE MAP");
        save.setOnClickListener(new View.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                this.f$0.lambda$buildUi$9(view);
            }
        });
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(50), 1.0f));
        this.arVoiceButton = button("🎤");
        this.arVoiceButton.setOnClickListener(v -> toggleArVoice());
        actions.addView(this.arVoiceButton, new LinearLayout.LayoutParams(0, dp(50), 1.0f));
        linearLayout.addView(actions);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-1, -2);
        bp.gravity = 80;
        bp.setMargins(0, 0, 0, dp(30));
        frameLayout.addView(linearLayout, bp);
        setContentView(frameLayout);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ boolean lambda$buildUi$0(View v, MotionEvent e) {
        if (e.getAction() == 1) {
            this.tapX = e.getX();
            this.tapY = e.getY();
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$1(View v) {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }
        this.tapX = this.width / 2.0f;
        this.tapY = this.height / 2.0f;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$2(View v) {
        setMode(Mode.LINE);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$3(View v) {
        setMode(Mode.RECT);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$4(View v) {
        setMode(Mode.POLYGON);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$5(Button units, View v) {
        String str;
        this.unit = (this.unit + 1) % 3;
        if (this.unit == 0) {
            str = "UNIT: m";
        } else {
            str = this.unit == 1 ? "UNIT: cm" : "UNIT: ft";
        }
        units.setText(str);
        refreshMeasure();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$6(View v) {
        undo();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$7(View v) {
        clearPoints();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$8(View v) {
        takeMeasurementScreenshot();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$buildUi$9(View v) {
        save();
    }

    private void setMode(Mode m) {
        if (this.mode == Mode.WALK && m != Mode.WALK && this.walking) {
            toggleWalking();
        }
        this.mode = m;
        clearPoints();
        this.walkPath.clear();
        this.walkNearestPlane = null;
        this.walkNearestStreak = 0;
        this.walkLastPoint = null;
        if (m == Mode.RECT) {
            this.modeText.setText("Mode: AUTO RECT — aim at a plane and tap +");
        } else if (m == Mode.POLYGON) {
            this.modeText.setText("Mode: POLYGON — aim and tap + at every corner");
        } else if (m == Mode.WALK) {
            this.modeText.setText("Mode: WALK — walk close to the walls, corners are detected automatically. Tap + to start.");
        } else {
            this.modeText.setText("Mode: LINE — aim crosshair and tap + for start/end");
        }
    }

    private void requestCenterMeasurement() {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }
        this.tapX = this.width / 2.0f;
        this.tapY = this.height / 2.0f;
    }

    @Override // android.app.Activity
    protected void onResume() {
        super.onResume();
        if (this.rotationSensor != null) {
            this.sensorManager.registerListener(this, this.rotationSensor, 2);
        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.CAMERA") != 0) {
            requestPermissions(new String[]{"android.permission.CAMERA"}, 41);
            return;
        }
        if (this.session != null || createSession()) {
            try {
                this.session.resume();
                this.gl.onResume();
            } catch (Exception e) {
                showError("AR camera unavailable: " + safe(e));
            }
        }
    }

    @Override // android.app.Activity
    protected void onPause() {
        this.sensorManager.unregisterListener(this);
        stopArVoice();
        this.gl.onPause();
        if (this.session != null) {
            try {
                this.session.pause();
            } catch (Exception e) {
            }
        }
        super.onPause();
    }

    @Override // android.app.Activity
    protected void onDestroy() {
        clearPointsOnRenderThread();
        if (this.session != null) {
            try {
                this.session.close();
            } catch (Exception e) {
            }
            this.session = null;
        }
        super.onDestroy();
    }

    private boolean createSession() {
        try {
            ArCoreApk.InstallStatus s = ArCoreApk.getInstance().requestInstall(this, !this.installRequested);
            if (s == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
                this.installRequested = true;
                return false;
            }
            this.session = new Session(this);
            Config c = new Config(this.session);
            c.setPlaneFindingMode(Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL);
            this.depthEnabled = this.session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
            if (this.depthEnabled) {
                c.setDepthMode(Config.DepthMode.AUTOMATIC);
            }
            c.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            this.session.configure(c);
            this.depthText.setText(this.depthEnabled ? "Depth: ARCore Depth enabled" : "Depth: not supported — plane tracking fallback active");
            this.depthText.setTextColor(this.depthEnabled ? -4589878 : -10929);
            return true;
        } catch (Exception e) {
            showError("ARCore is unavailable on this phone: " + safe(e));
            return false;
        }
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int req, String[] p, int[] g) {
        super.onRequestPermissionsResult(req, p, g);
        if (req == 42) {
            if (g.length > 0 && g[0] == 0) {
                startArVoiceLoop();
            } else {
                Toast.makeText(this, "Microphone permission denied — voice control stays off.", 0).show();
            }
            return;
        }
        if (req != 41 || g.length <= 0 || g[0] != 0) {
            showError("Camera permission is required for AR measurement.");
        } else {
            onResume();
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        this.bg = new ARBackgroundRenderer();
        int tex = this.bg.createOnGlThread();
        if (this.session != null) {
            this.session.setCameraTextureName(tex);
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onSurfaceChanged(GL10 unused, int w, int h) {
        this.width = w;
        this.height = h;
        GLES20.glViewport(0, 0, w, h);
        if (this.session != null) {
            this.session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), w, h);
        }
    }

    @Override // android.opengl.GLSurfaceView.Renderer
    public void onDrawFrame(GL10 unused) throws Throwable {
        GLES20.glClear(16640);
        if (this.session == null || this.bg == null) {
            return;
        }
        try {
            this.session.setCameraTextureName(this.bg.getTextureId());
            this.session.setDisplayGeometry(getWindowManager().getDefaultDisplay().getRotation(), this.width, this.height);
            Frame frame = this.session.update();
            this.bg.draw(frame);
            updatePlanes();
            if (this.mode == Mode.WALK && this.walking && frame.getCamera().getTrackingState() == TrackingState.TRACKING) {
                updateWalkthrough(frame);
            }
            updateProjection(frame);
            if (this.tapX < 0.0f || this.tapY < 0.0f) {
                return;
            }
            float x = this.tapX;
            float y = this.tapY;
            this.tapY = -1.0f;
            this.tapX = -1.0f;
            place(frame, x, y);
        } catch (Exception e) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$place$10() {
        this.status.setText("Tracking paused — move phone slowly and keep surfaces visible");
    }

    private void updateWalkthrough(Frame frame) {
        Pose camPose = frame.getCamera().getPose();

        Plane nearest = null;
        float nearestDist = Float.MAX_VALUE;
        java.util.HashSet<Plane> seenRoots = new java.util.HashSet<>();
        for (Plane p : this.session.getAllTrackables(Plane.class)) {
            if (p.getType() != Plane.Type.VERTICAL || p.getTrackingState() != TrackingState.TRACKING) continue;
            Plane root = p;
            while (root.getSubsumedBy() != null) root = root.getSubsumedBy();
            if (!seenRoots.add(root)) continue;
            Pose c = root.getCenterPose();
            float dx = c.tx() - camPose.tx();
            float dz = c.tz() - camPose.tz();
            float d = (float) Math.sqrt(dx * dx + dz * dz);
            if (d < nearestDist) {
                nearestDist = d;
                nearest = root;
            }
        }
        if (nearest == null) return;

        if (nearest == this.walkNearestPlane) {
            this.walkNearestStreak++;
        } else {
            this.walkNearestPlane = nearest;
            this.walkNearestStreak = 1;
        }

        boolean debounceOk = this.walkNearestStreak >= WALK_DEBOUNCE_FRAMES;
        boolean stepOk = true;
        if (this.walkLastPoint != null) {
            float dx = camPose.tx() - this.walkLastPoint[0];
            float dz = camPose.tz() - this.walkLastPoint[2];
            stepOk = Math.sqrt(dx * dx + dz * dz) >= WALK_MIN_STEP_M;
        }

        if (debounceOk && stepOk) {
            float[] pt = {camPose.tx(), camPose.ty(), camPose.tz()};
            this.walkPath.add(pt);
            this.walkLastPoint = pt;
            this.walkNearestStreak = 0;
            final int n = this.walkPath.size();
            runOnUiThread(() -> this.status.setText("Walking — " + n + " corner(s) marked. Keep close to the walls."));
        }
    }

    private void toggleWalking() {
        if (!this.walking) {
            this.walking = true;
            this.walkPath.clear();
            this.walkNearestPlane = null;
            this.walkNearestStreak = 0;
            this.walkLastPoint = null;
            this.status.setText("Walking — move along the walls. Tap + again to stop.");
            return;
        }
        this.walking = false;
        if (this.walkPath.size() < 3) {
            this.status.setText("Only " + this.walkPath.size() + " corner(s) detected — walk a full loop and try again.");
            this.walkPath.clear();
            return;
        }
        this.gl.queueEvent(() -> {
            clearPointsOnRenderThread();
            synchronized (this.anchors) {
                for (float[] pt : this.walkPath) {
                    this.anchors.add(this.session.createAnchor(Pose.makeTranslation(pt[0], pt[1], pt[2])));
                }
            }
            final int n = this.walkPath.size();
            this.walkPath.clear();
            runOnUiThread(() -> {
                this.status.setText("Review " + n + " detected corners — UNDO removes the last one, SAVE MAP when it looks right.");
                refreshMeasure();
            });
        });
    }

    private void toggleArVoice() {
        if (this.voiceModeOn) {
            stopArVoice();
        } else {
            startArVoiceLoop();
        }
    }

    private void startArVoiceLoop() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is unavailable on this phone.", 0).show();
            return;
        }
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != 0) {
            requestPermissions(new String[]{"android.permission.RECORD_AUDIO"}, 42);
            return;
        }
        this.voiceModeOn = true;
        this.arVoiceButton.setText("🎤 On");
        listenOnceAr();
    }

    private void listenOnceAr() {
        if (!this.voiceModeOn) return;
        if (this.speech != null) this.speech.destroy();
        this.speech = SpeechRecognizer.createSpeechRecognizer(this);
        this.speech.setRecognitionListener(new RecognitionListener() {
            @Override public void onResults(Bundle b) {
                ArrayList<String> a = b.getStringArrayList("results_recognition");
                if (a != null && !a.isEmpty()) {
                    parseArVoiceCommand(a.get(0));
                }
                relisten();
            }
            @Override public void onError(int e) { relisten(); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onEvent(int t, Bundle b) {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onReadyForSpeech(Bundle b) {}
            @Override public void onRmsChanged(float r) {}

            private void relisten() {
                if (ARScanActivity.this.voiceModeOn) {
                    new Handler(Looper.getMainLooper()).postDelayed(ARScanActivity.this::listenOnceAr, 300);
                }
            }
        });
        Intent i = new Intent("android.speech.action.RECOGNIZE_SPEECH");
        i.putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form");
        i.putExtra("android.speech.extra.PREFER_OFFLINE", true);
        this.speech.startListening(i);
    }

    private void stopArVoice() {
        this.voiceModeOn = false;
        if (this.speech != null) {
            this.speech.stopListening();
            this.speech.destroy();
            this.speech = null;
        }
        this.arVoiceButton.setText("🎤");
    }

    private void parseArVoiceCommand(String raw) {
        String s = raw.toLowerCase(Locale.US);
        if (s.contains("start walk")) {
            if (this.mode != Mode.WALK) setMode(Mode.WALK);
            if (!this.walking) toggleWalking();
        } else if (s.contains("stop walk")) {
            if (this.mode == Mode.WALK && this.walking) toggleWalking();
        } else if (s.contains("walk")) {
            setMode(Mode.WALK);
        } else if (s.contains("rectangle")) {
            setMode(Mode.RECT);
        } else if (s.contains("polygon")) {
            setMode(Mode.POLYGON);
        } else if (s.contains("line")) {
            setMode(Mode.LINE);
        } else if (s.contains("mark") || s.contains("corner")) {
            requestCenterMeasurement();
        } else if (s.contains("undo")) {
            undo();
        } else if (s.contains("clear")) {
            clearPoints();
        } else if (s.contains("save")) {
            save();
        }
    }

    private void place(Frame frame, float x, float y) {
        if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$place$10();
                }
            });
            return;
        }
        for (HitResult hit : frame.hitTest(x, y)) {
            Trackable t = hit.getTrackable();
            if (t instanceof Plane) {
                Plane plane = (Plane) t;
                if (plane.isPoseInPolygon(hit.getHitPose())) {
                    if (this.mode == Mode.RECT) {
                        fitPlaneRectangle(plane);
                        return;
                    }
                    if (this.mode == Mode.LINE && this.anchors.size() >= 2) {
                        clearPoints();
                    }
                    Anchor a = hit.createAnchor();
                    synchronized (this.anchors) {
                        this.anchors.add(a);
                    }
                    runOnUiThread(new ARScanActivity$$ExternalSyntheticLambda2(this));
                    return;
                }
            }
        }
        runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$place$11();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$place$11() {
        Toast.makeText(this, "Surface not locked there yet. Move slowly, aim at a textured surface, then tap.", 0).show();
    }

    private void fitPlaneRectangle(Plane plane) {
        clearPointsOnRenderThread();
        Pose center = plane.getCenterPose();
        float hx = Math.max(0.03f, plane.getExtentX() * 0.48f);
        float hz = Math.max(0.03f, plane.getExtentZ() * 0.48f);
        char c = 0;
        char c2 = 1;
        float[][] local = {new float[]{-hx, 0.0f, -hz}, new float[]{hx, 0.0f, -hz}, new float[]{hx, 0.0f, hz}, new float[]{-hx, 0.0f, hz}};
        synchronized (this.anchors) {
            int length = local.length;
            int i = 0;
            while (i < length) {
                float[] q = local[i];
                float[] world = center.transformPoint(q);
                char c3 = c2;
                this.anchors.add(this.session.createAnchor(Pose.makeTranslation(world[c], world[c3], world[2])));
                i++;
                c2 = c3;
                c = 0;
            }
        }
        runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$fitPlaneRectangle$12();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$fitPlaneRectangle$12() {
        this.status.setText("Rectangle fitted to detected AR plane");
        refreshMeasure();
    }

    private void updatePlanes() {
        float best = 0.0f;
        int count = 0;
        for (Plane p : this.session.getAllTrackables(Plane.class)) {
            if (p.getTrackingState() == TrackingState.TRACKING) {
                count++;
                best = Math.max(best, p.getExtentX() * p.getExtentZ());
            }
        }
        if (Math.abs(best - this.autoAreaM2) > 0.05f || count != this.trackedPlanes) {
            this.autoAreaM2 = best;
            this.trackedPlanes = count;
            runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$updatePlanes$13();
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updatePlanes$13() {
        this.status.setText(this.trackedPlanes > 0 ? "AR tracking locked • choose a measure mode" : "Move slowly and scan textured surfaces");
        this.autoEstimate.setText(String.format(Locale.US, "Largest detected plane: %.2f m² • %d plane(s)", Float.valueOf(this.autoAreaM2), Integer.valueOf(this.trackedPlanes)));
    }

    private void updateProjection(Frame frame) throws Throwable {
        long now = SystemClock.elapsedRealtime();
        if (now - this.lastProjectionUiMs < 100) {
            return;
        }
        this.lastProjectionUiMs = now;
        float[] view = new float[16];
        float[] proj = new float[16];
        char c = 0;
        frame.getCamera().getViewMatrix(view, 0);
        frame.getCamera().getProjectionMatrix(proj, 0, 0.1f, 100.0f);
        ArrayList<float[]> screen = new ArrayList<>();
        synchronized (this.anchors) {
            try {
                for (Anchor a : this.anchors) {
                    Pose p = a.getPose();
                    float fTx = p.tx();
                    float fTy = p.ty();
                    float fTz = p.tz();
                    float[] world = new float[4];
                    world[c] = fTx;
                    world[1] = fTy;
                    world[2] = fTz;
                    world[3] = 1.0f;
                    float[] cam = new float[4];
                    float[] clip = new float[4];
                    Matrix.multiplyMV(cam, 0, view, 0, world, 0);
                    proj = proj;
                    try {
                        Matrix.multiplyMV(clip, 0, proj, 0, cam, 0);
                        char c2 = c;
                        screen = screen;
                        if (Math.abs(clip[3]) < 1.0E-5d) {
                            c = c2;
                        } else {
                            try {
                                float nx = clip[c2] / clip[3];
                                float ny = clip[1] / clip[3];
                                float f = (nx + 1.0f) * this.width * 0.5f;
                                float f2 = (1.0f - ny) * this.height * 0.5f;
                                float[] fArr = new float[2];
                                fArr[c2] = f;
                                fArr[1] = f2;
                                screen.add(fArr);
                                c = c2;
                            } catch (Throwable th) {
                                th = th;
                                throw th;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                }
                final ArrayList<float[]> screen2 = screen;
                runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda18
                    @Override // java.lang.Runnable
                    public final void run() {
                        this.f$0.lambda$updateProjection$14(screen2);
                    }
                });
            } catch (Throwable th3) {
                th = th3;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$updateProjection$14(ArrayList screen) {
        this.overlay.setPoints(screen, this.mode == Mode.LINE ? 0 : 1);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshMeasure() {
        synchronized (this.anchors) {
            int n = this.anchors.size();
            if (n == 0) {
                this.measure.setText("No measurement yet");
                return;
            }
            double open = AudioStats.AUDIO_AMPLITUDE_NONE;
            for (int i = 1; i < n; i++) {
                open += distance(this.anchors.get(i - 1).getPose(), this.anchors.get(i).getPose());
            }
            if (this.mode == Mode.LINE) {
                this.measure.setText(n < 2 ? "Start point set — tap end point" : "Distance: " + fmtDistance(open));
                return;
            }
            double perimeter = open;
            if (n >= 3) {
                perimeter += distance(this.anchors.get(n - 1).getPose(), this.anchors.get(0).getPose());
            }
            double area = polygonAreaLocked();
            this.measure.setText(String.format(Locale.US, "Points %d • perimeter %s • area %s", Integer.valueOf(n), fmtDistance(perimeter), fmtArea(area)));
        }
    }

    private String fmtDistance(double m) {
        if (this.unit == 1) {
            return String.format(Locale.US, "%.1f cm", Double.valueOf(100.0d * m));
        }
        if (this.unit != 2) {
            return String.format(Locale.US, "%.3f m", Double.valueOf(m));
        }
        double ft = 3.280839895d * m;
        return Math.abs(ft) < 1.0d ? String.format(Locale.US, "%.1f in", Double.valueOf(39.37007874d * m)) : String.format(Locale.US, "%.2f ft", Double.valueOf(ft));
    }

    private String fmtArea(double m2) {
        if (this.unit == 1) {
            return String.format(Locale.US, "%.0f cm²", Double.valueOf(10000.0d * m2));
        }
        return this.unit == 2 ? String.format(Locale.US, "%.2f ft²", Double.valueOf(10.7639104d * m2)) : String.format(Locale.US, "%.2f m²", Double.valueOf(m2));
    }

    private static double distance(Pose a, Pose b) {
        float dx = a.tx() - b.tx();
        float dy = a.ty() - b.ty();
        float dz = a.tz() - b.tz();
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private double polygonAreaLocked() {
        if (this.anchors.size() < 3) {
            return AudioStats.AUDIO_AMPLITUDE_NONE;
        }
        double s = AudioStats.AUDIO_AMPLITUDE_NONE;
        for (int i = 0; i < this.anchors.size(); i++) {
            Pose a = this.anchors.get(i).getPose();
            Pose b = this.anchors.get((i + 1) % this.anchors.size()).getPose();
            s += (double) ((a.tx() * b.tz()) - (b.tx() * a.tz()));
        }
        return Math.abs(s) / 2.0d;
    }

    private double polygonArea() {
        double dPolygonAreaLocked;
        synchronized (this.anchors) {
            dPolygonAreaLocked = polygonAreaLocked();
        }
        return dPolygonAreaLocked;
    }

    private void undo() {
        if (this.gl == null) {
            return;
        }
        this.gl.queueEvent(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$undo$15();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$undo$15() {
        synchronized (this.anchors) {
            if (!this.anchors.isEmpty()) {
                Anchor a = this.anchors.remove(this.anchors.size() - 1);
                try {
                    a.detach();
                } catch (Exception e) {
                }
            }
        }
        runOnUiThread(new ARScanActivity$$ExternalSyntheticLambda2(this));
    }

    private void clearPoints() {
        if (this.gl != null) {
            this.gl.queueEvent(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    this.f$0.lambda$clearPoints$17();
                }
            });
            return;
        }
        clearPointsOnRenderThread();
        this.overlay.setPoints(new ArrayList(), 0);
        refreshMeasure();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$clearPoints$16() {
        this.overlay.setPoints(new ArrayList(), 0);
        refreshMeasure();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$clearPoints$17() {
        clearPointsOnRenderThread();
        runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$clearPoints$16();
            }
        });
    }

    private void clearPointsOnRenderThread() {
        synchronized (this.anchors) {
            for (Anchor a : this.anchors) {
                try {
                    a.detach();
                } catch (Exception e) {
                }
            }
            this.anchors.clear();
        }
    }

    private void save() {
        if (this.mode == Mode.LINE && this.anchors.size() < 2) {
            Toast.makeText(this, "Measure a line first.", 0).show();
            return;
        }
        if (this.mode != Mode.LINE && this.anchors.size() < 3) {
            Toast.makeText(this, "Add at least 3 corners.", 0).show();
            return;
        }
        try {
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("schemaVersion", 2);
            jSONObject.put("propertyId", this.propertyId);
            jSONObject.put("createdAt", System.currentTimeMillis());
            jSONObject.put("mode", this.mode.name());
            jSONObject.put("depthEnabled", this.depthEnabled);
            jSONObject.put("areaM2", polygonArea());
            jSONObject.put("autoPlaneEstimateM2", this.autoAreaM2);
            JSONArray a = new JSONArray();
            synchronized (this.anchors) {
                for (Anchor an : this.anchors) {
                    Pose p = an.getPose();
                    JSONObject q = new JSONObject();
                    q.put("x", p.tx());
                    q.put("y", p.ty());
                    q.put("z", p.tz());
                    a.put(q);
                }
            }
            jSONObject.put("corners", a);
            new ARScanStore(this).save(this.propertyId, jSONObject);
            Toast.makeText(this, "AR measurement saved.", 1).show();
        } catch (Exception e) {
            showError("Could not save map: " + safe(e));
        }
    }

    private void takeMeasurementScreenshot() {
        final Bitmap b = Bitmap.createBitmap(Math.max(1, getWindow().getDecorView().getWidth()), Math.max(1, getWindow().getDecorView().getHeight()), Bitmap.Config.ARGB_8888);
        PixelCopy.request(getWindow(), b, new PixelCopy.OnPixelCopyFinishedListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda16
            @Override // android.view.PixelCopy.OnPixelCopyFinishedListener
            public final void onPixelCopyFinished(int i) {
                this.f$0.lambda$takeMeasurementScreenshot$18(b, i);
            }
        }, new Handler(Looper.getMainLooper()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$takeMeasurementScreenshot$18(Bitmap b, int result) {
        if (result != 0) {
            Toast.makeText(this, "Screenshot failed.", 0).show();
            b.recycle();
            return;
        }
        try {
            ContentValues v = new ContentValues();
            v.put("_display_name", "ARMeasure_" + System.currentTimeMillis() + ".jpg");
            v.put("mime_type", MimeTypes.IMAGE_JPEG);
            v.put("relative_path", "Pictures/VastuSurvey/Measurements");
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) {
                throw new IllegalStateException("Could not create image");
            }
            OutputStream out = getContentResolver().openOutputStream(uri);
            try {
                if (out == null) {
                    throw new IllegalStateException("Could not open image");
                }
                b.compress(Bitmap.CompressFormat.JPEG, 94, out);
                if (out != null) {
                    out.close();
                }
                b.recycle();
                Toast.makeText(this, "Measurement screenshot saved.", 1).show();
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
            b.recycle();
            Toast.makeText(this, "Screenshot save failed: " + safe(e), 1).show();
        }
    }

    @Override // android.hardware.SensorEventListener
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != 11) {
            return;
        }
        float[] r = new float[9];
        float[] ori = new float[3];
        SensorManager.getRotationMatrixFromVector(r, event.values);
        SensorManager.getOrientation(r, ori);
        this.pitchDeg = (float) Math.toDegrees(ori[1]);
        this.rollDeg = (float) Math.toDegrees(ori[2]);
        float tilt = Math.max(Math.abs(this.pitchDeg), Math.abs(this.rollDeg));
        this.levelText.setText(String.format(Locale.US, "Level: pitch %.1f° • roll %.1f°%s", Float.valueOf(this.pitchDeg), Float.valueOf(this.rollDeg), ((double) tilt) < 1.0d ? " • LEVEL" : ""));
        this.levelText.setTextColor(((double) tilt) < 1.0d ? -4589878 : -1);
        this.overlay.setLevel(this.rollDeg, this.pitchDeg);
    }

    @Override // android.hardware.SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showError$19(DialogInterface d, int w) {
        finish();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$showError$20(String m) {
        new AlertDialog.Builder(this).setTitle("AR Measure").setMessage(m).setPositiveButton("Close", new DialogInterface.OnClickListener() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda20
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                this.f$0.lambda$showError$19(dialogInterface, i);
            }
        }).show();
    }

    private void showError(final String m) {
        runOnUiThread(new Runnable() { // from class: com.tiscan.app.ARScanActivity$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                this.f$0.lambda$showError$20(m);
            }
        });
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(-1);
        if (bold) {
            t.setTypeface(null, 1);
        }
        return t;
    }

    private Button button(String s) {
        Button b = new Button(this);
        b.setText(s);
        b.setTextSize(11.0f);
        b.setAllCaps(false);
        return b;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float dp(float x) {
        return getResources().getDisplayMetrics().density * x;
    }

    private static String safe(Throwable e) {
        String s = e == null ? null : e.getMessage();
        return s == null ? "unknown error" : s;
    }

    private class MeasureOverlay extends View {
        private int closePolygon;
        private final Paint p;
        private float pitch;
        private List<float[]> points;
        private float roll;

        MeasureOverlay() {
            super(ARScanActivity.this);
            this.p = new Paint(1);
            this.points = new ArrayList();
            setWillNotDraw(false);
        }

        void setPoints(List<float[]> v, int close) {
            this.points = v;
            this.closePolygon = close;
            invalidate();
        }

        void setLevel(float r, float pi) {
            this.roll = r;
            this.pitch = pi;
            invalidate();
        }

        @Override // android.view.View
        protected void onDraw(Canvas c) {
            this.p.setStrokeWidth(ARScanActivity.this.dp(3));
            this.p.setColor(-8978550);
            this.p.setStyle(Paint.Style.STROKE);
            for (int i = 1; i < this.points.size(); i++) {
                c.drawLine(this.points.get(i - 1)[0], this.points.get(i - 1)[1], this.points.get(i)[0], this.points.get(i)[1], this.p);
            }
            if (this.closePolygon == 1 && this.points.size() >= 3) {
                c.drawLine(this.points.get(this.points.size() - 1)[0], this.points.get(this.points.size() - 1)[1], this.points.get(0)[0], this.points.get(0)[1], this.p);
            }
            this.p.setStyle(Paint.Style.FILL);
            this.p.setTextAlign(Paint.Align.CENTER);
            this.p.setTextSize(ARScanActivity.this.dp(13));
            this.p.setFakeBoldText(true);
            for (int i2 = 0; i2 < this.points.size(); i2++) {
                float[] q = this.points.get(i2);
                this.p.setColor(-16090588);
                c.drawCircle(q[0], q[1], ARScanActivity.this.dp(11), this.p);
                this.p.setColor(-1);
                c.drawText(String.valueOf(i2 + 1), q[0], q[1] + ARScanActivity.this.dp(5), this.p);
            }
            int i3 = getWidth();
            float bx = i3 - ARScanActivity.this.dp(58);
            float by = ARScanActivity.this.dp(235);
            this.p.setStyle(Paint.Style.STROKE);
            this.p.setStrokeWidth(ARScanActivity.this.dp(2));
            this.p.setColor(-855638017);
            c.drawCircle(bx, by, ARScanActivity.this.dp(28), this.p);
            this.p.setStyle(Paint.Style.FILL);
            this.p.setColor(Math.max(Math.abs(this.roll), Math.abs(this.pitch)) >= 1.0f ? -10929 : -8978550);
            c.drawCircle((clamp(this.roll, -10.0f, 10.0f) * ARScanActivity.this.dp(1.7f)) + bx, (clamp(this.pitch, -10.0f, 10.0f) * ARScanActivity.this.dp(1.7f)) + by, ARScanActivity.this.dp(7), this.p);
        }

        private float clamp(float v, float lo, float hi) {
            return Math.max(lo, Math.min(hi, v));
        }
    }
}
