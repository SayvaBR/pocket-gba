package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.app.mobile.shared.compose.ui.pocketAccentColor
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState

private val accentValues = listOf("blue", "purple", "cyan", "green", "orange", "pink")
private val accentNames = listOf("Azul", "Roxo", "Ciano", "Verde", "Laranja", "Rosa")

@Composable
fun PocketAppearanceSettings() {
    val accent = indexPreferenceState(
        key = "pocket_theme_accent", default = "blue", values = accentValues,
    )
    LemuroidCardSettingsGroup(title = { Text("Aparência") }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Cor do tema", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                accentValues.forEachIndexed { index, _ ->
                    val color = pocketAccentColor(index)
                    val selected = accent.value == index
                    Box(
                        modifier = Modifier.size(42.dp)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) color else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .padding(4.dp)
                            .background(color, CircleShape)
                            .semantics { contentDescription = "Tema ${accentNames[index]}" }
                            .clickable { accent.value = index },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF07111E))
                    }
                }
            }
            Text("Escolhida: ${accentNames.getOrElse(accent.value) { "Azul" }}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium)
        }
        LemuroidSettingsList(
            state = indexPreferenceState(
                key = "pocket_controller_skin", default = "minimal",
                values = listOf("minimal", "classic", "glow"),
            ),
            title = { Text("Skin dos controles") },
            items = listOf("Minimal", "Clássico", "Glow"),
        )
        LemuroidSettingsList(
            state = indexPreferenceState(
                key = "pocket_bezel_style", default = "minimal",
                values = listOf("none", "minimal", "classic", "accent"),
            ),
            title = { Text("Moldura da tela (bezel)") },
            items = listOf("Nenhuma", "Minimal", "Clássica", "Cor do tema"),
        )
        Text(
            "Os estilos de moldura são simples e não alteram a imagem do jogo.",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
