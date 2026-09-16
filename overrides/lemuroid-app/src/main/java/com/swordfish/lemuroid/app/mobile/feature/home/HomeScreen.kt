package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.VideogameAsset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.app.shared.covers.CoverUtils
import com.swordfish.lemuroid.app.utils.android.ComposableLifecycle
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenCoreSelection: () -> Unit,
    onOpenLibrary: () -> Unit = {},
) {
    val context = LocalContext.current
    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) viewModel.updatePermissions(context.applicationContext)
    }
    val state = viewModel.getViewStates().collectAsState(HomeViewModel.UIState()).value
    PocketHomeScreen(
        modifier = modifier,
        state = state,
        onGameClicked = onGameClick,
        onGameLongClick = onGameLongClick,
        onOpenLibrary = onOpenLibrary,
        onSetDirectoryClicked = { viewModel.changeLocalStorageFolder(context) },
    )
}

@Composable
private fun PocketHomeScreen(
    modifier: Modifier,
    state: HomeViewModel.UIState,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenLibrary: () -> Unit,
    onSetDirectoryClicked: () -> Unit,
) {
    val playedGame = state.recentGames.firstOrNull()
    val featuredGame = playedGame ?: state.favoritesGames.firstOrNull() ?: state.discoveryGames.firstOrNull()
    val allVisibleGames = (state.recentGames + state.favoritesGames + state.discoveryGames).distinctBy { it.id }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        if (featuredGame == null) {
            PocketEmptyHome(
                indexing = state.indexInProgress,
                onImport = onSetDirectoryClicked,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        if (playedGame != null) "Continue jogando" else "Comece por aqui",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onOpenLibrary) {
                        Text("Ver todos")
                        Icon(Icons.Outlined.ArrowForward, contentDescription = null)
                    }
                }
                PocketFeaturedGame(
                    game = featuredGame,
                    isContinue = playedGame != null,
                    onClick = { onGameClicked(featuredGame) },
                    onLongClick = { onGameLongClick(featuredGame) },
                )
            }

            PocketGamesRow(
                title = if (state.recentGames.isNotEmpty()) "Recentes" else "Sua biblioteca",
                games = if (state.recentGames.isNotEmpty()) allVisibleGames.filterNot { it.id == featuredGame.id }
                    else state.discoveryGames.filterNot { it.id == featuredGame.id },
                onGameClicked = onGameClicked,
                onGameLongClick = onGameLongClick,
                onOpenLibrary = onOpenLibrary,
            )
            if (state.favoritesGames.isNotEmpty()) {
                PocketGamesRow(
                    title = "Favoritos",
                    games = state.favoritesGames.filterNot { it.id == featuredGame.id },
                    onGameClicked = onGameClicked,
                    onGameLongClick = onGameLongClick,
                    onOpenLibrary = onOpenLibrary,
                )
            }
            TextButton(
                onClick = onSetDirectoryClicked,
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Adicionar jogos")
            }
        }
    }
}

@Composable
private fun PocketFeaturedGame(game: Game, isContinue: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val fallback = remember(game.id) { CoverUtils.getFallbackDrawable(game) }
    val fallbackPainter = rememberDrawablePainter(fallback)
    val shape = RoundedCornerShape(20.dp)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape = shape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(244.dp).clip(shape).clickable(onClick = onClick)) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(game.coverFrontUrl).build(),
                contentDescription = game.title,
                modifier = Modifier.fillMaxSize(),
                fallback = fallbackPainter,
                error = fallbackPainter,
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier.fillMaxWidth().height(94.dp).align(Alignment.BottomCenter)
                    .background(Color(0xE609111B)),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("GAME BOY ADVANCE", color = Color(0xFFADC0D6), fontSize = 10.sp, letterSpacing = 1.1.sp)
                        Text(game.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Button(onClick = onClick, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
                        Text(if (isContinue) "Continuar" else "Jogar", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketGamesRow(
    title: String,
    games: List<Game>,
    onGameClicked: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onOpenLibrary: () -> Unit,
) {
    if (games.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenLibrary) { Text("Ver todos") }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            items(games.size, key = { games[it].id }) { index ->
                val game = games[index]
                LemuroidGameCard(
                    modifier = Modifier.width(150.dp),
                    game = game,
                    onClick = { onGameClicked(game) },
                    onLongClick = { onGameLongClick(game) },
                )
            }
        }
    }
}

@Composable
private fun PocketEmptyHome(indexing: Boolean, onImport: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Outlined.VideogameAsset, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(8.dp))
        Text(if (indexing) "Preparando sua biblioteca" else "Sua biblioteca começa aqui",
            fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Text(if (indexing) "Buscando jogos no dispositivo…" else "Escolha uma pasta com seus arquivos .gba.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!indexing) {
            Button(onClick = onImport, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text("Adicionar jogos")
            }
        }
    }
}
