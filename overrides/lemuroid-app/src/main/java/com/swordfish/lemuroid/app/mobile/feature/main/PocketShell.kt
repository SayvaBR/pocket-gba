package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel
import com.swordfish.lemuroid.app.mobile.feature.search.SearchViewModel
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.pocketAccentColor
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.lib.library.db.entity.Game

/* Pocket GBA owns this complete UI. Lemuroid's emulation, ROM index and save services
 * are deliberately kept behind the callbacks below until the backend migration is ready.
 * All bottom navigation/system bar insets are applied at the shell boundary. */
private enum class PocketTab(val label: String) { HOME("Início"), LIBRARY("Biblioteca"), SEARCH("Buscar"), SETTINGS("Ajustes") }
private val accentValues = listOf("blue", "purple", "cyan", "green", "orange", "pink")
private val accentLabels = listOf("Azul", "Roxo", "Ciano", "Verde", "Laranja", "Rosa")

@Composable
fun PocketShell(
    gamesViewModel: GamesViewModel,
    searchViewModel: SearchViewModel,
    onPlay: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onImport: () -> Unit,
    onRescan: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf(PocketTab.HOME) }
    var query by rememberSaveable { mutableStateOf("") }
    val games = gamesViewModel.games.collectAsLazyPagingItems()
    val results = searchViewModel.searchResults.collectAsLazyPagingItems()
    LaunchedEffect(query) { searchViewModel.queryString.value = query }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // This shell handles physical system bars itself; do not double-apply Scaffold insets.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PocketHeader(
                title = if (tab == PocketTab.HOME) "Pocket GBA" else tab.label,
                showImport = tab == PocketTab.HOME || tab == PocketTab.LIBRARY,
                onImport = onImport,
                onSearch = { tab = PocketTab.SEARCH },
            )
        },
        bottomBar = { PocketBottomBar(tab) { tab = it } },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (tab) {
                PocketTab.HOME -> PocketHome(games, onPlay, onFavorite, onImport, onLibrary = { tab = PocketTab.LIBRARY })
                PocketTab.LIBRARY -> PocketLibrary(games, onPlay, onFavorite, onImport)
                PocketTab.SEARCH -> PocketSearch(query, onQuery = { query = it }, results, onPlay, onFavorite)
                PocketTab.SETTINGS -> PocketSettings(onImport, onRescan)
            }
        }
    }
}

@Composable
private fun PocketHeader(title: String, showImport: Boolean, onImport: () -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
            .statusBarsPadding().height(74.dp).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        if (showImport) {
            IconButton(onClick = onImport) { Icon(Icons.Outlined.Add, contentDescription = "Adicionar jogos") }
        }
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, contentDescription = "Buscar jogos") }
    }
}

