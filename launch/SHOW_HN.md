# Show HN

## Recommended title

**Show HN: Push-to-talk dictation for Android apps and terminal workflows**

## Alternative titles

- **Show HN: Push-to-talk dictation for Android for Termux and other apps**
- **Show HN: Android dictation that works without switching keyboards**
- **Show HN: A macWhisper-like dictation tool for Android and Termux**

## Recommended post body

I built this because I was frustrated with voice input on Android.

Two things bothered me:

1. most keyboard dictation felt too inaccurate for real use
2. Gemini's voice input didn't fit the workflow I wanted — I wanted to dictate into a normal text field and then keep editing the draft, not use a more one-shot voice UI

So I made a small Android app that adds a floating push-to-talk button on top of any app.

Flow is:

- tap overlay
- speak
- tap again
- transcribe
- insert text into the currently focused field

It works with normal keyboards like SwiftKey/Gboard, so you don't have to switch keyboards.

It supports:

- local on-device transcription
- cloud transcription with your own OpenAI key
- optional post-processing/cleanup for punctuation, formatting, prompts, commands, etc.

A nice use case for me has been **Termux / terminal workflows** on Android.
Termux's main terminal view is a bit special, but swiping the extra-keys row switches to its native text input box, and dictation works well there.

The app is open source. No backend — in cloud mode requests go directly from the phone to OpenAI using the user's own API key.

Repo / APK:
- Repo: https://github.com/kafkasl/phone-whisper
- APK: https://github.com/kafkasl/phone-whisper/releases

Would love feedback, especially on:

- compatibility across apps
- whether the local/cloud split makes sense
- whether this is actually useful beyond my own workflow

## Shorter version

I built this because Android dictation kept missing for me.

Keyboard dictation was often too inaccurate, and Gemini's voice input didn't give me the "dictate, then keep editing the same draft" flow I wanted.

So I made a push-to-talk overlay for Android:

- tap to record
- tap again to stop
- transcribe
- insert into the focused field

It works with your normal keyboard, supports local or cloud transcription, and can optionally post-process the text for punctuation / cleanup.

It's especially useful for me in Termux and other text-heavy workflows.

Open source, no backend, early MVP:
- https://github.com/kafkasl/phone-whisper

## First comment

A couple of notes that may answer the first questions people usually have:

- It uses Android Accessibility Service only to insert text into the currently focused field.
- It does **not** replace the keyboard.
- It supports both local models and cloud transcription.
- In cloud mode, requests go directly from the phone to OpenAI using the user's own API key.
- Some apps use custom input surfaces, so insertion is not universal. Clipboard fallback is built in.
- In Termux, you need to swipe the extra-keys row to switch to Termux's native text input box.
