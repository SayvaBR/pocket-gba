#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa/captures
TEST_APK=$(find qa/test -name '*.apk' -print -quit)
test -n "$TEST_APK"
test -s "$TEST_APK"
adb install -r "$TEST_APK"
adb shell am force-stop br.sayva.pocketlauncher || true
# Real MIT ROM is confined to the instrumented APK, never the delivered user APK.
# The tests load and advance the core, open actual Pocket gameplay, persist autosave,
# then navigate the portrait details screen and verify the favorite survives reload.
adb shell am instrument -w -r br.sayva.pocketlauncher.test/androidx.test.runner.AndroidJUnitRunner \
  | tee qa/captures/gameplay-test.txt
grep -Eq 'OK \(3 tests\)' qa/captures/gameplay-test.txt
if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|FATAL EXCEPTION' qa/captures/gameplay-test.txt; then
  echo 'Android gameplay or details instrumentation failed' >&2
  exit 1
fi
adb shell pidof br.sayva.pocketlauncher >/dev/null || adb shell am start -W -n br.sayva.pocketlauncher/.MainActivity
adb logcat -d -v time -t 1000 > qa/captures/logcat-after-gameplay.txt
if grep -E 'FATAL EXCEPTION|UnsatisfiedLinkError|dlopen failed' qa/captures/logcat-after-gameplay.txt; then
  echo 'Native or Android runtime crash detected in gameplay smoke' >&2
  exit 1
fi
echo 'PASS: real licensed GBA ROM boot, output and saves, plus original portrait detail UI with persistent favorite.'
