# FFIX Save Bridge

![FFIX Save Bridge icon](docs/icon.svg)

An offline Android app for converting Final Fantasy IX PS1 saves into the vanilla 2016 PC/Steam save format accepted by FF9Recomp.

**Latest release: [v1.0.0](https://github.com/MudahDI/FFIXSaveBridge/releases/tag/v1.0.0)**

## Features

- Import DuckStation memory-card images (.mcd, .mcr, .srm) through Android's file picker.
- Detect FFIX save entries and validate their CRC / directory checksums.
- Convert a selected PS1 save into the vanilla PC/Steam save container.
- Encrypt the PC save using the rr2016 format through Android JCA.
- Export a ZIP containing SavedData_ww.dat, ready for FF9Recomp import.
- Reopen and verify the generated encrypted container before export.
- Keep the original memory-card image read-only.
- Work offline; no account or network connection is required for conversion.

## How to use

1. Open FFIX Save Bridge.
2. Choose the DuckStation memory-card image (.mcd, .mcr, or .srm).
3. Select the FFIX save entry. The app displays its block, region, party leader, location, gil and integrity checks.
4. Tap **Konversi save terpilih → ZIP FF9Recomp**.
5. Save the generated ZIP.
6. In FF9Recomp, use its Import function and select the ZIP.

The ZIP contains SavedData_ww.dat. The converter currently writes the selected PS1 save to PC slot 1 / file 1 in a new container.

## Compatibility

- Vanilla 2016 PC/Steam SavedData_ww.dat format.
- NTSC-U is the verified PS1 region. PAL is accepted with a warning by the upstream engine but has less test coverage.
- NTSC-J is detected but rejected because the Japanese text encoding is not implemented.
- Modded/newer PC encryption formats are not supported.
- One selected PS1 save is exported per ZIP.
- The app has been built, installed, and tested with a real conversion imported successfully into FF9Recomp on Android.

## Build

Open this repository in Android Studio with JDK 17, Android SDK 35, and Python 3.11 available to Gradle. The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, and Chaquopy 17.2.0.

GitHub Actions runs unit tests and assembles the APK. The public v1.0.0 release APK is debug-signed so it can be installed directly; a private release-signing key has not been configured in this repository.

## Third-party code and notices

The PS1/PC conversion engine is adapted from [thekojac304/ffix-save-converter](https://github.com/thekojac304/ffix-save-converter), version 0.3.0-dev, under the MIT License. Its license and third-party notices are retained in THIRD_PARTY_LICENSES/.

Final Fantasy IX and its game data are owned by Square Enix. This project is an independent utility and is not affiliated with Square Enix.
