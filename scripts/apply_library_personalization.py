#!/usr/bin/env python3
"""Own game presentation and navigation on top of the pinned, tested mGBA backend.

Applied after prepare_upstream.sh and apply_shell_v2.py. Hard-fail if the UI anchors drift.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
shell = root / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/PocketShell.kt"
provider = root / "lemuroid-metadata-libretro-db/src/main/java/com/swordfish/lemuroid/metadata/libretrodb/LibretroDBMetadataProvider.kt"
text = shell.read_text()

def replace_once(before, after, label):
    global text
    if text.count(before) != 1:
        raise SystemExit(f"Pocket personalization patch failed: {label}: anchor count {text.count(before)}")
    text = text.replace(before, after, 1)

replace_once("package com.swordfish.lemuroid.app.mobile.feature.main\n", """package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalContext
""", "UI imports")
replace_once("    var tab by rememberSaveable { mutableStateOf(PocketTab.HOME) }\n    var query by rememberSaveable { mutableStateOf(\"\") }", """    val context = LocalContext.current
    val navigationPreferences = remember(context) {
        context.applicationContext.getSharedPreferences("pocket_navigation_v1", Context.MODE_PRIVATE)
    }
    // A persistent tab history makes Android Back return to the previous tab; the app
    // can also restore the last visible page after ordinary process recreation.
    var navigationPath by rememberSaveable {
        mutableStateOf(navigationPreferences.getString("tab_path", "HOME")
            ?.takeIf { path -> path.split(",").all { item -> PocketTab.entries.any { it.name == item } } }
            ?: "HOME")
    }
    val tab = PocketTab.valueOf(navigationPath.substringAfterLast(','))
    fun openTab(next: PocketTab) {
        if (next != tab) {
            navigationPath = (navigationPath.split(',').takeLast(7) + next.name).joinToString(",")
        }
    }
    BackHandler(enabled = navigationPath.contains(',')) {
        navigationPath = navigationPath.substringBeforeLast(',')
    }
    LaunchedEffect(navigationPath) {
        navigationPreferences.edit().putString("tab_path", navigationPath).apply()
    }
    var query by rememberSaveable { mutableStateOf("") }""", "persistent tab history")
replace_once("onSearch = { tab = PocketTab.SEARCH },", "onSearch = { openTab(PocketTab.SEARCH) },", "header search route")
replace_once("bottomBar = { PocketBottomBar(tab) { tab = it } },", "bottomBar = { PocketBottomBar(tab) { openTab(it) } },", "bottom routes")
replace_once("onLibrary = { tab = PocketTab.LIBRARY }", "onLibrary = { openTab(PocketTab.LIBRARY) }", "library route")
replace_once("    val loaded = games.itemSnapshotList.items\n", """    val overrides = PocketGamePersonalization.get(LocalContext.current)
    val loaded = games.itemSnapshotList.items.filterNot { overrides.hidden(it) }
