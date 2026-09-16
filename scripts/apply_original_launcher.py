#!/usr/bin/env python3
"""Original console-first Pocket launcher, not a recolored Lemuroid navigation.

Applied after base, library and multisystem patches. Emulation and saves remain
behind the existing callbacks; the Android launcher chrome is entirely Pocket.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
app = root / 'lemuroid-app'
shell_path = app / 'src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/PocketShell.kt'
text = shell_path.read_text()

def change(before, after, label):
    global text
    if text.count(before) != 1:
        raise SystemExit(f'Pocket launcher UI anchor drift: {label}: {text.count(before)}')
    text = text.replace(before, after, 1)

change('import androidx.compose.ui.graphics.Color',
       'import androidx.compose.ui.graphics.Brush\nimport androidx.compose.ui.graphics.Color',
       'gradient import')
change('SEARCH("Buscar")', 'SEARCH("Explorar")', 'new launcher navigation label')
change('                onSearch = { openTab(PocketTab.SEARCH) },',
       '                onSearch = { openTab(PocketTab.SEARCH) },\n                active = tab,\n                onSelect = { openTab(it) },',
       'header navigation')
change('bottomBar = { PocketBottomBar(tab) { openTab(it) } },',
       'bottomBar = {},',
       'remove legacy bottom bar')
change('Box(Modifier.fillMaxSize().padding(innerPadding)) {',
       'Box(Modifier.fillMaxSize().padding(innerPadding).navigationBarsPadding()) {',
       'system navigation safe-area at launcher boundary')
start = text.index('@Composable\nprivate fun PocketHeader(')
end = text.index('@Composable\nprivate fun PocketHome(', start)
text = text[:start] + '''@Composable
private fun PocketHeader(
    title: String,
    showImport: Boolean,
    onImport: () -> Unit,
    onSearch: () -> Unit,
    active: PocketTab,
    onSelect: (PocketTab) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(
            Brush.verticalGradient(listOf(Color(0xFF061325), Color(0xFF0C1727))),
        ).statusBarsPadding(),
    ) {
        Row(
            Modifier.fillMaxWidth().height(73.dp).padding(start = 24.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("P O C K E T", fontSize = 23.sp, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Black, color = Color.White)
                Text(if (active == PocketTab.HOME) "SEU UNIVERSO DE JOGOS" else title.uppercase(),
                    fontSize = 10.sp, letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            if (showImport) {
                IconButton(onClick = onImport) {
                    Icon(Icons.Outlined.Add, contentDescription = "Adicionar pasta de jogos",
                        tint = Color.White, modifier = Modifier.size(26.dp))
                }
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Outlined.Search, contentDescription = "Pesquisar jogos",
                    tint = Color.White, modifier = Modifier.size(23.dp))
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            PocketTab.entries.forEach { tab ->
                val selected = active == tab
                Column(
                    Modifier.width(if (tab == PocketTab.LIBRARY) 100.dp else 89.dp)
                        .clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))
                        .clickable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(tab.label, modifier = Modifier.padding(top = 13.dp, bottom = 12.dp),
                        fontSize = 13.sp,
                        color = if (selected) Color.White else Color(0xFFA6B5C7),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    Box(Modifier.fillMaxWidth(.67f).height(3.dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = Color(0xFF243447), thickness = 1.dp)
    }
}

''' + text[end:]
start = text.index('@Composable\nprivate fun PocketPlatformShelf(')
text = text[:start] + '''@Composable
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
        Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 19.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Text("Escolha seu universo", color = MaterialTheme.colorScheme.onSurface,
                fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
        }
        if (selected != null) TextButton(onClick = { onSelect(null) }) { Text("Todos os jogos") }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        .padding(start = 22.dp, end = 22.dp, bottom = 17.dp),
        horizontalArrangement = Arrangement.spacedBy(13.dp)) {
        platforms.forEach { (id, name, short) ->
            val active = selected == id
            val accent = when (id) {
                "gba" -> Color(0xFF8C77FF)
                "gb" -> Color(0xFF39D9B4)
                "snes" -> Color(0xFFFFAD75)
                else -> Color(0xFF64B8FF)
            }
            Surface(
                modifier = Modifier.width(223.dp).height(164.dp).clickable { onSelect(id) },
                color = Color(0xFF111F33),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(if (active) 2.dp else 1.dp,
                    if (active) accent else Color(0xFF34465D)),
            ) {
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(
                    listOf(accent.copy(alpha = .24f), Color(0xFF0E1C2F)))),
                ) {
                    Text(short, modifier = Modifier.align(Alignment.TopEnd)
                        .padding(end = 8.dp, top = 5.dp),
                        fontSize = 66.sp, fontWeight = FontWeight.Black,
                        color = accent.copy(alpha = .21f), letterSpacing = (-4).sp)
                    Column(Modifier.align(Alignment.BottomStart).padding(17.dp)) {
                        Box(Modifier.size(31.dp).clip(RoundedCornerShape(9.dp))
                            .background(accent.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.VideogameAsset, contentDescription = null,
                                tint = accent, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.height(13.dp))
                        Text(name, color = Color.White, fontWeight = FontWeight.Bold,
                            fontSize = 18.sp, maxLines = 1)
                        Text("ABRIR COLEÇÃO  ›", fontSize = 10.sp,
                            color = accent, letterSpacing = .8.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
'''
# Console-first identity: show the featured collection before the resume card.
change('''        PocketHero(featured, onPlay, onFavorite)
        Spacer(Modifier.height(20.dp))
        PocketPlatformShelf("Consoles", null, onConsole)
        Spacer(Modifier.height(26.dp))''',
'''        PocketPlatformShelf("Sistemas", null, onConsole)
        Spacer(Modifier.height(14.dp))
        PocketHero(featured, onPlay, onFavorite)
        Spacer(Modifier.height(27.dp))''',
'game platform-first hierarchy')
if 'PocketBottomBar(' in text or 'PocketPlatformShelf(' not in text:
    raise SystemExit('Legacy nav removal / console shelf failed')
shell_path.write_text(text)

# Identity changes outside the Compose surface.
gradle = app / 'build.gradle.kts'
content = gradle.read_text()
old = 'resValue("string", "lemuroid_name", "Pocket GBA")'
if content.count(old) < 1:
    raise SystemExit('Pocket app name injection anchor missing')
gradle.write_text(content.replace(old, 'resValue("string", "lemuroid_name", "Pocket")'))
manifest = app / 'src/main/AndroidManifest.xml'
content = manifest.read_text()
for old, new in [('@mipmap/lemuroid_launcher', '@drawable/pocket_launcher_mark'),
                 ('@mipmap/lemuroid_launcher_round', '@drawable/pocket_launcher_mark')]:
    if old not in content:
        raise SystemExit(f'Pocket launcher icon anchor missing: {old}')
    content = content.replace(old, new)
manifest.write_text(content)
icon = app / 'src/main/res/drawable/pocket_launcher_mark.xml'
icon.write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
  <path android:fillColor="#071728" android:pathData="M0,0 H108 V108 H0 Z" />
  <path android:fillColor="#142B4A" android:pathData="M18,37 C18,25 28,20 39,20 H69 C80,20 90,25 90,37 V69 C90,81 80,87 69,87 H39 C28,87 18,81 18,69 Z" />
  <path android:fillColor="#79C8FF" android:pathData="M31,49 H40 V40 H47 V49 H56 V56 H47 V65 H40 V56 H31 Z" />
  <path android:fillColor="#8C77FF" android:pathData="M70,42 a5,5 0,1 0,10 0 a5,5 0,1 0,-10 0" />
  <path android:fillColor="#39D9B4" android:pathData="M62,54 a5,5 0,1 0,10 0 a5,5 0,1 0,-10 0" />
</vector>
''')
print('Pocket original launcher applied: top console navigation, rich system carousel, safe insets, own icon/name')
