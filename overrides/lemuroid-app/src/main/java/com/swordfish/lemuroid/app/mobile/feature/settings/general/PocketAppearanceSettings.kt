package com.swordfish.lemuroid.app.mobile.feature.settings.general

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState

@Composable
fun PocketAppearanceSettings() {
    LemuroidCardSettingsGroup(
        title = { Text("Aparência") },
    ) {
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    key = "pocket_theme_accent",
                    default = "blue",
                    values = listOf("blue", "purple", "cyan", "green", "orange", "pink"),
                ),
            title = { Text("Cor do tema") },
            items = listOf("Azul", "Roxo", "Ciano", "Verde", "Laranja", "Rosa"),
        )
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    key = "pocket_controller_skin",
                    default = "minimal",
                    values = listOf("minimal", "classic", "glow"),
                ),
            title = { Text("Skin dos controles") },
            items = listOf("Minimal", "Clássico", "Glow"),
        )
        LemuroidSettingsList(
            state =
                indexPreferenceState(
                    key = "pocket_bezel_style",
                    default = "minimal",
                    values = listOf("none", "minimal", "classic", "accent"),
                ),
            title = { Text("Bezel da tela") },
            items = listOf("Nenhum", "Minimal", "Clássico", "Cor do tema"),
        )
    }
}
