#!/usr/bin/env python3
"""Preserve ALL user-selected ROM folders, SAF grants, existing games and saves.

Applied to our pinned upstream only. Existing single-folder installs migrate on
first additional folder selection without changing database IDs or ROM contents.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
picker = root / 'lemuroid-app/src/main/java/com/swordfish/lemuroid/app/shared/settings/StorageFrameworkPickerLauncher.kt'
provider = root / 'retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/storage/local/StorageAccessFrameworkProvider.kt'
library = root / 'retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library/LemuroidLibrary.kt'

def replace(path, original, new, name):
    content = path.read_text()
    if content.count(original) != 1:
        raise SystemExit(f'Pocket folder patch drift in {name}: expected one anchor, got {content.count(original)}')
    path.write_text(content.replace(original, new, 1))

replace(picker,
'''            if (newValue != null && newValue.toString() != currentValue) {
                updatePersistableUris(newValue)

                sharedPreferences.edit().apply {
                    this.putString(preferenceKey, newValue.toString())
                    this.apply()
                }
            }''',
'''            if (newValue != null) {
                // Migrate existing single-folder users BEFORE changing the legacy key.
                // Adding another folder must never replace the previous one.
                val roots = sharedPreferences.getStringSet(POCKET_ROOTS_KEY, emptySet())!!
                    .toMutableSet()
                currentValue?.let { roots.add(it) }
                roots.add(newValue.toString())
                contentResolver.takePersistableUriPermission(
                    newValue,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                sharedPreferences.edit()
                    .putStringSet(POCKET_ROOTS_KEY, roots.toSet())
                    .putString(preferenceKey, newValue.toString())
                    .apply()
            }''',
'picker append rather than replace')
start = picker.read_text().index('    private fun updatePersistableUris(uri: Uri) {')
end = picker.read_text().index('    private fun startLibraryIndexWork()', start)
content = picker.read_text()
picker.write_text(content[:start] + content[end:])
replace(picker, '        private const val REQUEST_CODE_PICK_FOLDER = 1',
'''        private const val REQUEST_CODE_PICK_FOLDER = 1
        private const val POCKET_ROOTS_KEY = "pocket_library_roots_v1"''',
'picker roots preference')

replace(provider,
'''    override fun listBaseStorageFiles(): Flow<List<BaseStorageFile>> {
        return getExternalFolder()?.let { folder ->
            traverseDirectoryEntries(Uri.parse(folder))
        } ?: emptyFlow()
    }''',
'''    override fun listBaseStorageFiles(): Flow<List<BaseStorageFile>> = flow {
        // One unified library across independent SAF trees. Check ALL permissions before
        // scanning ANY tree to avoid partially indexed collections being purged.
        val roots = getExternalFolders()
        val granted = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission }.map { it.uri.toString() }.toSet()
        roots.forEach { folder ->
            if (folder !in granted) {
                throw SecurityException("Pocket: storage permission missing for previously added folder")
            }
        }
        for (folder in roots) {
            traverseDirectoryEntries(Uri.parse(folder)).collect { emit(it) }
        }
    }''',
'provider union of folder streams')
replace(provider,
'''    private fun getExternalFolder(): String? {
        val prefString = context.getString(R.string.pref_key_extenral_folder)
        val preferenceManager = SharedPreferencesHelper.getLegacySharedPreferences(context)
        return preferenceManager.getString(prefString, null)
    }''',
'''    private fun getExternalFolders(): List<String> {
        val preferences = SharedPreferencesHelper.getLegacySharedPreferences(context)
        val legacy = preferences.getString(context.getString(R.string.pref_key_extenral_folder), null)
        val roots = preferences.getStringSet("pocket_library_roots_v1", emptySet())
            .orEmpty().filter { it.isNotBlank() }.toMutableSet()
        if (!legacy.isNullOrBlank()) roots.add(legacy)
        return roots.sorted()
    }''',
'provider folder collection migration')
replace(provider,
'''                if (result.isFailure) {
                    Timber.e(result.exceptionOrNull(), "Error while listing files")
                }

                val (files, directories) =
                    result.getOrDefault(
                        listOf<BaseStorageFile>() to listOf<String>(),
                    )''',
'''                if (result.isFailure) {
                    // Never pretend an inaccessible directory was successfully scanned:
                    // the indexer would otherwise delete its games as "missing".
                    throw result.exceptionOrNull()!!
                }
                val (files, directories) = result.getOrThrow()''',
'provider fail closed on unreadable folder')
replace(provider,
'''        context.contentResolver.query(childrenUri, projection, null, null, null)?.use {''',
'''        val children = context.contentResolver.query(childrenUri, projection, null, null, null)
            ?: throw SecurityException("Pocket: failed to enumerate previously indexed ROM directory")
        children.use {''',
'provider null query must not erase games')

replace(library,
'''        try {
            indexProviders(startedAtMs)
        } catch (e: Throwable) {
            Timber.e("Library indexing stopped due to exception", e)
        } finally {
            cleanUp(startedAtMs)
        }''',
'''        var scanCompleted = false
        try {
            indexProviders(startedAtMs)
            scanCompleted = true
        } catch (e: Throwable) {
            Timber.e("Pocket: incomplete scan; preserve every existing game and save", e)
        } finally {
            // A missing/revoked folder, failed query or interrupted scan must NEVER
            // erase previously indexed games. Delete only after full successful scan.
            if (scanCompleted) cleanUp(startedAtMs)
        }''',
'indexer cleanup only after successful full scan')
print('Pocket library: multiple folders, SAF permission retention and safe reindexing enabled')
