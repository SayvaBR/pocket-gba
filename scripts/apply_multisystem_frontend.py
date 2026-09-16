#!/usr/bin/env python3
"""Opt-in multisystem preview, applied AFTER existing Pocket customization.

Retains the existing application id, database and saves. Never touches user ROMs.
The Android app still uses the pinned Lemuroid-derived backend while the frontend
is extracted into independent modules; this is NOT a stand-alone launcher yet.
"""
from pathlib import Path
import os
import sys

root = Path(sys.argv[1]).resolve()
app = root / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile"
shared = root / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/library"
game_system = shared / "GameSystem.kt"
view_model = app / "feature/games/GamesViewModel.kt"
shell_file = app / "feature/main/PocketShell.kt"


def replace_exact(text: str, original: str, replacement: str, name: str) -> str:
    count = text.count(original)
    if count != 1:
        raise SystemExit(f"Multisystem patch: {name}: expected 1 anchor; got {count}")
    return text.replace(original, replacement, 1)


system_text = game_system.read_text()
system_text = replace_exact(
    system_text,
    "SYSTEMS.filter { it.id == SystemID.GBA }",
    "SYSTEMS.filter { it.id in setOf(SystemID.GBA, SystemID.GB, SystemID.GBC, SystemID.SNES, SystemID.NDS) }",
    "indexed console family",
)
game_system.write_text(system_text)

model_text = view_model.read_text()
model_text = replace_exact(
    model_text,
    "import com.swordfish.lemuroid.lib.library.MetaSystemID",
    "import com.swordfish.lemuroid.lib.library.MetaSystemID\nimport com.swordfish.lemuroid.lib.library.SystemID",
    "system import",
)
model_text = replace_exact(
    model_text,
    "    private val metaSystemId = MutableStateFlow(initialMetaSystem)",
    """    // One catalogue for all four initial console families. No separate ROM database.
    private val selectedSystemId = MutableStateFlow<String?>(null)

    fun selectConsole(id: String?) {
        require(id == null || id in listOf("gba", "gb", "gbc", "snes", "nds"))
        selectedSystemId.value = id
    }""",
    "catalogue selection",
)
model_text = replace_exact(
    model_text,
    """        metaSystemId
            .map { metaSystem -> metaSystem.systemIDs }""",
    """        selectedSystemId
            .map { selected ->
                listOf(SystemID.GBA, SystemID.GB, SystemID.GBC, SystemID.SNES, SystemID.NDS)
                    .filter { selected == null || it.dbname == selected }
            }""",
    "platform-aware paging",
)
view_model.write_text(model_text)

shell = shell_file.read_text()
shell = replace_exact(shell, 'title = if (tab == PocketTab.HOME) "Pocket GBA" else tab.label',
                      'title = if (tab == PocketTab.HOME) "Pocket" else tab.label', 'frontend name')
shell = replace_exact(shell,
                      '    var query by rememberSaveable { mutableStateOf("") }',
                      '    var consoleFilter by rememberSaveable { mutableStateOf<String?>(null) }\n    var query by rememberSaveable { mutableStateOf("") }',
                      'filter state')
shell = replace_exact(shell,
                      '    LaunchedEffect(query) { searchViewModel.queryString.value = query }',
                      '''    LaunchedEffect(query) { searchViewModel.queryString.value = query }
    // Home and search always display the full catalogue, never the last library filter.
    LaunchedEffect(tab) {
        if (tab != PocketTab.LIBRARY) {
            consoleFilter = null
            gamesViewModel.selectConsole(null)
        }
    }''',
                      'reset library filter')
shell = replace_exact(shell,
                      'PocketTab.LIBRARY -> PocketLibrary(games, onPlay, onFavorite, onImport)',
                      '''PocketTab.LIBRARY -> PocketLibrary(
                    games, onPlay, onFavorite, onImport, consoleFilter,
                    onFilter = { selected ->
                        consoleFilter = selected
                        gamesViewModel.selectConsole(selected)
                    },
                )''',
                      'filter callback')
shell = replace_exact(shell,
                      'private fun PocketLibrary(games: LazyPagingItems<Game>, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit, onImport: () -> Unit) {',
                      '''private fun PocketLibrary(
    games: LazyPagingItems<Game>, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit,
    onImport: () -> Unit, selectedConsole: String?, onFilter: (String?) -> Unit,
) {''',
                      'library parameters')
shell = replace_exact(shell,
                      '        LazyVerticalGrid(\n            columns = GridCells.Fixed(2),',
                      '''        // Single-row, controller-friendly console filter; all options are visible
        // without a settings screen. Filtering happens in SQL paging, not by hiding cells.
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            listOf(
                null to "Todos", "gba" to "GBA", "snes" to "SNES",
                "nds" to "DS", "gb" to "GB", "gbc" to "GBC",
            ).forEach { (id, label) ->
                val active = selectedConsole == id
                TextButton(
                    onClick = { onFilter(id) },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) { Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),''',
                      'console filter UI')
