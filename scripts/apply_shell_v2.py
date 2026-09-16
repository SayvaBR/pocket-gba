#!/usr/bin/env python3
"""Fail-fast integration patch for the pinned upstream MainActivity.

The upstream emulator, ROM indexing, saves and game launch paths remain unchanged.
Only Pocket GBA navigation callbacks are added here.
"""
from pathlib import Path
import sys

root = Path(sys.argv[1])
main = root / "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/MainActivity.kt"
source = main.read_text()
anchor = 'onOpenCoreSelection = { navController.navigateToRoute(MainRoute.SETTINGS_CORES_SELECTION) },'
if source.count(anchor) != 1:
    raise SystemExit("Pocket shell patch failed: pinned MainActivity changed")
source = source.replace(
    anchor,
    anchor + '\n                            onOpenLibrary = { navController.navigate("systems/GBA") },',
    1,
)
main.write_text(source)
print("Pocket GBA shell: Home > Library wired")
