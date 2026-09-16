#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
APP_GRADLE="$ROOT/lemuroid-app/build.gradle.kts"
GAME_SYSTEM="$ROOT/retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/GameSystem.kt"
CORES_DIR="$ROOT/lemuroid-cores/bundled-cores/src/main/jniLibs"

python3 - "$APP_GRADLE" "$GAME_SYSTEM" <<'PY'
from pathlib import Path
import sys

app_gradle = Path(sys.argv[1])
game_system = Path(sys.argv[2])

text = app_gradle.read_text()
text = text.replace('applicationId = "com.swordfish.lemuroid"', 'applicationId = "com.sayvabr.pocketgba"')
text = text.replace('resValue("string", "lemuroid_name", "Lemuroid")', 'resValue("string", "lemuroid_name", "Pocket GBA")')
text = text.replace('resValue("string", "lemuroid_name", "LemuroiDebug")', 'resValue("string", "lemuroid_name", "Pocket GBA")')
app_gradle.write_text(text)

text = game_system.read_text()
anchor = '        private val byIdCache by lazy { mapOf(*SYSTEMS.map { it.id.dbname to it }.toTypedArray()) }'
replacement = '''        // Pocket GBA is deliberately focused on Game Boy Advance only.\n        private val SUPPORTED_SYSTEMS by lazy { SYSTEMS.filter { it.id == SystemID.GBA } }\n\n        private val byIdCache by lazy { mapOf(*SUPPORTED_SYSTEMS.map { it.id.dbname to it }.toTypedArray()) }'''
if anchor not in text:
    raise SystemExit('Could not locate GameSystem cache anchor; upstream source changed.')
text = text.replace(anchor, replacement)
text = text.replace('            for (system in SYSTEMS) {', '            for (system in SUPPORTED_SYSTEMS) {')
text = text.replace('        fun all() = SYSTEMS', '        fun all() = SUPPORTED_SYSTEMS')
text = text.replace('            return SYSTEMS.flatMap { it.supportedExtensions }', '            return SUPPORTED_SYSTEMS.flatMap { it.supportedExtensions }')
game_system.write_text(text)
PY

# The bundle flavor uses symlinks/native libraries from LemuroidCores. Keep mGBA only.
if [[ -d "$CORES_DIR" ]]; then
  find "$CORES_DIR" -type l ! -name 'libmgba_libretro_android.so' -delete
  find "$CORES_DIR" -type f ! -name 'libmgba_libretro_android.so' -delete
fi

echo "Pocket GBA upstream patch applied."