""", "home excludes hidden titles")
replace_once("private fun PocketHero(game: Game, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit) {\n    val shape", """private fun PocketHero(game: Game, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit) {
    val overrides = PocketGamePersonalization.get(LocalContext.current)
    val shape""", "hero custom title")
replace_once("Text(game.title, fontSize = 19.sp,", "Text(overrides.title(game), fontSize = 19.sp,", "hero title display")
replace_once("    Column(Modifier.fillMaxSize()) {\n        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)", """    val overrides = PocketGamePersonalization.get(LocalContext.current)
    var showHidden by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp)""", "library hidden filter state")
replace_once('            Text("${games.itemCount} jogos", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)\n            TextButton(onClick = onImport)', '''            Text(if (showHidden) "Jogos ocultos" else "${games.itemCount} jogos", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            TextButton(onClick = { showHidden = !showHidden }) {
                Text(if (showHidden) "Ver todos" else "Ocultos", fontSize = 12.sp)
            }
            TextButton(onClick = onImport)''', "library hidden access")
replace_once("if (game != null) PocketGameCard(game, onPlay, onFavorite, Modifier.fillMaxWidth())", "if (game != null && overrides.hidden(game) == showHidden) PocketGameCard(game, onPlay, onFavorite, Modifier.fillMaxWidth())", "library hidden cards")

start = text.index("@Composable\nprivate fun PocketGameCard(")
end = text.index("@Composable\nprivate fun PocketCover(", start)
card = '''@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PocketGameCard(game: Game, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit, modifier: Modifier = Modifier) {
    val overrides = PocketGamePersonalization.get(LocalContext.current)
    var showActions by remember { mutableStateOf(false) }
    Column(modifier.clip(RoundedCornerShape(17.dp))
        .combinedClickable(onClick = { onPlay(game) }, onLongClick = { showActions = true })
        .animateContentSize()) {
        Box(Modifier.fillMaxWidth().height(178.dp).clip(RoundedCornerShape(16.dp))) {
            PocketCover(game, Modifier.fillMaxSize())
            IconButton(
                onClick = { onFavorite(game) },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(38.dp)
                    .background(Color(0xD9091421), RoundedCornerShape(22.dp)),
            ) {
                Icon(if (game.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (game.isFavorite) "Remover favorito" else "Favoritar",
                    tint = if (game.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(overrides.title(game), maxLines = 2, minLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    }
    PocketGameActionDialogs(game, overrides, showActions, onDismiss = { showActions = false })
}

@Composable
private fun PocketGameActionDialogs(
    game: Game,
    overrides: PocketGamePersonalization,
    open: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var renaming by remember { mutableStateOf(false) }
    var showingInfo by remember { mutableStateOf(false) }
    var draftTitle by remember(game.fileUri) { mutableStateOf(overrides.title(game)) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                overrides.setCustomCover(game, uri.toString())
            } catch (_: SecurityException) {
                // Do not save a transient URI that would break on next app launch.
            }
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(overrides.title(game), maxLines = 2) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { onDismiss(); draftTitle = overrides.title(game); renaming = true }) {
                        Text("✎  Renomear")
                    }
                    TextButton(onClick = { onDismiss(); coverPicker.launch(arrayOf("image/*")) }) {
                        Text("▧  Escolher capa")
                    }
                    if (overrides.customCover(game) != null) {
                        TextButton(onClick = { overrides.setCustomCover(game, null); onDismiss() }) {
                            Text("Restaurar capa automática")
                        }
                    }
                    TextButton(onClick = { onDismiss(); showingInfo = true }) {
                        Text("ⓘ  Informações do jogo")
                    }
                    TextButton(onClick = { overrides.setHidden(game, !overrides.hidden(game)); onDismiss() }) {
                        Text(if (overrides.hidden(game)) "Restaurar à biblioteca" else "Remover da lista")
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } },
            shape = RoundedCornerShape(22.dp),
        )
    }
    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Renomear jogo") },
            text = { OutlinedTextField(value = draftTitle, onValueChange = { draftTitle = it.take(120) },
                label = { Text("Nome na biblioteca") }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = { overrides.rename(game, draftTitle); renaming = false },
                    enabled = draftTitle.isNotBlank()) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancelar") } },
            shape = RoundedCornerShape(22.dp),
        )
    }
    if (showingInfo) {
        AlertDialog(
            onDismissRequest = { showingInfo = false },
            title = { Text("Informações do jogo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(overrides.title(game), fontWeight = FontWeight.SemiBold)
                    Text("Sistema: Game Boy Advance")
                    game.developer?.let { Text("Desenvolvedora: $it") }
                    Text("Arquivo: ${game.fileName}")
                    Text(if (game.lastPlayedAt == null) "Ainda não jogado" else "Jogado anteriormente")
                    Text("A remoção da lista não apaga a ROM nem seus saves.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showingInfo = false }) { Text("Fechar") } },
            shape = RoundedCornerShape(22.dp),
        )
    }
}

'''
text = text[:start] + card + text[end:]
start = text.index("@Composable\nprivate fun PocketCover(")
end = text.index("@Composable\nprivate fun PocketSearch(", start)
cover = '''@Composable
private fun PocketCover(game: Game, modifier: Modifier) {
    val overrides = PocketGamePersonalization.get(LocalContext.current)
    val selectedCover = overrides.customCover(game)
    // Upstream metadata uses HTTP; Android may reject cleartext requests. Prefer HTTPS.
    val catalogCover = game.coverFrontUrl?.replace("http://thumbnails.libretro.com/", "https://thumbnails.libretro.com/")
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.VideogameAsset, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            Text(overrides.title(game).take(32), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
        }
        val image = selectedCover ?: catalogCover
        if (!image.isNullOrBlank()) {
            AsyncImage(model = image, contentDescription = "Capa de ${overrides.title(game)}",
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

'''
text = text[:start] + cover + text[end:]
# Prevent hidden games from unexpectedly resurfacing in search results.
replace_once("results[index]?.let { PocketGameCard(it, onPlay, onFavorite, Modifier.fillMaxWidth()) }", "results[index]?.takeUnless { PocketGamePersonalization.get(LocalContext.current).hidden(it) }?.let { PocketGameCard(it, onPlay, onFavorite, Modifier.fillMaxWidth()) }", "hidden titles search")
if "PocketGameActionDialogs(" not in text or "BackHandler(enabled" not in text:
    raise SystemExit("Pocket personalization did not attach to the UI")
shell.write_text(text)

metadata = provider.read_text()
http_url = 'return "http://thumbnails.libretro.com/$systemName/$imageType/$thumbGameName.png"'
if metadata.count(http_url) != 1:
    raise SystemExit("Libretro thumbnail HTTPS upgrade anchor drift")
provider.write_text(metadata.replace(http_url, 'return "https://thumbnails.libretro.com/$systemName/$imageType/$thumbGameName.png"'))
print("Pocket GBA: persistent navigation, long-press editor, safe removal, manual/HTTPS covers applied")
