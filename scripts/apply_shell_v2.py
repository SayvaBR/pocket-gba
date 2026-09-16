#!/usr/bin/env python3
"""Replace the Lemuroid Android frontend at the pinned integration boundary.

Keep the tested Dagger setup, data services, game Activity, mGBA and save pipeline.
The mobile MainActivity UI and navigation are now exclusively PocketShell.
Fail fast on source drift rather than silently delivering an unchanged app.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
main = root / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/MainActivity.kt"
source = main.read_text()
start_anchor = "    @OptIn(ExperimentalMaterial3Api::class)\n    @Composable\n    private fun MainScreen(navController: NavHostController) {"
end_anchor = "    override fun activity(): Activity = this"
if source.count(start_anchor) != 1 or source.count(end_anchor) != 1:
    raise SystemExit("Pocket GBA frontend replacement failed: upstream MainActivity drift")
start = source.index(start_anchor)
end = source.index(end_anchor, start)
replacement = '''    @Composable
    private fun MainScreen(navController: NavHostController) {
        AppTheme {
            val libraryModel: GamesViewModel =
                viewModel(factory = GamesViewModel.Factory(retrogradeDb, MetaSystemID.GBA))
            val searchModel: SearchViewModel =
                viewModel(factory = SearchViewModel.Factory(retrogradeDb))

            PocketShell(
                gamesViewModel = libraryModel,
                searchViewModel = searchModel,
                onPlay = { game -> gameInteractor.onGamePlay(game) },
                onFavorite = { game -> gameInteractor.onFavoriteToggle(game, !game.isFavorite) },
                onImport = {
                    com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher.pickFolder(
                        this@MainActivity,
                    )
                },
                onRescan = {
                    com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler.scheduleLibrarySync(
                        this@MainActivity,
                    )
                },
            )
        }
    }

'''
source = source[:start] + replacement + source[end:]
if "PocketShell(" not in source or "NavHost(" in source:
    raise SystemExit("Pocket frontend safety check failed: old navigation remains")
main.write_text(source)
print("Pocket GBA frontend replaced: own navigation, own home/library/search/settings")
