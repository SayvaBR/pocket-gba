package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.swordfish.lemuroid.lib.library.db.entity.Game
import java.net.URLEncoder

internal val pocketInk = Color(0xFF101116)
internal val pocketPanel = Color(0xFF252731)
internal val pocketText = Color(0xFFF8F7F9)
internal val pocketSecondary = Color(0xFFAFB1BF)
internal val pocketOutline = Color(0xFF3D404B)
internal val pocketColors = listOf(Color(0xFFFF6977), Color(0xFF74BEFF), Color(0xFFA98CFF), Color(0xFF58D9BA))
internal data class PocketConsole(val id: String, val name: String, val symbol: String, val color: Color)
internal val pocketConsoles = listOf(
    PocketConsole("gba", "Game Boy Advance", "ADV", Color(0xFF9B87FF)),
    PocketConsole("snes", "Super Nintendo", "SN", Color(0xFFFFA56B)),
    PocketConsole("nds", "Nintendo DS", "DS", Color(0xFF70BDFF)),
    PocketConsole("gb", "Game Boy", "GB", Color(0xFF65D9AF)),
    PocketConsole("gbc", "Game Boy Color", "COLOR", Color(0xFFE7C66F)),
)

@Composable
internal fun PocketPortraitConsole(console: PocketConsole, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(138.dp).clip(RoundedCornerShape(20.dp))
        .background(Brush.linearGradient(listOf(console.color.copy(alpha = .38f), pocketPanel)))
        .clickable(onClick = onClick)) {
        Text(console.symbol, Modifier.align(Alignment.TopEnd).padding(end = 9.dp, top = 4.dp),
            color = console.color.copy(alpha = .29f), fontSize = 39.sp, fontWeight = FontWeight.Black)
        Column(Modifier.align(Alignment.BottomStart).padding(15.dp)) {
            Box(Modifier.size(29.dp).clip(RoundedCornerShape(9.dp))
                .background(console.color.copy(alpha = .23f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.VideogameAsset, null, tint = console.color, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(console.name, color = pocketText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun PocketPortraitArtwork(game: Game, metadata: PocketGamePersonalization,
    modifier: Modifier, scaling: ContentScale = ContentScale.Crop) {
    Box(modifier.background(pocketPanel), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Filled.VideogameAsset, null, tint = pocketSecondary,
                modifier = Modifier.size(36.dp))
            Text(metadata.title(game).take(32), color = pocketSecondary, fontSize = 11.sp,
                maxLines = 2, modifier = Modifier.padding(horizontal = 12.dp))
        }
        val url = game.coverFrontUrl?.replace("http://", "https://") ?: pocketArtworkFallback(game)
        if (!url.isNullOrBlank()) AsyncImage(model = url,
            contentDescription = "Capa automática de ${game.title}", contentScale = scaling,
            modifier = Modifier.fillMaxSize())
    }
}

private fun pocketArtworkFallback(game: Game): String? {
    val system = when (game.systemId) {
        "gba" -> "Nintendo - Game Boy Advance"
        "gb" -> "Nintendo - Game Boy"
        "gbc" -> "Nintendo - Game Boy Color"
        "snes" -> "Nintendo - Super Nintendo Entertainment System"
        "nds" -> "Nintendo - Nintendo DS"
        else -> return null
    }
    val title = game.fileName.substringBeforeLast('.', game.fileName)
        .replace(Regex("\\s*\\([^)]*\\)|\\s*\\[[^]]*]"), " ").trim()
    if (title.isEmpty()) return null
    val name = URLEncoder.encode("$title (USA).png", "UTF-8").replace("+", "%20")
    val folder = URLEncoder.encode(system, "UTF-8").replace("+", "%20")
    return "https://thumbnails.libretro.com/$folder/Named_Boxarts/$name"
}
