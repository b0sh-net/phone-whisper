<p align="center">
  <img src="docs/logo.svg" width="128" height="128" alt="Phone Whisper Logo">
</p>

# Phone Whisper

Phone Whisper is an Android utility to transcribe audio files via the "Share" menu.

> **Note**: This project is a fork of [https://github.com/kafkasl/phone-whisper](https://github.com/kafkasl/phone-whisper). While the original project provided push-to-talk dictation via an Accessibility Service, this fork repurposes the tool specifically for **transcribing shared audio files**.

It supports:

- **Local on-device transcription** with sherpa-onnx
- **Cloud transcription** with OpenAI Whisper
- **Optional cleanup** with OpenAI to fix punctuation and grammar

## Changelog

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
3. Select **Phone Whisper**.
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

1. Open **Phone Whisper**.
2. If using **Cloud** transcription, paste your OpenAI API key in the settings.
3. If using **Local** transcription, download a model from the catalog within the app.

## Privacy

Phone Whisper supports two modes:

- **Local mode**: audio stays on-device
- **Cloud mode**: audio is sent directly from your device to OpenAI's transcription API
- **Optional cleanup**: transcript text is sent directly from your device to OpenAI's chat API

I don't run a backend for this app. In cloud mode, requests go straight from your phone to OpenAI using your own API key.

Full policy: [PRIVACY.md](PRIVACY.md)

## Local models

Models are stored in app storage under:

```bash
/data/data/com.kafkasl.phonewhisper/files/models/
```

The app downloads and extracts models directly from the sherpa-onnx release archives.

## License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for details.
