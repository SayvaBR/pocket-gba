#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa/captures
(adb shell cmd overlay enable --user 0 com.android.internal.systemui.navbar.threebutton || true)
adb shell settings put system screen_off_timeout 600000 || true
adb install -r qa/apk/app-debug.apk
adb shell am start -W -n br.sayva.pocketlauncher/.MainActivity
sleep 7
adb shell pidof br.sayva.pocketlauncher
# The default Android 35 test image may show a Quickstep launcher ANR unrelated
# to Pocket. Preserve every attempt, dismiss this particular system dialog and retry.
for attempt in 1 2 3; do
  adb shell uiautomator dump /sdcard/pocket.xml
  adb shell cat /sdcard/pocket.xml > "qa/captures/home-attempt-${attempt}.xml"
  adb exec-out screencap -p > "qa/captures/home-attempt-${attempt}.png"
  if grep -Fq 'Quickstep isn' "qa/captures/home-attempt-${attempt}.xml"; then
    echo "Android emulator home (Quickstep) ANR, dismissing system dialog: attempt $attempt"
    adb shell input tap 540 1200
    sleep 3
    adb shell am start -W -n br.sayva.pocketlauncher/.MainActivity
    sleep 5
    continue
  fi
  cp "qa/captures/home-attempt-${attempt}.xml" qa/captures/01-home.xml
  cp "qa/captures/home-attempt-${attempt}.png" qa/captures/01-home.png
  break
done
if ! test -s qa/captures/01-home.xml; then
  echo "No unobstructed app UI available in Android virtual device" >&2
  exit 1
fi
grep -Fq 'P O C K E T' qa/captures/01-home.xml
grep -Fq 'Consoles' qa/captures/01-home.xml
if ! grep -Eq 'Sua coleção|Nenhum jogo importado' qa/captures/01-home.xml; then
  echo "Pocket home screen not visible; evidence preserved" >&2
  exit 1
fi
if grep -Eiq "isn.t responding|Close app|not responding" qa/captures/01-home.xml; then
  echo "An error dialog obscures Pocket" >&2
  exit 1
fi
adb shell input tap 410 310
sleep 3
adb shell uiautomator dump /sdcard/pocket.xml
adb shell cat /sdcard/pocket.xml > qa/captures/02-navigation.xml
adb exec-out screencap -p > qa/captures/02-navigation.png
adb shell pidof br.sayva.pocketlauncher
test -s qa/captures/01-home.png
test -s qa/captures/02-navigation.png
echo 'Pocket independent app launch, Home and visual screenshots verified.'
