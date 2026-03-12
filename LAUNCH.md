# Phone Whisper — Launch Sprint (~2h)

## Current State

Working MVP: overlay dot → record → transcribe (local or cloud) → inject text.
All logic in programmatic Android views (no XML layouts). Ugly single-page UI.

### Files to change
```
MainActivity.kt          — full rewrite (multi-section UI)
WhisperAccessibilityService.kt — add post-process step, overlay hide/show
TranscriberClient.kt     — add post-process API call
+ new: PostProcessor.kt  — OpenAI chat completion for text cleanup
+ new: res/drawable/ic_launcher_*.xml — app icon
+ new: res/values/colors.xml — theme colors (optional)
```

---

## Tasks (priority order)

### 1. Post-Processing Step (20 min)
**New file: `PostProcessor.kt`**
- Takes raw transcript → sends to OpenAI chat API with cleanup prompt → returns cleaned text
- Default prompt: "Clean up this speech-to-text transcript. Fix punctuation, capitalization, and obvious speech-to-text errors. Keep the original meaning. Return only the cleaned text."
- Stored in SharedPreferences, editable in settings
- Toggle: enable/disable post-processing
- Works with both local and cloud transcription

**Changes to `WhisperAccessibilityService.kt`:**
- After transcription (local or API), optionally run post-process before injection
- Pipeline: record → transcribe → [post-process] → inject

**Test:** `PostProcessorTest.kt` — parse response, build request body

### 2. UI Overhaul (45 min)
**Full rewrite of `MainActivity.kt`** — use ScrollView with collapsible sections

#### Layout Structure
```
┌─────────────────────────────┐
│ 🎤 Phone Whisper            │
│ Status bar (ready/not ready)│
├─────────────────────────────┤
│ ▸ Setup                     │  ← collapsed by default once done
│   Audio permission: ✅       │
│   Accessibility: ✅ / [Enable]│
├─────────────────────────────┤
│ ▸ Transcription Engine      │
│   ○ Local (offline)         │
│   ○ Cloud (OpenAI Whisper)  │
│                             │
│   Local model: moonshine... │
│   [Model info / change]     │
├─────────────────────────────┤
│ ▸ Post-Processing           │
│   [✓] Enable cleanup        │
│   Prompt: [editable text]   │
├─────────────────────────────┤
│ ▸ API Settings              │  ← only shown when cloud or post-process enabled
│   Key: ••••••••sk-xxxx      │
│   [Show] [Save]             │
├─────────────────────────────┤
│ ▸ Overlay                   │
│   [Show/Hide overlay]       │
│   Bubble size: S / M / L    │
├─────────────────────────────┤
│ ▸ Available Models          │
│   Installed:                │
│     • moonshine-tiny-en ✓   │
│   Download via computer:    │
│     make push-model MODEL=..│
├─────────────────────────────┤
│ ▸ About                     │
│   v0.4.0 · GitHub link      │
└─────────────────────────────┘
```

#### Key UX decisions
- **API key hidden by default** — show masked `••••sk-xxxx`, tap to reveal/edit
- **Setup section auto-collapses** when all permissions granted
- **Sections are collapsible** — tap header to expand/collapse
- **Radio buttons** for engine selection (not a switch)
- **Overlay controls** — show/hide toggle (service stays running, just hides the dot)

### 3. Overlay Show/Hide (10 min)
**Changes to `WhisperAccessibilityService.kt`:**
- Add `showOverlay()` / `hideOverlay()` public methods
- Read preference `overlay_visible` (default true)
- MainActivity toggle calls `WhisperAccessibilityService.instance?.hideOverlay()` etc.
- Useful when you don't want the dot visible (e.g., screenshots, presentations)

### 4. App Icon (15 min)
- Create adaptive icon: `ic_launcher_foreground.xml` (mic waveform on colored bg)
- Use Material-style mic icon, rounded background
- Colors: dark (#1C1C1E) background, white mic — matches overlay style
- Update `AndroidManifest.xml` to use adaptive icon

### 5. Model Catalog (15 min)
**Show in "Available Models" section of UI:**

| Model | Size | Speed* | Quality | Type |
|-------|------|--------|---------|------|
| moonshine-tiny-en | ~117MB | ⚡ fastest | ★★☆ | Moonshine |
| whisper-tiny.en | ~40MB | ⚡ fast | ★★☆ | Whisper |
| whisper-base.en | ~80MB | ⚡ fast | ★★★ | Whisper |
| parakeet-110m-ctc | ~60MB | ⚡ fast | ★★★ | NeMo CTC |
| moonshine-base-en | ~200MB | ⚡⚡ | ★★★ | Moonshine |
| parakeet-0.6b-tdt-v3 | ~350MB | ⚡⚡⚡ | ★★★★ | NeMo TDT |

*Speed on Pixel 5, rough estimates

**No in-app downloads for v1** — show the `make push-model` command and model names.
List installed models with a checkmark, others as "available".

### 6. Theme/Colors (5 min, if time permits)
- `colors.xml` with dark theme palette matching overlay
- `themes.xml` override for DayNight to force dark
- Not critical for launch

---

## Time Budget

| Task | Est. | Cumulative |
|------|------|-----------|
| Post-processing | 20 min | 20 min |
| UI overhaul | 45 min | 65 min |
| Overlay show/hide | 10 min | 75 min |
| App icon | 15 min | 90 min |
| Model catalog UI | 15 min | 105 min |
| Theme (optional) | 5 min | 110 min |
| Buffer/testing | 10 min | 120 min |

**Total: ~2 hours** ✅ Doable.

---

## HTML Mockup Guide (for Gemini)

To generate HTML mockups of the UI, describe it as:
- **Android Material Design 3** style
- **Dark theme** (bg #121212, surface #1E1E1E, primary #BB86FC or #10A37F for OpenAI green)
- **Single scrollable page** with collapsible card sections
- Each section has a **header row** (title + chevron) that expands/collapses content
- **Font**: system sans-serif, 14sp body, 12sp caption, 24sp title
- **Spacing**: 16dp padding, 8dp between items, 12dp section margins
- **Status bar** at top: green "Ready" pill or red "Setup needed" pill
- **Radio buttons** for engine selection, **switches** for toggles
- **Masked text field** for API key with eye icon to toggle visibility
- Viewport: **393 x 851px** (Pixel 5 dimensions at 2.75x density → ~393dp wide)

Key interaction notes:
- Sections collapse/expand with smooth animation
- API key field: `••••••••sk-xxxx` → tap eye → shows full key
- Status pill changes color based on readiness
- Only show API section when cloud mode or post-process is enabled
