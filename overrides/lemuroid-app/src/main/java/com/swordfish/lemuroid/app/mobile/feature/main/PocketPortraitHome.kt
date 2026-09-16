package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.LazyPagingItems
import com.swordfish.lemuroid.lib.library.db.entity.Game

@Composable
internal fun PocketPortraitHome(
    games: LazyPagingItems<Game>, metadata: PocketGamePersonalization, accent: Color,
    scanning: Boolean, onImport: () -> Unit, onConsole: (String) -> Unit,
    onPlay: (Game) -> Unit, onDetails: (Game) -> Unit,
) {
    val loaded = games.itemSnapshotList.items.filterNot { metadata.hidden(it) }
    val featured = loaded.filter { it.lastPlayedAt != null }.maxByOrNull { it.lastPlayedAt ?: 0L }
        ?: loaded.firstOrNull()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 30.dp)) {
        Spacer(Modifier.height(21.dp))
        Text("Sua coleção", Modifier.padding(horizontal = 21.dp), color = pocketText,
            fontSize = 31.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
        Text("${games.itemCount} jogos", Modifier.padding(start = 22.dp, top = 5.dp, bottom = 21.dp),
            color = pocketSecondary, fontSize = 13.sp)
        if (featured == null) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(23.dp),
                color = pocketPanel) {
                Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    Icon(Icons.Filled.Folder, null, tint = accent, modifier = Modifier.size(34.dp))
                    Text(if (scanning) "Organizando seus jogos…" else "Adicione sua biblioteca", color = pocketText,
                        fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Escolha uma pasta principal ou pastas individuais dos consoles. As capas chegam automaticamente.",
                        color = pocketSecondary, fontSize = 13.sp, lineHeight = 19.sp)
                    Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(13.dp)) {
                        Text("Adicionar pasta", color = pocketInk, fontWeight = FontWeight.Bold)
                    }
                    if (scanning) CircularProgressIndicator(color = accent, modifier = Modifier.size(23.dp))
                }
            }
        } else {
            Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(220.dp)
                .clip(RoundedCornerShape(23.dp)).background(pocketPanel).clickable { onDetails(featured) }) {
                PocketPortraitArtwork(featured, metadata, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
                    listOf(Color.Transparent, pocketInk.copy(alpha = .97f)))))
                Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
                    Text(if (featured.lastPlayedAt != null) "CONTINUAR" else "EM DESTAQUE",
                        color = accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text(metadata.title(featured), color = pocketText, fontSize = 23.sp,
                        fontWeight = FontWeight.Bold, maxLines = 2, lineHeight = 27.sp)
                    Spacer(Modifier.height(11.dp))
                    Button(onClick = { onPlay(featured) }, colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Filled.PlayArrow, null, tint = pocketInk)
                        Text("Jogar", color = pocketInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(26.dp))
        Text("Sistemas", Modifier.padding(horizontal = 21.dp), color = pocketText,
            fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(13.dp))
        pocketConsoles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { system ->
                    PocketPortraitConsole(system, Modifier.weight(1f), { onConsole(system.id) })
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
        if (loaded.isNotEmpty()) {
            Spacer(Modifier.height(13.dp))
            Text("Seus jogos", Modifier.padding(horizontal = 21.dp), color = pocketText,
                fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Spacer(Modifier.height(12.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(loaded.size) { i ->
                    PocketPortraitGameCard(loaded[i], metadata, accent, Modifier.width(144.dp), onPlay, onDetails)
                }
            }
        }
    }
}
