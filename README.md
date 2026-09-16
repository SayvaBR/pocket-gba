# Pocket GBA

Pocket GBA is a focused Android Game Boy Advance emulator build based on the open-source Lemuroid/LibretroDroid stack and the mGBA Libretro core.

## Goal

Produce an installable Android APK that can scan and launch user-provided `.gba` ROM files with touch controls, audio, saves and save states.

## Automated build

Every push to `main` runs `.github/workflows/build-apk.yml`. The workflow:

1. fetches the pinned upstream Lemuroid source and its cores;
2. applies the Pocket GBA patch from `scripts/prepare_upstream.sh`;
3. limits the visible library to Game Boy Advance;
4. keeps only the bundled mGBA core in the APK;
5. builds a signed Android debug APK;
6. uploads `PocketGBA-debug.apk` as a GitHub Actions artifact.

No commercial ROMs or proprietary Nintendo BIOS files are included.

## Upstream and license

This project currently derives from [Lemuroid](https://github.com/Swordfish90/Lemuroid), licensed under GPL-3.0, and uses the mGBA Libretro core. Modifications and build scripts in this repository are intended to remain compatible with the applicable upstream licenses.

Pinned Lemuroid revision: `53752bf29bc3f95c50f6c38f70cd4a53450a7098`.