@Composable
private fun PocketBottomBar(active: PocketTab, onSelect: (PocketTab) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                PocketTab.entries.forEach { tab ->
                    val selected = tab == active
                    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(14.dp)).clickable { onSelect(tab) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                PocketTab.HOME -> Icons.Filled.Home
                                PocketTab.LIBRARY -> Icons.Filled.VideogameAsset
                                PocketTab.SEARCH -> Icons.Filled.Search
                                PocketTab.SETTINGS -> Icons.Filled.Settings
                            },
                            contentDescription = tab.label,
                            tint = tint,
                            modifier = Modifier.size(23.dp),
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(tab.label, color = tint, fontSize = 11.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketHome(
    games: LazyPagingItems<Game>,
    onPlay: (Game) -> Unit,
    onFavorite: (Game) -> Unit,
    onImport: () -> Unit,
    onLibrary: () -> Unit,
) {
    // Direct paging source: never block an existing library behind recent-games or indexing flows.
    val loaded = games.itemSnapshotList.items
    if (loaded.isEmpty()) {
        PocketEmpty(
            title = if (games.loadState.refresh is LoadState.Loading) "Carregando jogos" else "Sua biblioteca começa aqui",
            description = "Adicione uma pasta com seus arquivos .gba para começar.",
            action = "Adicionar jogos",
            onAction = onImport,
        )
        return
    }
    val featured = loaded.maxByOrNull { it.lastPlayedAt ?: 0L } ?: loaded.first()
    val recent = loaded.filter { it.lastPlayedAt != null }.sortedByDescending { it.lastPlayedAt }.filterNot { it.id == featured.id }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 28.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 12.dp, top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (featured.lastPlayedAt != null) "Continue jogando" else "Seus jogos", modifier = Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onLibrary) { Text("Ver todos") }
        }
        PocketHero(featured, onPlay, onFavorite)
        Spacer(Modifier.height(26.dp))
        Text("Biblioteca", Modifier.padding(horizontal = 22.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 22.dp)) {
            items(loaded.size, key = { loaded[it].id }) { index ->
                PocketGameCard(loaded[index], onPlay, onFavorite, Modifier.width(154.dp))
            }
        }
        if (recent.isNotEmpty()) {
            Spacer(Modifier.height(26.dp))
            Text("Recentes", Modifier.padding(horizontal = 22.dp), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(horizontal = 22.dp)) {
                items(recent.size, key = { recent[it].id }) { index ->
                    PocketGameCard(recent[index], onPlay, onFavorite, Modifier.width(154.dp))
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        TextButton(onClick = onImport, modifier = Modifier.padding(horizontal = 14.dp)) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Adicionar jogos")
        }
    }
}

@Composable
private fun PocketHero(game: Game, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.fillMaxWidth().height(218.dp).clickable { onPlay(game) }) {
            PocketCover(game, Modifier.fillMaxSize())
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color(0xF00A1524),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("GAME BOY ADVANCE", fontSize = 10.sp, color = Color(0xFFADC2D8), letterSpacing = 1.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(game.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = { onPlay(game) }, shape = RoundedCornerShape(12.dp)) {
                        Text(if (game.lastPlayedAt != null) "Continuar" else "Jogar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketLibrary(games: LazyPagingItems<Game>, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit, onImport: () -> Unit) {
    if (games.itemCount == 0) {
        PocketEmpty("Nenhum jogo ainda", "Selecione a pasta onde estão seus arquivos .gba.", "Adicionar jogos", onImport)
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${games.itemCount} jogos", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            TextButton(onClick = onImport) { Text("Importar +") }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            items(games.itemCount, key = { games[it]?.id ?: "placeholder-$it" }) { index ->
                val game = games[index]
                if (game != null) PocketGameCard(game, onPlay, onFavorite, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PocketGameCard(game: Game, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).clickable { onPlay(game) }) {
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
        Text(game.title, maxLines = 2, minLines = 1, overflow = TextOverflow.Ellipsis,
            fontSize = 13.sp, lineHeight = 17.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PocketCover(game: Game, modifier: Modifier) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        // A meaningful offline cover placeholder; never invent copyrighted cover art.
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.VideogameAsset, contentDescription = null, modifier = Modifier.size(38.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
            Text(game.title.take(24), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 12.dp))
        }
        if (!game.coverFrontUrl.isNullOrBlank()) {
            AsyncImage(model = game.coverFrontUrl, contentDescription = "Capa de ${game.title}", contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun PocketSearch(query: String, onQuery: (String) -> Unit, results: LazyPagingItems<Game>, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            placeholder = { Text("Nome do jogo") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
        )
        Spacer(Modifier.height(16.dp))
        if (query.isBlank()) {
            PocketInlineEmpty("Encontre um jogo", "Pesquise pelo nome na sua biblioteca.")
        } else if (results.itemCount == 0 && results.loadState.refresh !is LoadState.Loading) {
            PocketInlineEmpty("Nenhum resultado", "Tente outro nome.")
        } else if (results.itemCount == 0) {
            Box(Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 24.dp)) {
                items(results.itemCount, key = { results[it]?.id ?: "result-$it" }) { index ->
                    results[index]?.let { PocketGameCard(it, onPlay, onFavorite, Modifier.fillMaxWidth()) }
                }
            }
        }
    }
}

@Composable
private fun PocketSettings(onImport: () -> Unit, onRescan: () -> Unit) {
    val theme = indexPreferenceState(key = "pocket_theme_accent", default = "blue", values = accentValues)
    val skin = indexPreferenceState(key = "pocket_controller_skin", default = "minimal", values = listOf("minimal", "classic", "glow"))
    val bezel = indexPreferenceState(key = "pocket_bezel_style", default = "minimal", values = listOf("none", "minimal", "classic", "accent"))
    val autosave = booleanPreferenceState(R.string.pref_key_autosave, true)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("Aparência", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        PocketSettingPanel {
            Text("Cor do tema", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                accentLabels.forEachIndexed { index, name ->
                    val color = pocketAccentColor(index)
                    Column(Modifier.clickable { theme.value = index }.padding(horizontal = 1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(if (theme.value == index) 37.dp else 32.dp)
                            .clip(RoundedCornerShape(50)).background(color), contentAlignment = Alignment.Center) {
                            if (theme.value == index) Text("✓", color = Color(0xFF07111E), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(name, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Durante o jogo", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        PocketSettingPanel {
            Text("Skin dos controles", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            PocketChoices(listOf("Minimal", "Clássico", "Glow"), skin.value) { skin.value = it }
            Spacer(Modifier.height(22.dp))
            Text("Moldura da tela (bezel)", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(12.dp))
            PocketChoices(listOf("Nenhuma", "Minimal", "Clássica", "Cor do tema"), bezel.value) { bezel.value = it }
            Spacer(Modifier.height(16.dp))
            Text("As escolhas são aplicadas ao abrir o jogo.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Biblioteca e salvamento", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        PocketSettingPanel {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Salvar automaticamente", modifier = Modifier.weight(1f))
                Switch(checked = autosave.value, onCheckedChange = { autosave.value = it })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            TextButton(onClick = onImport) { Text("Escolher pasta de jogos") }
            TextButton(onClick = onRescan) { Text("Reescanear biblioteca") }
        }
        Text("Pocket GBA • Core mGBA", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp))
    }
}

@Composable
private fun PocketSettingPanel(content: @Composable () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) { content() }
    }
}

@Composable
private fun PocketChoices(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        labels.forEachIndexed { index, label ->
            Surface(shape = RoundedCornerShape(14.dp), color = if (selected == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.clickable { onSelect(index) }) {
                Text(label, modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp), fontSize = 12.sp,
                    color = if (selected == index) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun PocketEmpty(title: String, description: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(horizontal = 28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Filled.VideogameAsset, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) { Text(action) }
        }
    }
}

@Composable
private fun PocketInlineEmpty(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 92.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
