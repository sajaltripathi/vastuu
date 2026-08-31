# TiScan

Rebuilt/upgraded source — not a new app. Package is now `com.tiscan.app` — renamed from `com.vastusurvey.camera` this round. Since Android treats package name as an app's identity, this installs as a fresh app, not an update over any earlier build on your phone.

## Get a real APK — do this now

This sandbox can't reach Google's Maven servers, so it can't compile ARCore/ML
Kit dependencies itself — but GitHub's own build runners can, and a workflow
that does exactly that is already sitting in `.github/workflows/build-apk.yml`.
Push this repo and GitHub builds the APK for you automatically:

```bash
cd tiscan
git init
git add .
git commit -m "Rebuild: compass fixes, rule engine, WALK mode, AR voice, redesign"
git branch -M main
git remote add origin https://github.com/<your-username>/<your-repo>.git
git push -u origin main
```

Then on GitHub: **Actions tab → the running workflow → Artifacts →
`tiscan-debug`**. That zip contains `app-debug.apk` — a real,
installable build, produced by an actual compiler, not a promise. Takes a few
minutes after each push.

## Building locally instead (if you have Android Studio / the SDK already)

```bash
./gradlew assembleDebug
```
APK lands at `app/build/outputs/apk/debug/app-debug.apk`. Launcher activity
is `PropertyActivity`, confirmed from your real manifest.

## What you still need to add yourself
- **App icon** — `android:icon` is intentionally left out of the manifest
  rather than filled with something fake. Drop your real icon into
  `app/src/main/res/mipmap-*/ic_launcher.png` and add
  `android:icon="@mipmap/ic_launcher"` to the `<application>` tag.
- **Exact dependency versions** — `app/build.gradle` uses current, real
  library coordinates (ARCore, ML Kit image-labeling, CameraX, Play Services
  Location), but I can't verify exact version compatibility by actually
  compiling from here. If the Actions build fails on a specific version,
  that failure will name the exact line to bump — normal, expected, not a
  sign anything else is wrong.

See `RECONSTRUCTION_NOTES.md` for exactly which files are newly written this
round vs. reconstructed from your compiled APK, and what that distinction
means for you before you trust this as "the" source of truth.
