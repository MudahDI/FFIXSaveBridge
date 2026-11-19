# FFIX Save Bridge

An offline Android utility for inspecting Final Fantasy IX PlayStation memory-card saves and, in a later milestone, converting them into the PC/Steam save format accepted by FF9Recomp.

## Current status

**Early development — PS1 card inspection is implemented; save conversion is not implemented yet.**

- [x] Android project scaffold (Kotlin, Jetpack Compose, Material 3)
- [x] Android Storage Access Framework file picker
- [x] Read-only inspection of standard 128 KiB PS1 memory-card images
- [x] Locate directory entries matching known FFIX product codes
- [x] Validate directory-frame XOR checksums
- [x] Validate FFIX save-block signature and CRC-16
- [x] Unit tests for invalid input, blank cards, and a synthetic valid save
- [x] GitHub Actions build/test workflow
- [ ] Support additional PS1 wrappers (DexDrive, VMP, PSV, single-block dumps)
- [ ] Parse the complete FFIX PS1 save model
- [ ] Map PS1 fields to the PC/Steam rr2016 save model
- [ ] Build and verify the encrypted `SavedData_ww.dat` container
- [ ] Export a FF9Recomp-compatible ZIP
- [ ] Test conversions with real user-provided save fixtures and in-game round trips

The current app does **not** generate a converted save. File-size matches are only hints, and a successful card inspection is not proof that the save can already be imported into FF9Recomp.

## Target workflow

1. Select a DuckStation memory-card image (`.mcd`, `.mcr`, or `.srm`).
2. Inspect the card directory and locate FFIX save blocks.
3. Validate directory checksums, save signatures, and the save CRC.
4. Parse and map supported game data into the PC/Steam save model.
5. Write and independently verify `SavedData_ww.dat`.
6. Optionally package it for FF9Recomp import.

## Build

Open the repository in Android Studio with JDK 17 and Android SDK 35. The GitHub Actions workflow runs unit tests and assembles a debug APK using Gradle 8.9.

## Safety

- The app works offline.
- The selected source file is read-only and is never overwritten.
- The converter will refuse unsupported or invalid save data rather than guessing.
- Keep an untouched copy of the original memory card.

## Format research and acknowledgements

The PS1 card and FFIX save layout research is based on the public documentation in [thekojac304/ffix-save-converter](https://github.com/thekojac304/ffix-save-converter), which is MIT licensed. No conversion-engine source has been copied into this repository at this stage. If code is adapted, its MIT copyright and license notice will be retained.

Other reference projects:
- FF9Recomp Android: https://github.com/bhuiyah/ff9-recomp-android
- FFIX Save Editor: https://github.com/nintendogamer15/ffix-save-editor

Final Fantasy IX and its game data are owned by Square Enix. This project is an independent utility and is not affiliated with Square Enix.
