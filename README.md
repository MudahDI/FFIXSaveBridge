# FFIX Save Bridge

An offline Android app for converting Final Fantasy IX PS1 saves into the vanilla 2016 PC/Steam save format accepted by FF9Recomp.

## Current implementation

**PS1 → PC conversion engine is integrated. Android build and real-device conversion still need verification.**

- [x] Android app scaffold (Kotlin, Jetpack Compose, Material 3)
- [x] Android Storage Access Framework file picker
- [x] Embedded Python 3.11 runtime using Chaquopy
- [x] PS1 memory-card / save-block parser
- [x] FFIX save discovery and CRC-16 validation
- [x] PS1 → PC canonical field mapping (story event variables, players, party, items, key items, gil, Tetra Master and playtime)
- [x] rr2016 PC container serializer and AES-256-CBC encryption through Android JCA
- [x] ZIP export containing `SavedData_ww.dat`
- [x] Reopen and verify the generated encrypted container and each output slot before export
- [x] Source memory-card file remains read-only
- [x] Unit tests for the Android AES key derivation and encryption round-trip
- [ ] GitHub Actions build verification
- [ ] Test with the user's real DuckStation memory card and FF9Recomp on Android
- [ ] Batch-convert every FFIX save from a memory card in one export
- [ ] PC template import and richer conversion report UI

## How to use

1. Open FFIX Save Bridge.
2. Choose the DuckStation memory-card image (`.mcd`, `.mcr`, or `.srm`).
3. Select the FFIX save entry. The app displays its block, region, party leader, location, gil and integrity checks.
4. Tap **Konversi save terpilih → ZIP FF9Recomp**.
5. Save the generated ZIP.
6. In FF9Recomp, use its Import function and select the ZIP.

The ZIP contains `SavedData_ww.dat`. The converter currently writes the selected PS1 save to PC slot 1 / file 1 in a new container.

## Conversion and safety

The conversion engine maps the persistent story/event data and gameplay fields rather than copying a few visible stats. It also applies the known transformations for player records, party empty-slot markers, Tetra Master records, playtime, and the encrypted PC container.

The output is independently reopened and each encrypted slot is parsed again before the ZIP is written. If the source CRC or directory checksum fails, or a region/name is unsupported, conversion is refused. The original memory-card image is never modified.

Some PS1-only bytes and PC-only settings do not have a direct equivalent. The upstream conversion engine records defaults and unmapped data in its conversion report. The current Android screen does not yet display the full report.

## Compatibility limits

- Vanilla 2016 PC/Steam `SavedData_ww.dat` format only.
- NTSC-U is the verified PS1 region. PAL is accepted with a warning by the upstream engine but has less test coverage.
- NTSC-J is detected but rejected because the Japanese text encoding is not implemented.
- Modded/newer PC encryption formats are not supported.
- One selected PS1 save is exported per ZIP in this Android milestone.
- The engine's upstream real-game tests are not a substitute for testing this Android build with the user's own save and FF9Recomp.

## Build

Open this repository in Android Studio with JDK 17, Android SDK 35, and Python 3.11 available to Gradle. The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, and Chaquopy 17.2.0.

The GitHub Actions workflow runs unit tests and assembles a debug APK. Once CI succeeds, the APK can be downloaded from the workflow artifact.

## Third-party code and notices

The PS1/PC conversion engine is adapted from [thekojac304/ffix-save-converter](https://github.com/thekojac304/ffix-save-converter), version 0.3.0-dev, under the MIT License. Its license and third-party notices are retained in `THIRD_PARTY_LICENSES/`.

The original project documents real-game PS1 ↔ PC tests, including story continuation, battles, saving and reloading. FFIX Save Bridge embeds its conversion modules and replaces the desktop crypto dependency with Android's standard JCA implementation.

Other references:
- FF9Recomp Android: https://github.com/bhuiyah/ff9-recomp-android
- FFIX Save Editor: https://github.com/nintendogamer15/ffix-save-editor

Final Fantasy IX and its game data are owned by Square Enix. This project is an independent utility and is not affiliated with Square Enix.
