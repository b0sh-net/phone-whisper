# Phone Whisper landing page requirements

## Goal

Build a very small landing page that makes Phone Whisper feel real and trustworthy.

This is not a startup-marketing site. It should feel like a polished indie open-source project by one developer.

Primary jobs:

1. explain what the app does in 5 seconds
2. make the APK easy to download
3. explain why Accessibility is needed
4. build trust around privacy and local mode
5. give Product Hunt a proper homepage URL

## Tone

Plain, personal, technical, calm.

Good references:
- indie app launch pages
- Jordi Bruin style: simple, product-first, not too salesy

Avoid:
- hypey AI copy
- gradients everywhere
- big startup claims
- corporate language
- fake social proof

## Core message

**Push-to-talk dictation for Android that works across apps without switching keyboards.**

Supporting line:

**Tap the floating button, speak, tap again, and your text is inserted into the currently focused field. Use local models or OpenAI Whisper.**

## Site shape

One page only.

No blog, no auth, no backend, no CMS.

Sections, in order:

1. hero
2. short product demo / screenshots
3. dev / Termux niche section
4. how it works
5. local vs cloud
6. why Accessibility is needed
7. privacy + open source
8. FAQ
9. footer

## Primary CTAs

Above the fold:
- **Download APK**
- **View on GitHub**

Secondary CTAs:
- **Contact me** → `mailto:pol.avms@gmail.com`
- **Sponsor the project** → `https://github.com/sponsors/kafkasl`

## Visual style

The page should match the app's current design language.

### Colors

Use the same accent blue as the Android app:
- light mode primary: `#1A73E8`
- dark mode primary: `#8AB4F8`

Related colors from the app:
- light primary container: `#D2E3FC`
- light on-primary-container: `#174EA6`
- dark primary container: `#1A73E8`
- dark on-primary-container: `#D2E3FC`

Overlay / recording button states should also match the app:
- idle: dark neutral `#1C1C1E` feel
- recording: red `#EF4444`
- busy/transcribing: gray `#6B6B6B`

### Overall feel

- clean white / near-white background in light mode
- clean dark background in dark mode
- flat UI
- no heavy gradients
- no glassmorphism
- no floating marketing cards with shadows everywhere
- minimal borders, subtle separators
- comfortable spacing
- product screenshots should do most of the talking
- if using the existing animated phone demo, keep the structure and motion, but restyle it to look like Phone Whisper rather than a generic app

### Typography

- simple modern sans-serif
- clear hierarchy
- large, clean headline
- body text should read like a README, not an ad

### Iconography

- no emoji
- if icons are used, keep them minimal and consistent
- use the app icon where appropriate

## Layout requirements

### Hero

Must include:
- app name: **Phone Whisper**
- one-line pitch
- short explanatory paragraph
- primary CTA: Download APK
- secondary CTA: View on GitHub
- tertiary text links: Contact me / Sponsor
- one product visual: screenshot or short looping demo

Suggested copy:

**Phone Whisper**

Push-to-talk dictation for Android that works across apps without switching keyboards.

Tap the floating button, speak, tap again, and your text is inserted into the currently focused field. Local on-device transcription is supported, and cloud mode works with your own OpenAI API key.

### Hero demo requirements

The hero demo should show the most obvious use case, not the niche one.

Preferred flow:
- open a chat app
- tap the floating button
- speak one short message
- tap again
- show the text inserted into the message field

Suggested caption:
- **Reply by voice, send text**

Important:
- keep the hero demo focused on the core loop
- do not mix the Termux/dev example into the same short hero sequence if it makes the story harder to follow

### Demo / screenshots section

Keep it simple.

Use either:
- one short muted looping video / GIF
- or 2 to 4 screenshots in a clean grid

Suggested screenshots:
1. main settings screen
2. local model download section
3. cloud mode with masked API key
4. overlay used over another app

If an auto-generated phone animation already exists, reuse it. Do not rebuild it from scratch unless necessary. The important part is that it is restyled to match Phone Whisper's actual UI and button states.

### Dev / Termux niche section

This section is explicitly for power users and developers.

Goal: make people think, _if you type into terminals on your phone, this is going to be great for you._

