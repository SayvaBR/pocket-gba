package com.swordfish.lemuroid.app.mobile.feature.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.app.mobile.feature.games.GamesViewModel
import com.swordfish.lemuroid.app.mobile.feature.search.SearchViewModel
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.lib.library.db.entity.Game

/** Entirely Pocket-owned portrait presentation. No inherited Lemuroid navigation,
 * home, system tabs or settings Composables are used. Emulation stays behind callbacks.
 */
@Composable
fun PocketPortraitLauncher(gamesViewModel: GamesViewModel, searchViewModel: SearchViewModel,
    onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit,
    onImport: () -> Unit, onRescan: () -> Unit) {
    val context = LocalContext.current
    val navigation = remember(context) { context.applicationContext.getSharedPreferences(
        "pocket_portrait_navigation", Context.MODE_PRIVATE) }
    var page by rememberSaveable { mutableStateOf(navigation.getString("page", "home") ?: "home") }
    var previous by rememberSaveable { mutableStateOf("home") }
    var console by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<Game?>(null) }
    val metadata = remember(context) { PocketGamePersonalization.get(context) }
    val accentPref = indexPreferenceState(key = "pocket_launcher_accent", default = "coral",
        values = listOf("coral", "blue", "purple", "mint"))
    val accent = pocketColors.getOrElse(accentPref.value) { pocketColors.first() }
    val games = gamesViewModel.games.collectAsLazyPagingItems()
    val results = searchViewModel.searchResults.collectAsLazyPagingItems()
    val scanFlow = remember(context) {
        PendingOperationsMonitor(context.applicationContext).isDirectoryScanInProgress()
    }
    val scanning by scanFlow.collectAsState(initial = false)
    LaunchedEffect(query) { searchViewModel.queryString.value = query }
    LaunchedEffect(page) {
        navigation.edit().putString("page", page).apply()
        if (page != "library") {
            console = null
            gamesViewModel.selectConsole(null)
        }
    }
    fun navigate(route: String) { if (route != page) { previous = page; page = route } }
    BackHandler(enabled = selected != null || page != "home") {
        if (selected != null) selected = null
        else {
            val destination = previous
            previous = "home"
            page = if (destination == page) "home" else destination
        }
    }
    Scaffold(containerColor = pocketInk, contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(Modifier.fillMaxWidth().background(pocketInk).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(68.dp).padding(horizontal = 19.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (selected != null) {
                        IconButton(onClick = { selected = null }) {
                            Icon(Icons.Filled.ArrowBack, "Voltar", tint = pocketText)
                        }
                        Text("Detalhes", Modifier.weight(1f), color = pocketText,
                            fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(accent),
                            contentAlignment = Alignment.Center) {
                            Text("P", color = pocketInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("pocket", Modifier.weight(1f), color = pocketText, fontSize = 25.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
                        IconButton(onClick = { navigate("search") }) {
                            Icon(Icons.Filled.Search, "Buscar", tint = pocketSecondary)
                        }
                        IconButton(onClick = onImport) {
                            Icon(Icons.Filled.Add, "Adicionar pasta", tint = pocketSecondary)
                        }
                        IconButton(onClick = { navigate("settings") }) {
                            Icon(Icons.Filled.Settings, "Ajustes",
                                tint = if (page == "settings") accent else pocketSecondary)
                        }
                    }
                }
                if (selected == null) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                        listOf("home" to "Início", "library" to "Coleção", "search" to "Busca")
                            .forEach { (route, name) ->
                                val active = page == route
                                Column(Modifier.clickable { navigate(route) },
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(name, Modifier.padding(vertical = 11.dp),
                                        color = if (active) pocketText else pocketSecondary,
                                        fontSize = 14.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                                    Box(Modifier.width(if (active) 37.dp else 1.dp).height(3.dp)
                                        .background(if (active) accent else Color.Transparent,
                                            RoundedCornerShape(3.dp)))
                                }
                            }
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = pocketOutline.copy(alpha = .65f))
            }
        }) { padding ->
        Box(Modifier.fillMaxSize().background(pocketInk).padding(padding).navigationBarsPadding()) {
            val details = selected
            if (details != null) {
                PocketPortraitDetails(details, metadata, accent, onPlay, onFavorite) { selected = null }
            } else when (page) {
                "home" -> PocketPortraitHome(games, metadata, accent, scanning, onImport,
                    onConsole = { id ->
                        console = id
                        gamesViewModel.selectConsole(id)
                        navigate("library")
                    }, onPlay = onPlay, onDetails = { selected = it })
                "library" -> PocketPortraitLibrary(games, metadata, accent, console,
                    onFilter = { id -> console = id; gamesViewModel.selectConsole(id) },
                    onImport = onImport, onPlay = onPlay, onDetails = { selected = it })
                "search" -> PocketPortraitSearch(query, { query = it }, results, metadata, accent,
                    onPlay, onDetails = { selected = it })
                else -> PocketPortraitSettings(accentPref.value, { accentPref.value = it },
                    accent, scanning, onImport, onRescan)
            }
        }
    }
}
