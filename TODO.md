# TODO: Phone Whisper - Audio File Sharing Transformation

## Phase 1: Preparation & Cleanup
- [x] Create `TODO.md`
- [x] Remove `WhisperAccessibilityService.kt` and references.
- [x] Remove `accessibility_service_config.xml`.
- [x] Remove `BIND_ACCESSIBILITY_SERVICE` and `RECORD_AUDIO` permissions from `AndroidManifest.xml`.
- [x] Update `strings.xml` to remove accessibility-related strings.

## Phase 2: Intent Handling & UI Update
- [x] Add `ACTION_SEND` and `ACTION_SEND_MULTIPLE` intent filters to `MainActivity` in `AndroidManifest.xml` for `audio/*`.
- [x] Update `MainActivity.kt` to handle incoming intents in `onCreate` and `onNewIntent`.
- [x] Redesign `MainActivity` UI to show:
    - Currently selected model.
    - Status of the received file.
    - Transcription result area with a "Copy to Clipboard" button.
    - Progress indicator for transcription.

## Phase 3: Audio Decoding & Transcription
- [x] Implement `AudioDecoder` utility to convert various audio formats (MP3, M4A, AAC) to PCM FloatArray (16kHz mono) required by `sherpa-onnx`.
- [x] Modify `LocalTranscriber` to accept file streams or URIs if needed, or use the decoded PCM.
- [x] Update `TranscriberClient` (Cloud mode) to handle file-based transcription.
- [x] Ensure `PostProcessor` still works on the resulting text.

## Phase 4: Validation & Testing
- [x] Test sharing an audio file from another app (e.g., Voice Recorder, Files, WhatsApp).
- [x] Verify both Local and Cloud transcription modes.
- [x] Verify Post-Processing (Cleanup) works.
- [x] Build and verify APK.

## Known Issues / Bugs
- [x] **Model Loading Crash (v0.4.3/v0.4.4)**: Fixed. Root causes:
  - **AAR Binary Incompatibility**: The `sherpa-onnx` AAR contains Kotlin data classes, but local project definitions were outdated. Refactored `LocalTranscriber.kt` to use the AAR's definitions directly via named parameters and fixed the `OfflineRecognizer` constructor call (added missing `AssetManager` parameter).
  - **Model Corruption (Protobuf parsing failed)**: Interrupted downloads or extractions left partial files. Implemented **atomic extraction**: models are now extracted to a temporary directory and moved to the final destination only upon success.
  - **Improved Validation**: `isInstalled()` now checks for essential files (`.onnx`/`.ort` and `tokens.txt`) instead of just the directory existence.
- [x] **Local Transcription Failure**: Fixed. Root causes:
  - `detectModelConfig()` in `LocalTranscriber.kt` only looked for `tokens.txt` literally, but sherpa-onnx Whisper models use variant-prefixed names like `base.en-tokens.txt`. Added `findTokensFile()` to handle `tokens.txt`, `*-tokens.txt`, `*.tokens.txt`.
  - `findFile()` only matched files starting with `"encoder"` / `"decoder"`, but Whisper model files are named `xxx-encoder.onnx`. Replaced with `findFileContaining()` and `findFileStarting()` that handle hyphen-prefixed names.
  - Added Moonshine v2 support (mergedDecoder without preprocess.onnx).
  - `getOrCreateTranscriber()` in `MainActivity.kt` now lazily loads the transcriber with proper synchronization and prefers the user's selected model.
  - `initLocalModel()` now returns a `Boolean` and updates the status subtitle on success/failure.
- [x] **Model Download UI**: Fixed. Root causes:
  - `refresh()` ran synchronously before `initLocalModel()` finished loading. Now `refresh()` is called *after* `initLocalModel()` completes, via nested threads.
  - Added status text feedback: "Model installed → Model ready: name" on success, or "Failed to load model" on failure.
