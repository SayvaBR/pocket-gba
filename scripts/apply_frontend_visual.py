#!/usr/bin/env python3
"""Visible, original multi-console UI layer applied after multisystem patch.

Retains emulation and library callbacks. A real console shelf must be visible
BEFORE importing any ROM, unlike the old mostly-blank emulation frontend.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
file = root / 'lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/PocketShell.kt'
text = file.read_text()


def change(before: str, after: str, label: str) -> None:
    global text
    count = text.count(before)
    if count != 1:
        raise SystemExit(f'Pocket console UI: {label} anchor count {count}')
    text = text.replace(before, after, 1)


change('onLibrary = { openTab(PocketTab.LIBRARY) })',
       'onLibrary = { openTab(PocketTab.LIBRARY) },\n                    onConsole = { id -> consoleFilter = id; gamesViewModel.selectConsole(id); openTab(PocketTab.LIBRARY) })',
       'home console route')
change('    onLibrary: () -> Unit,\n) {\n    // Direct paging source:',
       '    onLibrary: () -> Unit,\n    onConsole: (String?) -> Unit,\n) {\n    // Direct paging source:',
       'home parameter')
change('    if (loaded.isEmpty()) {\n        PocketEmpty(',
       '''    if (loaded.isEmpty()) {
        Column(Modifier.fillMaxSize()) {
            PocketPlatformShelf("Consoles", null, onConsole)
            Box(Modifier.weight(1f)) {
                PocketEmpty(''',
       'always-visible home console shelf')
change('            onAction = onImport,\n        )\n        return\n    }',
       '            onAction = onImport,\n                )\n            }\n        }\n        return\n    }',
       'close home shelf')
change('        PocketHero(featured, onPlay, onFavorite)\n        Spacer(Modifier.height(26.dp))',
       '        PocketHero(featured, onPlay, onFavorite)\n        Spacer(Modifier.height(20.dp))\n        PocketPlatformShelf("Consoles", null, onConsole)\n        Spacer(Modifier.height(26.dp))',
       'console shelf following resume hero')
change('''        PocketEmpty("Nenhum jogo ainda", "Selecione uma pasta contendo seus jogos. As capas serão buscadas automaticamente.", "Adicionar jogos", onImport)
        return''',
       '''        Column(Modifier.fillMaxSize()) {
            PocketPlatformShelf("Consoles", selectedConsole, onFilter)
            Box(Modifier.weight(1f)) {
                PocketEmpty("Nenhum jogo ainda", "Selecione uma pasta contendo seus jogos. As capas serão buscadas automaticamente.", "Adicionar jogos", onImport)
            }
        }
        return''',
       'empty library console shelf')
change('fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)\n    }\n    PocketGameActionDialogs',
       '''fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(3.dp))
        Text(pocketConsoleName(game.systemId), fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
    }
    PocketGameActionDialogs''',
       'card console metadata')
change('Text("Pocket GBA • Core mGBA",',
       'Text("Pocket • GBA · Game Boy · SNES · Nintendo DS",',
       'settings accuracy')

# Four completely original UI tiles, no borrowed console logos or marketing.
text += '''

@Composable
private fun PocketPlatformShelf(
    title: String,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    val platforms = listOf(
        Triple("gba", "Game Boy Advance", "GBA"),
        Triple("gb", "Game Boy", "GB"),
        Triple("snes", "Super Nintendo", "SNES"),
        Triple("nds", "Nintendo DS", "DS"),
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 20.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        if (selected != null) TextButton(onClick = { onSelect(null) }) { Text("Todos", fontSize = 12.sp) }
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(start = 22.dp, end = 22.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        platforms.forEach { (id, name, short) ->
            val active = id == selected
            Surface(
                modifier = Modifier.width(162.dp).height(116.dp).clickable { onSelect(id) },
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(21.dp),
                border = BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    Modifier.fillMaxSize().padding(start = 16.dp, top = 13.dp, end = 12.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VideogameAsset, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.weight(1f))
                        Text(short, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Text(name, fontSize = 16.sp, lineHeight = 19.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 2,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
'''
if text.count('PocketPlatformShelf(') != 4:
    raise SystemExit('Pocket platform shelf not attached to both empty and populated states')
file.write_text(text)
print('Pocket console-first UI applied: original platform tiles, visible empty states and accurate system cards')
