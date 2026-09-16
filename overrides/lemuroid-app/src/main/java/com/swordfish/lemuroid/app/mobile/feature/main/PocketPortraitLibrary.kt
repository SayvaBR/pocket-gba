package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
internal fun PocketPortraitLibrary(games: LazyPagingItems<Game>, metadata: PocketGamePersonalization,
    accent: Color, filter: String?, onFilter: (String?) -> Unit, onImport: () -> Unit,
    onPlay: (Game) -> Unit, onDetails: (Game) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 21.dp, end = 12.dp, top = 17.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Biblioteca", Modifier.weight(1f), color = pocketText, fontSize = 28.sp,
                fontWeight = FontWeight.Bold)
            IconButton(onClick = onImport) { Icon(Icons.Filled.Add, "Adicionar pasta", tint = accent) }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(null to "Todos") + pocketConsoles.map { it.id to it.name }).forEach { (id, name) ->
                val active = id == filter
                Surface(Modifier.clickable { onFilter(id) }, shape = RoundedCornerShape(25.dp),
                    color = if (active) accent else pocketPanel) {
                    Text(name, Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                        color = if (active) pocketInk else pocketSecondary, fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
        Text("${games.itemCount} jogos", Modifier.padding(start = 22.dp, bottom = 13.dp),
            color = pocketSecondary, fontSize = 12.sp)
        if (games.itemCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Icon(Icons.Filled.Folder, null, tint = accent)
                    Text(if (games.loadState.refresh is LoadState.Loading) "Carregando biblioteca…"
                         else "Nenhum jogo nesta coleção", color = pocketText,
                        fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Adicione uma pasta ou altere o filtro.", color = pocketSecondary, fontSize = 12.sp)
                    Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = accent)) {
                        Text("Adicionar pasta", color = pocketInk)
                    }
                }
            }
        } else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 25.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp), verticalArrangement = Arrangement.spacedBy(21.dp)) {
            items(count = games.itemCount, key = { games[it]?.id ?: "pending-$it" }) { i ->
                games[i]?.takeUnless { metadata.hidden(it) }?.let {
                    PocketPortraitGameCard(it, metadata, accent, Modifier.fillMaxWidth(), onPlay, onDetails)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PocketPortraitGameCard(game: Game, metadata: PocketGamePersonalization, accent: Color,
    modifier: Modifier, onPlay: (Game) -> Unit, onDetails: (Game) -> Unit) {
    Column(modifier.combinedClickable(onClick = { onPlay(game) }, onLongClick = { onDetails(game) })) {
        Box(Modifier.fillMaxWidth().height(181.dp).clip(RoundedCornerShape(16.dp))
            .background(pocketPanel)) {
            PocketPortraitArtwork(game, metadata, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopStart).padding(8.dp).clip(RoundedCornerShape(8.dp))
                .background(pocketInk.copy(alpha = .88f)).padding(horizontal = 7.dp, vertical = 4.dp)) {
                Text(game.systemId.uppercase(), color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(metadata.title(game), color = pocketText, fontSize = 13.sp, lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun PocketPortraitSearch(query: String, onQuery: (String) -> Unit,
    results: LazyPagingItems<Game>, metadata: PocketGamePersonalization, accent: Color,
    onPlay: (Game) -> Unit, onDetails: (Game) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(19.dp))
        Text("Encontrar jogo", color = pocketText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            singleLine = true, placeholder = { Text("Buscar na biblioteca", color = pocketSecondary) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = accent) },
            shape = RoundedCornerShape(16.dp))
        Spacer(Modifier.height(18.dp))
        if (query.isBlank()) Text("Encontre seus jogos em todos os consoles.", color = pocketSecondary, fontSize = 13.sp)
        else if (results.itemCount == 0 && results.loadState.refresh !is LoadState.Loading) {
            Text("Nenhum resultado encontrado.", color = pocketSecondary, fontSize = 13.sp)
        } else LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(13.dp), verticalArrangement = Arrangement.spacedBy(19.dp)) {
            items(count = results.itemCount) { i ->
                results[i]?.takeUnless { metadata.hidden(it) }?.let {
                    PocketPortraitGameCard(it, metadata, accent, Modifier.fillMaxWidth(), onPlay, onDetails)
                }
            }
        }
    }
}
