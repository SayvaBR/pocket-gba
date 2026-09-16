#!/usr/bin/env python3
"""Non-destructive Pocket library scan. Metadata lookup must not be required for a ROM to exist.
Only explicit user action may remove entries; never delete games merely because SAF/media
was temporarily unavailable. Existing ROM paths, game IDs and save mappings are retained.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
file = root / 'retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/LemuroidLibrary.kt'
text = file.read_text()

def one(old, new, title):
    global text
    if text.count(old) != 1:
        raise SystemExit(f'Pocket scanner drift ({title}): expected 1, got {text.count(old)}')
    text = text.replace(old, new, 1)

one('''        return buildScanEntry(groupedStorageFile, game)
    }

    private fun safeStorageFile(''', '''        return buildScanEntry(groupedStorageFile, game ?: pocketRecognizeByExtension(groupedStorageFile, startedAtMs))
    }

    /** Recognize a valid, unambiguous ROM extension when the online/hash catalogue has no entry.
     * Do not guess ZIP contents or firmware: incorrectly identifying those could launch a wrong core.
     * Retain the real SAF URI as the game identity so the emulator and saves work normally.
     */
    private fun pocketRecognizeByExtension(files: GroupedStorageFiles, scannedAt: Long): Game? {
        val file = files.primaryFile
        val extension = file.name.substringAfterLast('.', "").lowercase(java.util.Locale.ROOT)
        val system = when (extension) {
            "gba" -> "gba"
            "gb" -> "gb"
            "gbc" -> "gbc"
            "smc", "sfc" -> "snes"
            "nds" -> "nds"
            else -> return null
        }
        val cleaned = file.name.substringBeforeLast('.', file.name)
            .replace(Regex("\\\\s*\\\\([^)]*\\\\)|\\\\s*\\\\[[^]]*]"), " ")
            .replace(Regex("\\\\s+"), " ").trim()
        return Game(
            fileName = file.name,
            fileUri = file.uri.toString(),
            title = cleaned.ifBlank { file.name },
            systemId = system,
            developer = null,
            coverFrontUrl = null,
            lastIndexedAt = scannedAt,
        )
    }

    private fun safeStorageFile(''', 'safe fallback for uncatalogued ROM')
# Existing library should never disappear because an Android folder is empty, disconnected,
# temporarily revoked or the user changes its configuration. User hides entries explicitly
# via PocketGamePersonalization; a future source-scoped removal feature can safely delete rows.
one('''            if (scanCompleted) cleanUp(startedAtMs)''', '''            // Keep all existing game IDs and their saves. An explicit remove operation,
            // not a background rescan, is the only safe way to delete library entries.
            // The old destructive cleanUp() is deliberately never invoked in Pocket.
            if (!scanCompleted) Timber.w("Pocket scan incomplete; library preserved")''', 'disable global database deletion')
file.write_text(text)
print('Pocket scanner: uncatalogued .gba/.gb/.gbc/.smc/.sfc/.nds included; automatic deletion disabled')
