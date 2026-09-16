# Pocket Launcher — independent Android application

This branch is a clean replacement project. **No Lemuroid source, APK, cores, build patches, assets or dependencies are included.** Parent history remains only because GitHub requires a commit parent; this branch's actual Git tree contains only new Pocket files.

Design direction: portrait game launcher, independent UI and storage engine, inspired by documented ZNeko workflows without copying its proprietary application code, logos or assets. ZNeko's public repository at https://github.com/zneko-org/zneko-launcher contains documentation and media but does not publish the launcher application source or a general source license (verified 2026-09-16).

Initial console scope: Game Boy / Color, Game Boy Advance, Super Nintendo and Nintendo DS. The launcher indexes user-granted SAF folders without deleting previously indexed titles; fetches Libretro thumbnails automatically when their filenames match; offers search, favorites, rename, game details and external ACTION_VIEW launch where supported by an installed emulator. It does **not** bundle ROMs or emulator engines.

Known gaps: ZNeko has much broader features (achievements, S3, custom emulator adapters, full metadata and widgets); those are **not** in the initial build. Do not declare feature parity. Do not uninstall previous app or assume old data migrates automatically; this is a different Android application ID.
