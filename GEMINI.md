# Phone Whisper

Push-to-talk dictation for Android that allows speaking into most apps without switching keyboards.

## Project Overview

Phone Whisper is an Android application (minSdk 30, targetSdk 34) that provides a floating push-to-talk button for voice dictation. It utilizes an Accessibility Service to inject transcribed text directly into the focused input field of the current foreground application.

### Key Features
- **Local Transcription**: Uses `sherpa-onnx` for on-device speech-to-text.
- **Cloud Transcription**: Integration with OpenAI's Whisper API.
- **Post-Processing**: Optional text cleanup (grammar, punctuation, context-specific formatting) via OpenAI's Chat API.
- **Floating Overlay**: A persistent, draggable button for easy access across all apps.
- **Model Management**: In-app catalog for downloading and managing `sherpa-onnx` models.

## Technology Stack
- **Language**: Kotlin 1.9+
- **Framework**: Android SDK (Target 34)
- **Transcription**: `sherpa-onnx` (On-device), OpenAI Whisper API (Cloud)
- **Networking**: OkHttp
- **Compression**: Apache Commons Compress (for model extraction)
- **Build System**: Gradle (Kotlin DSL)

## Core Components
- `WhisperAccessibilityService.kt`: The heart of the app. Manages the overlay UI, handles audio recording (`AudioRecord`), and performs text injection via `AccessibilityNodeInfo` actions.
- `LocalTranscriber.kt`: Wrapper for `OfflineRecognizer` from `sherpa-onnx`. Handles local model detection and transcription.
- `TranscriberClient.kt`: REST client for OpenAI Whisper API.
- `PostProcessor.kt`: Logic for LLM-based text refinement.
- `ModelDownloader.kt`: Service for fetching and extracting `.tar.bz2` models from GitHub releases.
- `MainActivity.kt`: Settings and setup UI.

## Building and Running

The project includes a `Makefile` for common development tasks. 

**Note on Java Version:** Java 17 is recommended. If using a newer version (like Java 25) results in Gradle failures, use a compatible JDK. In this environment, several JDKs are available in `C:\Users\simone.galliani\.jdks`, including:
- `temurin-17.0.17` (Recommended)
- `temurin-21.0.10`

- **Build Debug APK**: `make build` (runs `./gradlew assembleDebug`)
- **Run Unit Tests**: `make test` (runs `./gradlew testDebugUnitTest`)
- **Install via ADB**: `make adb-install`
- **Clean Project**: `make clean`
- **Push Local Model**: `make push-model MODEL=/path/to/model-dir`

The generated APK can be found at: `app/build/outputs/apk/debug/app-debug.apk`

## Development Conventions

### Accessibility Service
- The service must be manually enabled in Android Settings.
- It uses `TYPE_ACCESSIBILITY_OVERLAY` for the UI.
- Text injection prioritizes `ACTION_PASTE`, falling back to `ACTION_SET_TEXT` or clipboard.

### Model Storage
- Models are stored in the app's internal files directory: `/data/data/com.kafkasl.phonewhisper/files/models/`.
- `LocalTranscriber` auto-detects model types based on directory contents (Moonshine, Whisper, NeMo, etc.).

### Preferences
- All settings (API keys, prompts, model selection) are stored in `SharedPreferences` named `"phonewhisper"`.

### Threading
- Never perform transcription or model loading on the Main Thread. Use `thread {}` or other background mechanisms.
