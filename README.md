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

### v1.0.1 (2026-09-18)
- **Catalog quality labels**: the catalog now shows clearer quality labels — Parakeet 0.6B (multilingual) is marked "★★★★ Best multilingual quality", the per-language Kroko models are marked "★★★ Best quality for the selected language", and the remaining models show "★★".
- **Version bump**: 1.0.0 -> 1.0.1 (versionCode 24).

### v1.0.0 (2026-09-13) — closed testing
- **First closed-testing release**: consolidates the work of the 0.9.x line (Jetpack Compose Material 3 UI, bottom navigation bar, background downloads via foreground service, auto model re-selection, landscape scrolling, Material Symbols Outlined icons and all related fixes) into a stable release ready for Google Play **closed testing**.
- **Version bump**: 0.9.9 -> 1.0.0 (versionCode 23).

### v0.9.9 (2026-09-13) — internal release
- **Jetpack Compose (Material 3) UI**: the whole interface has been migrated from programmatic Android Views to Jetpack Compose with a Material 3 theme (light/dark), reusing the existing color palette.
- **Bottom navigation bar**: navigation now uses a fixed Material `NavigationBar` at the bottom with 3 destinations — **Home**, **Catalog** and **More info** — each with a Material Symbols Outlined icon. The active tab is highlighted in light blue (primary), the inactive ones are gray. This replaces the previous button/Activity-based navigation.
- **Background downloads (foreground service)**: model downloads keep running even after leaving the catalog (system Back), via a `dataSync` foreground service with a progress notification. When a download finishes in the background, the installed-model list and the catalog refresh automatically.
- **Fix: app freeze (ANR) when pressing Back during a download**: download progress for the main thread is now throttled to whole percentages, removing the main-thread message flood that previously caused the app to freeze and close.
- **Fix: auto-select model after removal**: removing the currently selected model now automatically selects the first remaining installed model.
- **Fix: inactive links in "More info"**: the GitHub and Issues links in the "More info" screen open the browser again (previously they stopped working after the Compose migration and could show raw HTML).
- **Fix: main screen in landscape**: the title and the info text stay fixed; the status panel and the installed models now scroll vertically.
- **Material Symbols Outlined icons**: the download button (previously a plain "↓" character) and the model-remove button (previously an emoji) now use proper Material Symbols Outlined icons.
- **Version bump**: 0.9.4 -> 0.9.9 (versionCode 22).

### v0.9.4 (2026-09-10)
- **Fix: Kroko Italiano crash on load**: fixed a native crash when loading the Kroko 128l Italian model. The model is a *streaming* (online) ASR model, but the app was loading it with the offline recognizer. `LocalTranscriber` now detects streaming models automatically (by scanning the encoder graph for recurrent cache states) and loads them with the streaming recognizer, following the sherpa-onnx online ASR pattern (tail padding + `inputFinished` + decode loop).
- **Fix: download progress for archived models**: restored the intermediate download percentage for archive-based models (e.g. Moonshine). A regression from v0.9.3 made the progress jump from 0% to 100% without intermediate values, because the progress relied on a HEAD request that GitHub release assets answer with a redirect and no content length. Progress is now computed from the final GET response, with the HEAD probe kept only as a fallback.
- **Catalog clarity**: model display names now include the target language (e.g. "Whisper Base - English", "Parakeet 0.6B - Multilanguage") so users can tell English-only models apart from multilingual ones.
- **Version bump**: 0.9.3 -> 0.9.4 (versionCode 21).

### v0.9.3 (2026-09-07)
- **Multi-source model downloads**: the model downloader no longer assumes a single global repository — each catalog entry now defines its own download source, so models hosted on different repositories can be added and downloaded.
- **Uncompressed models**: the catalog supports models distributed as a plain list of files (no archive), downloaded individually into the model directory. The status panel shows the overall progress and the current file, without an extraction phase.
- **New model — Kroko Italiano**: an Italian speech-to-text model (~154 MB) hosted on Hugging Face, downloaded as four uncompressed files.
- **Version bump**: 0.9.2 -> 0.9.3 (versionCode 20).

### v0.9.2 (2026-09-06)
- **Status panel icons**: the status panel on the main screen now shows an icon next to the status text. Each status (ready, initializing, installing, installed, removing, removed, load/download failed, etc.) has its own icon taken from the status icon set, and the icon switches automatically as the app state changes.
- **Version bump**: 0.9.1 -> 0.9.2 (versionCode 19).

### v0.9.1 (2026-09-06)
- **Onboarding fix**: the introduction's three screens now scroll horizontally as intended — previously a vertical `ScrollView` showed only the first image with no way to advance. The carousel now uses an `HorizontalScrollView` with page snap on release and page indicators that update while swiping.
- **Version bump**: 0.9.0 -> 0.9.1 (versionCode 18).

### v0.9.0 (2026-09-04)
- **Onboarding guide**: on first launch, the app shows a short introduction with three swipeable screens illustrated by `icon-graphics/intro-{1,2,3}.png`: how to download the model, to wait for the download and installation, and how to transcribe a message. The intro can be reviewed anytime via a button in the "More info" screen. Images are localized per supported language (English and Italian).
- **Version bump**: 0.8.1 -> 0.9.0 (versionCode 17).

### Pre-0.9.0 — Historical summary
- **From push-to-talk to audio sharing**: the app evolved from a push-to-talk dictation tool (Accessibility Service) into an app that transcribes audio files shared via the "Share" menu, decoding MP3/M4A/AAC/WAV to PCM with MediaCodec and transcribing locally with sherpa-onnx (Moonshine, Whisper, NeMo).
- **Local-only transcription & rebrand**: cloud/OpenAI transcription was removed and the app was renamed to **Audio To Text** (package `net.b0sh.audiotext`).
- **Separated transcription UI**: a dedicated `TranscribeActivity` share-target and the `TranscriberManager` singleton; robust model installs with atomic extraction and installation validation.
- **UX & platform**: "More info"/About screen and UI fixes, multilingual UI (EN/IT), model removal, support for all ABIs, target SDK 36, and the first availability and promotions on Google Play closed testing.

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
