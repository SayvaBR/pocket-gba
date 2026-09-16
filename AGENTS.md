# Pocket GBA — instructions for coding agents

This repository is a **patch/build repository**, not a checked-in complete Android application. The GitHub Actions workflow fetches a fixed upstream Lemuroid commit and the pinned mGBA core, applies `scripts/prepare_upstream.sh`, copies `overrides/`, applies `scripts/apply_shell_v2.py` and `scripts/apply_game_menu.py`, then builds `:lemuroid-app:assembleFreeBundleDebug`. Do not edit `.work/upstream` or assume local generated files are tracked. Any new overrides or patch steps must also be wired into `.github/workflows/build-apk.yml` and checked for anchor drift.

## Product and safety constraints

- Only Game Boy Advance. User's main goal: clean, original dark UI with accent-color choice, game art, intuitive games library, safe navigation/insets and stable emulation.
- Do not rewrite/remove working ROM loading, mGBA, audio, input, saves or save states merely to change visuals. Never bundle Nintendo ROMs, proprietary BIOS or commercial cover collections.
- User-selected custom names and cover images are presentation metadata; do not mutate ROM files or corrupt/remove saves. 'Remove from library' must be reversible unless a separate, strongly confirmed delete flow is designed and tested.
- Respect Android status/navigation insets on devices using three-button AND gesture navigation. Android Back should navigate tab history and preserve last selected tab on resume. Cover flow needs resilient HTTPS/loading/error placeholders and no claim of 100% automatic matches.
- Existing Lemuroid-derived backend and LibretroDroid imply GPL-3.0 obligations; retain upstream license, notices and source availability. Do not call this code proprietary.
- RetroAchievements requires a real official runtime integration (rcheevos/rc_client, ROM hash, emulator memory callbacks, secure auth, rules); never render fake achievement unlocks. Hardcore requires explicit platform compliance and client validation.

## Working agreement

1. Inspect current `main`, issues, PRs, CI, and user screenshots before altering code.
2. Work in a short-lived feature branch/PR, update this build's patch pipeline, and state clearly what is implemented vs proposed.
3. Build the actual APK on GitHub Actions. A Gradle `BUILD SUCCESSFUL` does not prove the app launches or looks right; use Android emulator smoke tests, screenshots, UI hierarchy and actual navigation interactions. Preserve test screenshots as artifacts.
4. Test both Android navigation modes, foreground/background resume, imported games, long-press, manual cover permission persistence, rename/reset, hide/restore, and gameplay save/load. Avoid changes that require uninstalling the existing app; warn about data and signature compatibility.
5. No approval based solely on mockups or aesthetically empty screenshot. If a test fails, identify the exact failure and fix or explicitly leave blocked.

## Rive CLI

Official CLI: `curl -fsSL https://releases.rive.app/cli/install.sh | bash`; Linux headless CI also needs libEGL/GLES/X11. Commands: `rive doctor`, `rive create pocket-motion`, `rive pocket-motion --verify --format=json`, `rive pocket-motion --once`, `rive pocket-motion --screenshot=preview.png --advance=1s --viewport=390x844`. Read the automatically created Rive project's own `AGENTS.md`, use `rive docs` and `rive schema` before editing RML, and preserve source RML next to compiled `.riv`. Our separate `rive-cli-prototype.yml` proves CLI compilation only; it does NOT wire animations into the APK. Start with a subtle interactive game-card press/release, test accessibility/reduced-motion, and keep native Compose fallback if integration fails. Don't use Rive to animate the video frame or introduce input latency in gameplay.

## Scoped goals

- #1 original subtle Rive motion; #2 RetroAchievements proof of concept; #3 covers+metadata recognition and user overrides.
- The Play Store release needs a proper signed release build, source/legal notices, privacy assessment and QA. Do not publish a debug APK as a finished release.
