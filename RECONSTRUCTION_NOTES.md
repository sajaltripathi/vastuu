# Where this code actually came from

Stated once, plainly, so it doesn't get lost in the excitement of having a repo.

## Written new this conversation (trust these fully)
- `CompassManager.java` — pitch-instability fix
- `VastuRuleEngine.java` + `assets/vastu_rules.json` — the dosh-verdict engine
- `LifeAspectZones.java` + `assets/life_aspect_zones.json` — the Shakti-Chakra-style wheel
- All of `.github/workflows/`, the Gradle build files, and this README

## Patched with real, targeted edits on top of a decompiled base
- `MainActivity.java`, `ARScanActivity.java` — every change (true-heading fix,
  hands-free voice, the visual redesign, WALK mode, AR voice control) was
  written against line numbers and method names read directly from your
  actual compiled APK this round, not guessed. The parts of these two files
  I didn't touch, though, are still decompiler output underneath.

## Everything else in `java/com/tiscan/app/` (untouched)
`Vastu32.java`, `VastuDirection.java`, `SurveyMarker.java`,
`ARBackgroundRenderer.java`, `FloorPlanActivity.java`, and the rest —
**reconstructed by decompiling your compiled `app-debug.apk`, not your
original source.** Decompilation is lossy: variable names, comments, and
exact formatting from your real project are gone, and a handful of
`*$$ExternalSyntheticLambda*.java` files are decompiler-generated artifacts
(harmless, still referenced by untouched code, left in place on purpose).

**If you still have your real Android Studio project**, that's the one to
build from — copy the files listed in the first two sections above into it,
rather than the other way around. This repo exists so you have a complete,
pushable, buildable whole *right now*; it isn't a claim that every file in
it is byte-for-byte what you originally wrote.

## Manifest
Hand-written fresh from your real, extracted `AndroidManifest.xml` (package,
permissions, all 7 activities, launcher confirmed as `PropertyActivity`) —
but trimmed to only the entries a developer writes by hand. The long tail of
`<meta-data>`/`<provider>` entries visible in a compiled manifest (AndroidX
startup, WorkManager, Play Services version, datatransport) are injected
automatically by Gradle's manifest merger from the libraries themselves at
build time — copying those in by hand would double-declare them and break
the build, so they were deliberately left out, not missed.
