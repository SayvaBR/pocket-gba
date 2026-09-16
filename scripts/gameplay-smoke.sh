#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa/captures
TEST_APK=$(find qa/test -name '*.apk' -print -quit)
test -n "$TEST_APK"
test -s "$TEST_APK"
adb install -r "$TEST_APK"
adb shell am force-stop br.sayva.pocketlauncher || true
# Do not infer gameplay from a launcher screenshot. Real MIT ROM is inside the test APK,
# NOT the delivered Pocket APK. Tests must load it, step the mGBA core, open gameplay,
# and ensure save files are committed on exit.
adb shell am instrument -w -r br.sayva.pocketlauncher.test/androidx.test.runner.AndroidJUnitRunner \
  | tee qa/captures/gameplay-test.txt
grep -Eq 'OK \(2 tests\)' qa/captures/gameplay-test.txt
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|FATAL EXCEPTION' qa/captures/gameplay-test.txt; then
  echo 'Android gameplay instrumentation failed' >&2
  exit 1
fi
adb shell pidof br.sayva.pocketlauncher >/dev/null || adb shell am start -W -n br.sayva.pocketlauncher/.MainActivity
adb logcat -d -v time -t 1000 > qa/captures/logcat-after-gameplay.txt
if grep -E 'FATAL EXCEPTION|UnsatisfiedLinkError|dlopen failed' qa/captures/logcat-after-gameplay.txt; then
  echo 'Native or Android runtime crash detected in gameplay smoke' >&2
  exit 1
fi
echo 'PASS: actual licensed GBA ROM booted, rendered 240 frames, restored save state, launched Pocket gameplay, and wrote autosave.'
