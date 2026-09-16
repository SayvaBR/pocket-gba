package com.swordfish.lemuroid.app.mobile.feature.settings.general

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import androidx.navigation.NavController
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.main.MainRoute
import com.swordfish.lemuroid.app.mobile.feature.main.navigateToRoute
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidCardSettingsGroup
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsList
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsMenuLink
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsPage
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSlider
import com.swordfish.lemuroid.app.utils.android.settings.LemuroidSettingsSwitch
import com.swordfish.lemuroid.app.utils.android.settings.booleanPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.indexPreferenceState
import com.swordfish.lemuroid.app.utils.android.settings.intPreferenceState
import com.swordfish.lemuroid.app.utils.android.stringListResource

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel,
    navController: NavController,
) {
    val state = viewModel.uiState.collectAsState(SettingsViewModel.State()).value
    val scanInProgress = viewModel.directoryScanInProgress.collectAsState(false).value
    val indexingInProgress = viewModel.indexingInProgress.collectAsState(false).value
    val context = LocalContext.current

    LemuroidSettingsPage(modifier = modifier) {
        PocketAppearanceSettings()

        LemuroidCardSettingsGroup(title = { Text("Biblioteca") }) {
            val currentDirectoryName = remember(state.currentDirectory) {
                runCatching {
                    DocumentFile.fromTreeUri(context, Uri.parse(state.currentDirectory))?.name
                }.getOrNull() ?: "Nenhuma pasta selecionada"
            }
            LemuroidSettingsMenuLink(
                title = { Text("Pasta dos jogos") },
                subtitle = { Text(currentDirectoryName) },
                onClick = { viewModel.changeLocalStorageFolder() },
                enabled = !indexingInProgress,
            )
            if (scanInProgress) {
                LemuroidSettingsMenuLink(
                    title = { Text("Parar busca") },
                    onClick = { LibraryIndexScheduler.cancelLibrarySync(context) },
                )
            } else {
                LemuroidSettingsMenuLink(
                    title = { Text("Atualizar biblioteca") },
                    onClick = { LibraryIndexScheduler.scheduleLibrarySync(context) },
                    enabled = !indexingInProgress,
                )
            }
        }

        val hdMode = booleanPreferenceState(R.string.pref_key_hd_mode, false)
        LemuroidCardSettingsGroup(title = { Text("Jogabilidade e vídeo") }) {
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(R.string.pref_key_autosave, true),
                title = { Text("Salvar automaticamente") },
                subtitle = { Text("Preservar o progresso ao sair de um jogo") },
            )
            LemuroidSettingsSwitch(
                state = booleanPreferenceState(R.string.pref_key_enable_immersive_mode, false),
                title = { Text("Tela imersiva") },
                subtitle = { Text("Ocultar as barras do Android durante o jogo") },
            )
            LemuroidSettingsSwitch(
                state = hdMode,
                title = { Text("Melhoria de imagem HD") },
            )
            LemuroidSettingsSlider(
                enabled = hdMode.value,
                state = intPreferenceState(stringResource(id = R.string.pref_key_hd_mode_quality), 2),
                steps = 1,
                valueRange = 0f..2f,
                title = { Text("Qualidade HD") },
            )
            LemuroidSettingsList(
                enabled = !hdMode.value,
                state = indexPreferenceState(
                    R.string.pref_key_shader_filter,
                    "auto",
                    stringListResource(R.array.pref_key_shader_filter_values).toList(),
                ),
                title = { Text("Filtro visual") },
                items = stringListResource(R.array.pref_key_shader_filter_display_names),
            )
        }

        LemuroidCardSettingsGroup(title = { Text("Controles") }) {
            LemuroidSettingsList(
                state = indexPreferenceState(
                    R.string.pref_key_haptic_feedback_mode,
                    "press",
                    stringListResource(R.array.pref_key_haptic_feedback_mode_values),
                ),
                title = { Text("Vibração ao tocar") },
                items = stringListResource(R.array.pref_key_haptic_feedback_mode_display_names),
            )
            LemuroidSettingsMenuLink(
                title = { Text("Controle Bluetooth e USB") },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_INPUT_DEVICES) },
            )
        }

        LemuroidCardSettingsGroup(title = { Text("Mais opções") }) {
            if (state.isSaveSyncSupported) {
                LemuroidSettingsMenuLink(
                    title = { Text("Sincronização de saves") },
                    onClick = { navController.navigateToRoute(MainRoute.SETTINGS_SAVE_SYNC) },
                )
            }
            LemuroidSettingsMenuLink(
                title = { Text("Informações de BIOS") },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_BIOS) },
            )
            LemuroidSettingsMenuLink(
                title = { Text("Avançado") },
                onClick = { navController.navigateToRoute(MainRoute.SETTINGS_ADVANCED) },
            )
        }
    }
}