Do not over-explain architecture here. This is a niche, high-signal use case section.

Suggested heading options:
- **For people who use Termux**
- **Great for Termux and prompts**
- **If you type into terminals on your phone, this is going to be great for you**

Suggested supporting copy:

Phone Whisper is also surprisingly useful in Termux and other text-heavy workflows. Dictate prompts, commands, commit messages, or rough notes without switching keyboards.

Suggested visual:
- separate short demo or screenshot showing Termux / terminal text input
- this should live in its own section, not be mixed into the main hero demo

### How it works

Very short, maybe 3 to 5 steps:
- tap the overlay button
- speak
- tap again to stop
- transcribe locally or in the cloud
- insert text into the focused field

### Local vs cloud

Present this as a simple comparison, not a pricing table.

Columns or stacked rows:

#### Local mode
- on-device transcription
- better for privacy
- no API key required
- requires a downloaded model

#### Cloud mode
- uses OpenAI Whisper
- your own API key
- no backend run by me
- optional cleanup with OpenAI

### Why Accessibility is needed

This section is mandatory.

Suggested copy direction:

Phone Whisper uses Android Accessibility Service for one narrow reason: inserting dictated text into the currently focused text field across apps.

It does not replace your keyboard. It does not run background automation. It only acts after you explicitly tap the overlay button.

This should be visually prominent but calm. A bordered info section is fine.

### Privacy + open source

Short section with 3 points:
- local mode keeps audio on-device
- cloud mode sends audio directly to OpenAI from the device
- the project is open source on GitHub

Link to:
- GitHub repo
- privacy policy

### FAQ

Keep it short. Good questions:
- Why not make it a keyboard?
- Why does it need Accessibility?
- Does audio stay on-device?
- Does it work in every app?
- Is it on the Play Store?

Suggested answer direction for Play Store:

Not yet. I'm shipping the APK directly first, tightening the experience, and deciding later whether a Play Store release is worth the extra review overhead.

### Footer

Should feel personal.

Include:
- built by Pol
- GitHub link
- contact email: `pol.avms@gmail.com`
- sponsor link
- privacy policy link

Suggested footer line:

**Built by Pol. A small open-source Android app, shipped early and improved in public.**

## Content requirements

The page should mention these product truths clearly:

- cross-app dictation
- no keyboard switching
- local or cloud transcription
- optional OpenAI cleanup
- your own API key in cloud mode
- Accessibility used only for text insertion into focused fields
- open-source
- early but usable MVP

The page should not claim:

- full accessibility suite
- background automation
- perfect compatibility with every app
- enterprise-grade security
- Play Store availability unless that is true

## Technical requirements

Keep implementation dead simple.

Preferred options:
- plain static HTML/CSS/JS
- or a tiny static site framework if the implementing agent strongly prefers it

Deployment target should be simple:
- GitHub Pages preferred
- Netlify or Vercel also fine

No backend.
No database.
No analytics unless explicitly added later.

## Responsiveness

Must be mobile-first.

Looks good on:
- phone
- laptop
- wide desktop

Hero should stack cleanly on mobile.
Buttons must stay obvious and tappable.

## Dark mode

Support both light and dark mode.

Should follow system preference by default.
Use the app's blue values in both modes.

## Performance

Fast, static, lightweight.

No huge JS bundle.
No autoplay audio.
No large unoptimized videos.

If using a demo video/GIF:
- compress it well
- lazy-load if needed

## Assets needed

The implementing agent should expect these inputs or placeholders:
- app icon
- 2 to 4 screenshots
- optional short demo video/GIF
- APK download URL
- GitHub repo URL
- privacy policy URL

## Nice-to-have, only if cheap

- subtle anchor nav at the top
- copyable APK version number
- small badge for "Open source"
- small badge for "Local mode available"

## Explicit non-goals

Do not build:
- blog
- changelog system
- waitlist
- email capture form
- pricing page
- user accounts
- fancy animations

## Deliverable

A single polished one-page landing page that feels like a clean indie product launch and is good enough to:

- link from GitHub
- use for Product Hunt
- share on Hacker News / Reddit
- reassure users that a real human made this and is reachable
