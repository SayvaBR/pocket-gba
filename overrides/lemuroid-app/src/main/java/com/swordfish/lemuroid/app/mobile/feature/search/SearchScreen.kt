package com.swordfish.lemuroid.app.mobile.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.FavoriteToggle
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.LemuroidGameCard
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel,
    searchQuery: String,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
    onResetSearchQuery: () -> Unit,
) {
    val searchState = viewModel.searchState.collectAsState(SearchViewModel.UIState.Idle).value
    val games = viewModel.searchResults.collectAsLazyPagingItems()
    LaunchedEffect(Unit) { onResetSearchQuery() }
    LaunchedEffect(searchQuery) { viewModel.queryString.value = searchQuery }

    when {
        searchState == SearchViewModel.UIState.Idle -> PocketSearchEmpty(
            modifier, "Encontre seus jogos", "Busque pelo nome na sua biblioteca.",
        )
        searchState == SearchViewModel.UIState.Loading -> Box(
            modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator() }
        searchState == SearchViewModel.UIState.Ready && games.itemCount == 0 -> PocketSearchEmpty(
            modifier, "Nenhum resultado", "Tente buscar por outro nome.",
        )
        else -> SearchGamesGrid(modifier, games, onGameClick, onGameLongClick, onGameFavoriteToggle)
    }
}

@Composable
private fun SearchGamesGrid(
    modifier: Modifier,
    games: LazyPagingItems<Game>,
    onGameClick: (Game) -> Unit,
    onGameLongClick: (Game) -> Unit,
    onGameFavoriteToggle: (Game, Boolean) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(count = games.itemCount, key = { games[it]?.id ?: it }) { index ->
            val game = games[index] ?: return@items
            Box {
                LemuroidGameCard(
                    modifier = Modifier.fillMaxWidth(),
                    game = game,
                    onClick = { onGameClick(game) },
                    onLongClick = { onGameLongClick(game) },
                )
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(40.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f), CircleShape),
                ) {
                    FavoriteToggle(game.isFavorite) { onGameFavoriteToggle(game, it) }
                }
            }
        }
    }
}

@Composable
private fun PocketSearchEmpty(modifier: Modifier, title: String, detail: String) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
