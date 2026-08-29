<p align="center">
  <img src="docs/logo.svg" width="128" height="128" alt="Audio To Text Logo">
</p>

# Audio To Text

Audio To Text is an Android utility to transcribe audio files via the "Share" menu.

> **Note**: This project is a fork of [https://github.com/kafkasl/phone-whisper](https://github.com/kafkasl/phone-whisper). While the original project provided push-to-talk dictation via an Accessibility Service, this fork repurposes the tool specifically for **transcribing shared audio files**.

> 🧪 **Public testing on Google Play**: Audio To Text is now available on Google Play and is **looking for testers**. Try it at [https://play.google.com/store/apps/details?id=net.b0sh.audiotext](https://play.google.com/store/apps/details?id=net.b0sh.audiotext) — if you'd like to join the **closed testing** track, subscribe to the testers Google Group at [https://groups.google.com/g/testers-community](https://groups.google.com/g/testers-community).

It supports:

- **Local on-device transcription** with sherpa-onnx

## Changelog

### v0.6.6 (2026-08-29)
- **UI fixes**: the "More info" button on the main screen now uses the same outlined style and margins as the "Close" button of the About screen; the About screen title was lowered to avoid overlapping the system status bar; the transcription result box now uses the dark frame in dark mode, fixing text/background contrast.
- **Version bump**: 0.6.5 -> 0.6.6 (versionCode 13).

### v0.6.5 (2026-08-29)
- **About screen**: added a "More info" button on the main screen leading to a new screen with a general description of the project (a personal experiment, published as open source), a link to the GitHub repository (https://github.com/b0sh-net/phone-whisper), and a note that the Issues feature can be used to report problems or get information. The screen states that, being developed in spare time and without profit, no minimum level of support is guaranteed.
- **Version bump**: 0.6.4 -> 0.6.5 (versionCode 12).

### v0.6.4 (2026-08-28)
- **Target SDK 36 (Android 16)**: compileSdk and targetSdk updated to 36.
- **Version bump**: 0.6.3 -> 0.6.4 (versionCode 11).

### v0.6.3 (2026-08-27)
- **Google Play closed testing**: the app is now available for testing on Google Play (https://play.google.com/store/apps/details?id=net.b0sh.audiotext). Looking for testers — join the testers Google Group at https://groups.google.com/g/testers-community.
- **Version bump**: 0.6.2 -> 0.6.3 (versionCode 10).

### v0.6.1 (2026-08-25)
- **Multilingual UI**: the app now supports English and Italian, following the device language. The app name (Audio To Text) and model names are not translated.
- **Version bump**: 0.6.0 -> 0.6.1 (versionCode 8).

### v0.6.0 (2026-08-25)
- **Removed OpenAI integration**: cloud transcription and post-processing have been removed. The app now only supports local on-device transcription with downloaded sherpa-onnx models.
- **Rebrand**: app renamed to **Audio To Text**, package moved to `net.b0sh.audiotext`.
- **Version bump**: 0.5.0 -> 0.6.0 (versionCode 7).

### v0.5.0 (2026-04-20)
- **Separated Transcription UI from Settings**: Created a dedicated `TranscribeActivity` for audio transcription, launched automatically when sharing an audio file via the "Share" menu.
- **Refactored MainActivity**: Removed all transcription logic. It now serves exclusively as the settings panel (engine selection, model catalog, API key).
- **Added TranscriberManager singleton**: Shares the `LocalTranscriber` instance between activities so that model changes in settings persist without re-downloading on each transcription session.
- **Updated Intent filters**: Moved `ACTION_SEND` and `ACTION_SEND_MULTIPLE` filters to `TranscribeActivity`.
- **Version bump**: 0.4.4 -> 0.5.0 (versionCode 7).

### v0.4.4 (2026-04-19)
- **Fixed Model Corruption**: Implemented atomic extraction (extract to temp dir, then move) to prevent loading incomplete/corrupted models if the download or extraction process is interrupted.
- **Improved Installation Validation**: The app now verifies the presence of essential model files before considering a model "installed".
- **Enhanced AAR Compatibility**: Refactored `LocalTranscriber` to align with the `sherpa-onnx` AAR's Kotlin data classes and constructor signatures, resolving runtime `NoSuchMethodError`.
- **Version bump**: 0.4.3 -> 0.4.4 (versionCode 6).

### v0.4.1
- **Fixed Local Transcription Failure**: Resolved model detection logic and lazily loading transcribers.
- **Fixed Model Download UI**: Completion status now properly reflects "installed" without app restart.

## How it works

1. Select an audio file (MP3, M4A, WAV, etc.) in any Android app (e.g., File Manager, Voice Recorder).
2. Tap the **Share** button.
3. Select **Audio To Text**.
4. The app opens, automatically decodes the audio, and performs transcription.
5. The result is displayed on screen, ready to be copied to the clipboard.

## Install

### Build from source

Requires JDK 17 and Android SDK.

```bash
git clone <your-fork-url> && cd phone-whisper
.\gradlew.bat assembleDebug
```

APK output:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

## Setup

1. Open **Audio To Text**.
2. Download a model from the catalog within the app.

## Privacy

Audio To Text works fully offline: audio and transcriptions stay on your device. The only network activity is downloading models from the sherpa-onnx release archives inside the app.

Full policy: [PRIVACY.md](PRIVACY.md)

## Local models

Models are stored in app storage under:

```bash
/data/data/net.b0sh.audiotext/files/models/
```

The app downloads and extracts models directly from the sherpa-onnx release archives.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
