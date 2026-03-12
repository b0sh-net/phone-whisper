# Phone Whisper release plan

## Goal

Ship fast, learn quickly, keep the story simple.

Phone Whisper is not trying to be a new keyboard or a broad automation tool. It is a focused Android dictation app: tap, speak, insert text into the current field.

My plan would be:

1. Launch the APK on GitHub first
2. Get real usage and feedback
3. Tighten the messaging and install flow
4. Only then decide if Play Store is worth the extra work

## Positioning

### One-line pitch

**Push-to-talk dictation for Android that works across apps without switching keyboards.**

### Short version

Phone Whisper adds a floating push-to-talk button to Android. Tap once to record, tap again to transcribe, and the text is inserted into the focused field. It works with local models or OpenAI Whisper.

### What to emphasize

- works across apps
- no keyboard switching
- local mode available
- your own OpenAI key in cloud mode
- Accessibility is used only for focused-field text insertion

### What not to emphasize

- automation
- app control
- screen reading
- disability-first framing unless that becomes the actual product

## Launch assets

### Must-have

- public GitHub repo
- GitHub Release with APK
- README that builds trust quickly
- privacy policy
- 2 to 4 screenshots
- 1 short GIF or video
- clear install steps
- clear explanation of why Accessibility is needed

### Nice-to-have

- simple landing page
- demo video with voiceover
- issue template for device/app compatibility reports
- changelog per release

## Distribution

### 1. GitHub release

This is the real launch.

Ship one APK, write a clean release note, and keep the ask simple: try it and tell me where it breaks.

Suggested release title:

**Phone Whisper v0.3.0 — cross-app Android dictation**

What I want from this channel:

- installs
- bug reports
- device compatibility data
- a few people who genuinely use it every day

### 2. Reddit

Best early traction source if the post is useful and not over-marketed.

#### Good targets

- `r/macapps`
  - angle: people already comparing MacWhisper, Superwhisper, Wispr Flow, VoiceInk
  - angle here is: **Android equivalent / companion for people who like voice dictation**
- `r/androidapps`
  - angle: **voice dictation without replacing the keyboard**
- `r/accessibility`
  - angle: **cross-app voice input / reduced typing**, but keep it honest and practical
- `r/productivity`
  - angle: **quick capture and messaging by voice**
- `r/ProductivityApps`
  - angle: same as above
- `r/LocalLLaMA`
  - angle: **local Whisper-style transcription on Android**

#### Notes

- Do not spray the same post everywhere
- Read the rules first
- The best Reddit posts are concrete: what it does, why you built it, what still breaks
- For `r/macapps`, the hook is not “Android app”. The hook is “if you like MacWhisper-style dictation, I built the Android version I wanted”

#### Useful thread themes to search and reply under

- MacWhisper alternatives
- Superwhisper vs Wispr Flow
- Android Whisper dictation keyboard
- voice typing better than Gboard
- local dictation on Android

### 3. Hacker News

Use **Show HN** if the repo and demo are clean.

Best angle:

**Show HN: Phone Whisper — cross-app dictation for Android without replacing the keyboard**

Why it can work:

- technical product
- local mode story
- open-source angle
- unusual Android Accessibility implementation

HN will care about:

- why not an IME?
- why Accessibility?
- latency
- privacy
- what works locally

Prepare answers for those in advance.

### 4. Product Hunt

I would only do this once the page and assets look polished. PH is less about deep technical discussion and more about presentation.

#### Needed before launch

- app icon
- feature image
- screenshots
- short explainer video
- tagline
- first comment from maker
- a few friends ready to support in the first hours

#### Draft tagline

**Android dictation that works across apps without switching keyboards**

#### Draft description

**Tap a floating button, speak, and insert text into the current field. Local models or OpenAI Whisper.**

### 5. X / Indie Hackers / personal network

Good support channels, not primary ones.

I would use them to point people toward the GitHub release, not to carry the launch alone.

## Finding MacWhisper-type users

I would not try to build a list of individual users. I would go where they already talk.

Good places:

- `r/macapps` threads mentioning MacWhisper / Superwhisper / Wispr Flow / VoiceInk
- Product Hunt alternative pages for MacWhisper and similar tools
- HN Show HN posts about dictation apps
- X search for “MacWhisper”, “Superwhisper”, “Wispr Flow”, “voice dictation”

The goal is not to pitch cold. The goal is to join existing conversations with a relevant link.

## Website

A website is useful, but I would not block the launch on it.

### Recommendation

Ship first with:

- GitHub repo
- GitHub releases
- README
- privacy policy

Then, if the project gets traction, add a very small site.

### If we do a site

Keep it to one page:

- headline
- one short demo
- three bullets
- local vs cloud
- why Accessibility is needed
- download APK
- GitHub link
- sponsor link

GitHub Pages is enough for this.

## GitHub Sponsors

Worth setting up now.

### Minimum version

- enable GitHub Sponsors on your profile
- add `.github/FUNDING.yml`
- add one short line in the README

### Optional later

- Ko-fi
- Buy Me a Coffee
- sponsor note in release posts

I would keep the tone simple: if the app saves you time, you can support it.

## Launch materials to prepare

### Screenshots

At minimum:

1. main settings screen
2. local model download screen
3. cloud mode with masked API key
4. overlay in action over another app

### Demo video

Keep it under 30 seconds:

1. open an app with a text field
2. tap overlay
3. speak one sentence
4. tap again
5. show text insertion
6. show local/cloud toggle briefly

### Copy snippets

#### Reddit / HN short intro

I built an Android dictation app for one reason: I wanted MacWhisper-style voice input on Android without replacing the keyboard. Phone Whisper adds a floating push-to-talk button, transcribes locally or with OpenAI, and inserts the text into the focused field.

#### Maker comment for Product Hunt

I built Phone Whisper because Android dictation still felt awkward if you did not want to switch keyboards. I wanted something simpler: tap, speak, insert. This release is still early, but the core loop already works well enough to use every day.

## Launch day checklist

### Before posting

- cut GitHub release
- upload APK
- add screenshots
- add GIF/video
- confirm README is current
- add privacy policy link
- test install on one clean device
- write launch posts in advance

### Launch order

1. GitHub Release
2. Reddit post in the best-fit subreddit
3. HN Show HN
4. X / personal network
5. Product Hunt later, once assets are stronger

## What success looks like

In the first week, I would look for:

- installs, not vanity votes
- a few repeat users
- bug reports with device names
- specific feedback on insertion reliability
- local vs cloud usage preference
- whether people care enough to ask for Play Store / paid version

## Immediate next steps

1. polish README and release notes
2. create the first GitHub Release with APK
3. capture screenshots and a short demo
4. post to one Reddit community, not five at once
5. prepare a Show HN post
6. decide later if a simple landing page is worth the hour
