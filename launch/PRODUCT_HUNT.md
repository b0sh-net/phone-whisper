# Product Hunt

## Tagline options

- **Push-to-talk dictation for Android that works across apps**
- **Speak into Android apps without switching keyboards**
- **Whisper-style dictation for Android with local or cloud transcription**

## Recommended tagline

**Speak into Android apps without switching keyboards**

## Short description

Phone Whisper is a push-to-talk dictation app for Android. Tap the floating button, speak, tap again, and your text is inserted into the focused field. It works with your existing keyboard, supports local or cloud transcription, and can optionally clean up the transcript before insertion.

## Longer description

I built Phone Whisper because voice input on Android kept missing for me.

Keyboard dictation often felt too inaccurate, and some AI voice UIs didn't fit the workflow I wanted — I wanted to dictate into the text field I was already using, then keep editing normally.

Phone Whisper adds a small floating push-to-talk button on top of any app:

- tap once to record
- tap again to stop
- transcribe locally or in the cloud
- insert text into the focused field

It works with normal keyboards like SwiftKey or Gboard, so there is no keyboard switching.

It also supports optional post-processing for punctuation, formatting, prompts, and command-style text.

A few details:

- local mode keeps audio on-device
- cloud mode uses your own OpenAI key
- no backend run by me
- clipboard fallback when an app doesn't expose a standard Android input field
- works nicely for Termux if you switch to Termux's native text input box

This is an early MVP, released to see if people actually want this.

Links:
- Repo: https://github.com/kafkasl/phone-whisper
- APK: https://github.com/kafkasl/phone-whisper/releases

## First comment

Thanks for checking it out.

This is intentionally a very early release. I wanted to validate whether there is real demand for better dictation on Android before spending a lot more time on polish, Play Store packaging, or monetization.

A few things that make it different from the voice input I was using before:

- better transcription quality than typical keyboard dictation
- works with the keyboard you already use
- lets you dictate first and keep editing the same draft
- supports local transcription as well as cloud mode

Would especially love feedback on:

- compatibility across apps
- whether local mode is actually useful in practice
- whether the cleanup/post-processing step is valuable

## Maker reply if someone asks why Accessibility is needed

Android doesn't expose a normal cross-app text insertion API, so Accessibility is the sanctioned way to insert text into the currently focused field. The app uses it for that narrow purpose only.
