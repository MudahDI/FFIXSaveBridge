# FFIX Save Bridge

Android app for investigating and converting Final Fantasy IX PlayStation saves into the PC/Steam save format accepted by FF9Recomp.

## Current status

**Early foundation — conversion is not implemented yet.**

- [x] Android project scaffold (Kotlin, Jetpack Compose, Material 3)
- [x] Android Storage Access Framework file picker
- [x] Read-only file metadata and size-based format candidate hints
- [ ] Validate PS1 memory-card directory entries and save blocks
- [ ] Parse FFIX PS1 save payload and verify its checksum
- [ ] Map PS1 fields to the PC/Steam rr2016 save model
- [ ] Build and verify the encrypted `SavedData_ww.dat` container
- [ ] Export a FF9Recomp-compatible ZIP
- [ ] Automated fixtures and round-trip tests

The app deliberately does not rename files or emit a fake converted save. File-size matches are only hints, not proof that a file is valid.

## Target workflow

1. Select a DuckStation memory-card image (`.mcd`, `.mcr`, or `.srm`).
2. Inspect the card directory and locate valid Final Fantasy IX save blocks.
3. Parse and validate the selected save.
4. Convert supported fields to the PC/Steam save model.
5. Write and verify `SavedData_ww.dat`.
6. Optionally package it for FF9Recomp import.

## Build

Open this repository in Android Studio with JDK 17 and Android SDK 35, then build the `app` configuration.

## Safety

Always keep an untouched copy of the original memory card. This application is designed to work offline and never writes to the selected source file.

## References

- FF9Recomp Android: https://github.com/bhuiyah/ff9-recomp-android
- FFIX Save Editor (format research): https://github.com/nintendogamer15/ffix-save-editor
- FFIX save converter research: https://github.com/thekojac304/ffix-save-converter

Final Fantasy IX and its game data are owned by Square Enix. This project is an independent utility and is not affiliated with Square Enix.