shell = replace_exact(shell,
                      'Text("GAME BOY ADVANCE", fontSize = 10.sp,',
                      'Text(pocketConsoleName(game.systemId).uppercase(), fontSize = 10.sp,',
                      'featured console caption')
shell = replace_exact(shell,
                      'Text("Sistema: Game Boy Advance")',
                      'Text("Sistema: ${pocketConsoleName(game.systemId)}")',
                      'game information console')
shell = replace_exact(shell,
                      '"Adicione uma pasta com seus arquivos .gba para começar."',
                      '"Adicione uma pasta com jogos de GBA, SNES, DS ou Game Boy."',
                      'empty home message')
shell = replace_exact(shell,
                      '"Selecione a pasta onde estão seus arquivos .gba."',
                      '"Selecione uma pasta contendo seus jogos. As capas serão buscadas automaticamente."',
                      'empty library message')
# Retire the image-picker UI. Existing saved override data is NOT deleted, so old
# installations remain migratable; automatic artwork takes precedence from now on.
start = shell.index('    val coverPicker = rememberLauncherForActivityResult(')
end = shell.index('    if (open) {', start)
shell = shell[:start] + shell[end:]
start = shell.index('                    TextButton(onClick = { onDismiss(); coverPicker.launch(')
end = shell.index('                    TextButton(onClick = { onDismiss(); showingInfo = true })', start)
shell = shell[:start] + shell[end:]
shell = replace_exact(shell, '    val selectedCover = overrides.customCover(game)\n', '', 'remove manual cover priority')
shell = replace_exact(shell, '        val image = selectedCover ?: catalogCover',
                      '        val image = catalogCover ?: pocketAutomaticArtwork(game)', 'automatic artwork source')
# Fallback thumbnail guess for known No-Intro titles when DB has no media entry.
# A missing URL is harmless: Coil leaves our branded, offline fallback visible.
shell += '''

private fun pocketConsoleName(systemId: String): String = when (systemId) {
    "gba" -> "Game Boy Advance"
    "gb" -> "Game Boy"
    "gbc" -> "Game Boy Color"
    "snes" -> "Super Nintendo"
    "nds" -> "Nintendo DS"
    else -> "Jogo"
}

private fun pocketAutomaticArtwork(game: Game): String? {
    val folder = when (game.systemId) {
        "gba" -> "Nintendo - Game Boy Advance"
        "gb" -> "Nintendo - Game Boy"
        "gbc" -> "Nintendo - Game Boy Color"
        "snes" -> "Nintendo - Super Nintendo Entertainment System"
        "nds" -> "Nintendo - Nintendo DS"
        else -> return null
    }
    val name = game.fileName.substringBeforeLast('.', game.fileName)
        .replace(Regex("\\\\s*\\\\([^)]*\\\\)|\\\\s*\\\\[[^]]*]"), "")
        .trim()
        .replace(Regex("[&*/:<>?\\\\\\\"|]"), "_")
    if (name.isEmpty()) return null
    val path = java.net.URLEncoder.encode("$name (USA).png", "UTF-8").replace("+", "%20")
    val systemPath = java.net.URLEncoder.encode(folder, "UTF-8").replace("+", "%20")
    return "https://thumbnails.libretro.com/$systemPath/Named_Boxarts/$path"
}
'''
if 'coverPicker.launch(' in shell or 'selectedCover ?:' in shell:
    raise SystemExit('Manual artwork selector was not completely removed')
shell_file.write_text(shell)

# prepare_upstream.sh previously stripped every core except mGBA. Restore only
# these 3 explicit, pinned native cores from the same sparse checkout.
cores_root = root / 'lemuroid-cores'
bundled = cores_root / 'bundled-cores/src/main/jniLibs'
for abi_dir in bundled.iterdir():
    if not abi_dir.is_dir():
        continue
    for core in ('snes9x', 'gambatte', 'melonds'):
        source = cores_root / f'lemuroid_core_{core}/src/main/jniLibs/{abi_dir.name}/lib{core}_libretro_android.so'
        if not source.is_file():
            raise SystemExit(f'Required native core missing for {abi_dir.name}: {source}')
        dest = abi_dir / source.name
        if dest.exists() or dest.is_symlink():
            dest.unlink()
        dest.symlink_to(os.path.relpath(source, abi_dir))
print('Pocket frontend: GBA, SNES, NDS, GB/GBC, platform filters, automatic cover priority and native cores enabled')
