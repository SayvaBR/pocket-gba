#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OVERRIDES="$REPO_ROOT/overrides"
APP_GRADLE="$ROOT/lemuroid-app/build.gradle.kts"
GAME_SYSTEM="$ROOT/retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/GameSystem.kt"
SETTINGS_SCREEN="$ROOT/lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/settings/general/SettingsScreen.kt"
MOBILE_GAME="$ROOT/lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/game/MobileGameScreen.kt"
CORES_DIR="$ROOT/lemuroid-cores/bundled-cores/src/main/jniLibs"

# Copy Pocket GBA-owned UI files over the pinned upstream source.
if [[ -d "$OVERRIDES" ]]; then
  cp -R "$OVERRIDES"/. "$ROOT"/
fi

python3 - "$APP_GRADLE" "$GAME_SYSTEM" "$SETTINGS_SCREEN" "$MOBILE_GAME" <<'PY'
from pathlib import Path
import sys

app_gradle = Path(sys.argv[1])
game_system = Path(sys.argv[2])
settings_screen = Path(sys.argv[3])
mobile_game = Path(sys.argv[4])

# Pocket GBA application identity.
text = app_gradle.read_text()
text = text.replace('applicationId = "com.swordfish.lemuroid"', 'applicationId = "com.sayvabr.pocketgba"')
text = text.replace('resValue("string", "lemuroid_name", "Lemuroid")', 'resValue("string", "lemuroid_name", "Pocket GBA")')
text = text.replace('resValue("string", "lemuroid_name", "LemuroiDebug")', 'resValue("string", "lemuroid_name", "Pocket GBA")')
app_gradle.write_text(text)

# Restrict the library to Game Boy Advance.
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

# Add Pocket GBA appearance controls at the top of Settings.
text = settings_screen.read_text()
settings_anchor = '    LemuroidSettingsPage(modifier = modifier) {\n        RomsSettings('
settings_replacement = '    LemuroidSettingsPage(modifier = modifier) {\n        PocketAppearanceSettings()\n        RomsSettings('
if settings_anchor not in text:
    raise SystemExit('Could not locate Settings page anchor; upstream source changed.')
text = text.replace(settings_anchor, settings_replacement, 1)
settings_screen.write_text(text)

# Connect persisted appearance preferences to the gameplay UI.
text = mobile_game.read_text()
text = text.replace(
    'import androidx.compose.foundation.layout.Arrangement',
    'import androidx.compose.foundation.border\nimport androidx.compose.foundation.layout.Arrangement',
    1,
)
text = text.replace(
    'import androidx.compose.material3.Card',
    'import androidx.compose.foundation.shape.RoundedCornerShape\nimport androidx.compose.material3.Card',
    1,
)
text = text.replace(
    'import androidx.compose.ui.graphics.vector.ImageVector',
    'import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics.vector.ImageVector',
    1,
)
text = text.replace(
    'import com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel',
    'import com.swordfish.lemuroid.app.mobile.shared.compose.ui.pocketAccentColor\nimport com.swordfish.lemuroid.app.shared.game.BaseGameScreenViewModel\nimport com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState',
    1,
)

orientation_anchor = '''        val isLandscape = constraints.maxWidth > constraints.maxHeight\n\n        LaunchedEffect(isLandscape) {'''
orientation_replacement = '''        val isLandscape = constraints.maxWidth > constraints.maxHeight\n\n        val accentIndex =\n            indexPreferenceState(\n                key = "pocket_theme_accent",\n                default = "blue",\n                values = listOf("blue", "purple", "cyan", "green", "orange", "pink"),\n            ).value\n        val controllerSkinIndex =\n            indexPreferenceState(\n                key = "pocket_controller_skin",\n                default = "minimal",\n                values = listOf("minimal", "classic", "glow"),\n            ).value\n        val bezelIndex =\n            indexPreferenceState(\n                key = "pocket_bezel_style",\n                default = "minimal",\n                values = listOf("none", "minimal", "classic", "accent"),\n            ).value\n        val accentColor = pocketAccentColor(accentIndex)\n        val controllerStyle = listOf("minimal", "classic", "glow").getOrElse(controllerSkinIndex) { "minimal" }\n\n        LaunchedEffect(isLandscape) {'''
if orientation_anchor not in text:
    raise SystemExit('Could not locate gameplay orientation anchor; upstream source changed.')
text = text.replace(orientation_anchor, orientation_replacement, 1)

provider_anchor = 'CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme()) {'
provider_replacement = 'CompositionLocalProvider(LocalLemuroidPadTheme provides LemuroidPadTheme(style = controllerStyle, accent = accentColor)) {'
if provider_anchor not in text:
    raise SystemExit('Could not locate controller theme provider; upstream source changed.')
text = text.replace(provider_anchor, provider_replacement, 1)

constraint_anchor = '''            ConstraintLayout(\n                modifier = Modifier.fillMaxSize(),'''
constraint_replacement = '''            val bezelModifier =\n                when (bezelIndex) {\n                    1 -> Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))\n                    2 -> Modifier.border(8.dp, Color(0xFF1B2B3B), RoundedCornerShape(14.dp))\n                    3 -> Modifier.border(3.dp, accentColor.copy(alpha = 0.82f), RoundedCornerShape(14.dp))\n                    else -> Modifier\n                }\n\n            ConstraintLayout(\n                modifier = Modifier.fillMaxSize(),'''
if constraint_anchor not in text:
    raise SystemExit('Could not locate gameplay constraint anchor; upstream source changed.')
text = text.replace(constraint_anchor, constraint_replacement, 1)

viewport_anchor = '''                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)\n                            .windowInsetsPadding'''
viewport_replacement = '''                            .layoutId(GameScreenLayout.CONSTRAINTS_GAME_VIEW)\n                            .then(bezelModifier)\n                            .windowInsetsPadding'''
if viewport_anchor not in text:
    raise SystemExit('Could not locate gameplay viewport anchor; upstream source changed.')
text = text.replace(viewport_anchor, viewport_replacement, 1)
mobile_game.write_text(text)
PY

# The bundle flavor uses symlinks/native libraries from LemuroidCores. Keep mGBA only.
if [[ -d "$CORES_DIR" ]]; then
  find "$CORES_DIR" -type l ! -name 'libmgba_libretro_android.so' -delete
  find "$CORES_DIR" -type f ! -name 'libmgba_libretro_android.so' -delete
fi

echo "Pocket GBA upstream patch applied."
