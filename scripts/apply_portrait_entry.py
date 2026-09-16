#!/usr/bin/env python3
"""Make the new Pocket portrait UI the ONLY mounted Android main screen."""
from pathlib import Path
import sys
root = Path(sys.argv[1])
activity = root / 'lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/MainActivity.kt'
text = activity.read_text()
old = '            PocketShell('
if text.count(old) != 1:
    raise SystemExit(f'Portrait launcher entrypoint drift: {text.count(old)}')
activity.write_text(text.replace(old, '            PocketPortraitLauncher(', 1))
manifest = root / 'lemuroid-app/src/main/AndroidManifest.xml'
xml = manifest.read_text()
old = '''android:name="com.swordfish.lemuroid.app.mobile.feature.main.MainActivity"
            android:exported="true"'''
if xml.count(old) != 1:
    raise SystemExit('Portrait MainActivity manifest drift')
manifest.write_text(xml.replace(old, '''android:name="com.swordfish.lemuroid.app.mobile.feature.main.MainActivity"
            android:screenOrientation="portrait"
            android:exported="true"''', 1))
print('Pocket portrait launcher mounted; MainActivity locked to portrait, game Activity untouched')
