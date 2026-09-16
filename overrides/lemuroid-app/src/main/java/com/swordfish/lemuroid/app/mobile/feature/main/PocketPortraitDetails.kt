package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper

@Composable
internal fun PocketPortraitDetails(game: Game, metadata: PocketGamePersonalization,
    accent: Color, onPlay: (Game) -> Unit, onFavorite: (Game) -> Unit, onClose: () -> Unit) {
    var editing by remember(game.fileUri) { mutableStateOf(false) }
    var removing by remember(game.fileUri) { mutableStateOf(false) }
    var draft by remember(game.fileUri) { mutableStateOf(metadata.title(game)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Box(Modifier.fillMaxWidth().height(252.dp).clip(RoundedCornerShape(22.dp))
            .background(pocketPanel)) {
            PocketPortraitArtwork(game, metadata, Modifier.fillMaxSize(), ContentScale.Fit)
        }
        Spacer(Modifier.height(19.dp))
        Text(game.systemId.uppercase(), color = accent, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(Modifier.height(5.dp))
        Text(metadata.title(game), color = pocketText, fontSize = 27.sp,
            lineHeight = 31.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(19.dp))
        Button(onClick = { onPlay(game) }, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = accent), shape = RoundedCornerShape(15.dp)) {
            Icon(Icons.Filled.PlayArrow, null, tint = pocketInk)
            Text("Jogar", color = pocketInk, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PortraitAction(Icons.Filled.Edit, "Renomear", accent) {
                draft = metadata.title(game); editing = true
            }
            PortraitAction(if (game.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                "Favorito", accent) { onFavorite(game) }
            PortraitAction(Icons.Filled.Delete, if (metadata.hidden(game)) "Restaurar" else "Ocultar", accent) {
                removing = true
            }
        }
        Spacer(Modifier.height(24.dp))
        Surface(shape = RoundedCornerShape(18.dp), color = pocketPanel,
            border = BorderStroke(1.dp, pocketOutline)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, null, tint = accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(9.dp))
                    Text("Informações", color = pocketText, fontWeight = FontWeight.Bold)
                }
                Text("Arquivo: ${game.fileName}", color = pocketSecondary, fontSize = 12.sp)
                game.developer?.let { Text("Desenvolvedor: $it", color = pocketSecondary, fontSize = 12.sp) }
                Text(if (game.lastPlayedAt == null) "Ainda não jogado" else "Jogado anteriormente",
                    color = pocketSecondary, fontSize = 12.sp)
                Text("Capa identificada automaticamente pelo catálogo.", color = pocketSecondary, fontSize = 12.sp)
            }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { editing = false }, title = { Text("Renomear") },
        text = { OutlinedTextField(draft, { draft = it.take(120) }, singleLine = true) },
        confirmButton = { TextButton(enabled = draft.isNotBlank(), onClick = {
            metadata.rename(game, draft); editing = false
        }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } })
    if (removing) AlertDialog(onDismissRequest = { removing = false },
        title = { Text(if (metadata.hidden(game)) "Restaurar jogo?" else "Ocultar jogo?") },
        text = { Text("Esta ação só muda a biblioteca. Não apaga a ROM nem os saves.") },
        confirmButton = { TextButton(onClick = {
            metadata.setHidden(game, !metadata.hidden(game)); removing = false; onClose()
        }) { Text("Confirmar") } },
        dismissButton = { TextButton(onClick = { removing = false }) { Text("Cancelar") } })
}

@Composable
private fun PortraitAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String,
    accent: Color, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(pocketPanel),
            contentAlignment = Alignment.Center) { Icon(icon, title, tint = accent) }
        Spacer(Modifier.height(7.dp))
        Text(title, color = pocketSecondary, fontSize = 12.sp)
    }
}

@Composable
internal fun PocketPortraitSettings(chosen: Int, onColor: (Int) -> Unit, accent: Color,
    scanning: Boolean, onImport: () -> Unit, onRescan: () -> Unit) {
    val context = LocalContext.current
    val pref = remember(context) { SharedPreferencesHelper.getLegacySharedPreferences(context) }
    val legacy = pref.getString(context.getString(com.swordfish.lemuroid.lib.R.string.pref_key_extenral_folder), null)
    val roots = pref.getStringSet("pocket_library_roots_v1", emptySet()).orEmpty()
        .plus(listOfNotNull(legacy)).distinct().sorted()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Ajustes", color = pocketText, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Text("BIBLIOTECA", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Surface(color = pocketPanel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, pocketOutline)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Pastas de jogos", color = pocketText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("${roots.size} pastas cadastradas. Você pode adicionar pastas de consoles separadamente.",
                    color = pocketSecondary, fontSize = 12.sp)
                roots.forEach { root ->
                    Text("• ${android.net.Uri.parse(root).lastPathSegment ?: root}", color = pocketSecondary,
                        fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Add, null, tint = pocketInk)
                    Text("Adicionar pasta", color = pocketInk, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onRescan) {
                    Icon(Icons.Filled.Refresh, null, tint = accent)
                    Text("Verificar biblioteca", color = accent)
                }
                if (scanning) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(19.dp), color = accent, strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp))
                    Text("Escaneando pastas…", color = pocketSecondary, fontSize = 12.sp)
                }
                Text("Uma varredura interrompida não remove jogos nem saves.", color = pocketSecondary,
                    fontSize = 11.sp)
            }
        }
        Text("APARÊNCIA", color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Surface(color = pocketPanel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, pocketOutline)) {
            Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Cor do launcher", color = pocketText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                    pocketColors.forEachIndexed { index, color ->
                        Box(Modifier.size(39.dp).clip(RoundedCornerShape(20.dp)).background(color)
                            .clickable { onColor(index) }, contentAlignment = Alignment.Center) {
                            if (chosen == index) Text("✓", color = pocketInk, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Text("Vertical • capas automáticas • sem escolher imagens manualmente.",
                    color = pocketSecondary, fontSize = 12.sp)
            }
        }
        Text("Pocket é o frontend. A emulação continua isolada na base técnica.",
            color = pocketSecondary, fontSize = 12.sp)
    }
}
