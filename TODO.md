# TODO: Phone Whisper - Audio File Sharing Transformation

## Phase 1: Preparation & Cleanup
- [ ] Create `TODO.md` (Done)
- [ ] Remove `WhisperAccessibilityService.kt` and references.
- [ ] Remove `accessibility_service_config.xml`.
- [ ] Remove `BIND_ACCESSIBILITY_SERVICE` and `RECORD_AUDIO` permissions from `AndroidManifest.xml`.
- [ ] Update `strings.xml` to remove accessibility-related strings.

## Phase 2: Intent Handling & UI Update
- [ ] Add `ACTION_SEND` and `ACTION_SEND_MULTIPLE` intent filters to `MainActivity` in `AndroidManifest.xml` for `audio/*`.
- [ ] Update `MainActivity.kt` to handle incoming intents in `onCreate` and `onNewIntent`.
- [ ] Redesign `MainActivity` UI to show:
    - Currently selected model.
    - Status of the received file.
    - Transcription result area with a "Copy to Clipboard" button.
    - Progress indicator for transcription.

## Phase 3: Audio Decoding & Transcription
- [ ] Implement `AudioDecoder` utility to convert various audio formats (MP3, M4A, AAC) to PCM FloatArray (16kHz mono) required by `sherpa-onnx`.
- [ ] Modify `LocalTranscriber` to accept file streams or URIs if needed, or use the decoded PCM.
- [ ] Update `TranscriberClient` (Cloud mode) to handle file-based transcription.
- [ ] Ensure `PostProcessor` still works on the resulting text.

## Phase 4: Validation & Testing
- [ ] Test sharing an audio file from another app (e.g., Voice Recorder, Files, WhatsApp).
- [ ] Verify both Local and Cloud transcription modes.
- [ ] Verify Post-Processing (Cleanup) works.
- [ ] Build and verify APK.
