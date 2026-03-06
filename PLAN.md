Here’s the **clean executive summary** of what we decided, what we rejected, and the current best architecture.

---

# 🎯 Goal

Build a **macWhisper-like dictation tool on Android** that:

* Works **in any text field**
* Uses **push-to-talk recording**
* **Automatically inserts text**
* Keeps **SwiftKey as your keyboard**
* Uses **local transcription if possible**
* **Always runs a small API model post-process with a custom prompt**

Personal project → **Play Store rules don’t matter**.

---

# ✅ What We Agreed To Do

## 1️⃣ Keep SwiftKey

We decided **not to build a keyboard (IME)** because:

* Only **one keyboard can be active**
* Switching keyboards constantly is annoying
* You like SwiftKey

So SwiftKey stays your main keyboard.

---

## 2️⃣ Use an Overlay + AccessibilityService

This replaces the IME approach.

Architecture:

```
Hardware button / overlay
        ↓
record audio
        ↓
transcribe
        ↓
post-process via API
        ↓
AccessibilityService injects text
```

Accessibility lets the app:

* detect focused input field
* insert text programmatically
* work across most apps

If insertion fails → fallback to clipboard.

---

## 3️⃣ Push-to-Talk UX

We agreed **push-to-talk** is perfect.

Possible triggers:

* overlay button
* quick settings tile
* hardware button (volume)

Best candidate:

**Hold volume button → record**

Release:

```
stop recording
transcribe
postprocess
insert text
```

---

## 4️⃣ Local Transcription First

Use **whisper.cpp** locally.

Reason:

* proven on Android
* good accuracy
* works offline
* small models available

Recommended model:

```
whisper tiny.en (quantized)
```

Use **utterance transcription**, not live streaming.

---

## 5️⃣ Always Run API Post-Processing

This was a hard requirement from you.

Pipeline:

```
raw transcript
      ↓
OpenAI small model
      ↓
cleaned transcript
```

Purpose:

* punctuation
* vocabulary correction
* formatting
* slang / multilingual fixes

Using a **custom prompt**.

Audio never leaves device unless you choose cloud ASR.

---

## 6️⃣ Keep API Transcription as Fallback

We agreed the architecture should allow easy switching:

```
TranscriptionProvider
    ├── whisper.cpp (local)
    └── OpenAI transcription API
```

So if local is slow or inaccurate:

Just flip a setting.

---

# ❌ What We Rejected

## IME / Custom Keyboard

Rejected because:

* conflicts with SwiftKey
* keyboard switching sucks
* unnecessary complexity

(Though WhisperIME proves the ASR concept works.)

---

## WhisperIME as the final solution

We examined it and concluded:

Good for:

* reference implementation
* benchmarking

But not ideal because:

* it’s an IME
* no custom API post-process
* doesn’t coexist with SwiftKey

---

## Streaming ASR

Discarded because:

* Whisper architecture isn't optimized for it
* wastes compute
* worse latency

Instead:

```
push-to-talk
transcribe chunk
```

(macWhisper style)

---

## Clipboard-only solution

Rejected as primary UX because:

* requires manual paste
* breaks flow

But kept as **fallback if injection fails**.

---

# 🧠 Final Architecture

```
AccessibilityService
      │
      │ detects focused input
      │ injects text
      │
Trigger
(volume key / overlay)
      │
      ▼
Audio capture (16kHz)
      │
      ▼
Local whisper.cpp
      │
      ▼
API postprocess
(custom prompt)
      │
      ▼
Inject text
```

Fallback:

```
if injection fails
      ↓
copy to clipboard
```

Optional fallback:

```
if local ASR slow
      ↓
OpenAI transcription API
```

---

# ⭐ Why This Is the Best Setup

You get:

* SwiftKey unchanged
* near system-wide dictation
* privacy (local ASR)
* high accuracy (LLM cleanup)
* hardware push-to-talk
* cloud fallback
* custom vocabulary prompt

It’s basically **macWhisper but Android-native**.

---

# 🚀 Next Step (Most Useful)

If you want, I can also show you:

**The minimal prototype stack to build this in ~1–2 days**, including:

* the 4 Android components
* whisper.cpp integration strategy
* the exact post-process API call
* injection logic that works across the most apps.

