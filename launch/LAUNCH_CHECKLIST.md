# Launch Checklist

Goal: ship a credible first MVP today, get feedback, and avoid spending time on infrastructure or polish that does not affect launch.

## Top priority blocker

- [ ] **App icon**
  - Add a proper launcher icon
  - Verify it looks good on Android home screen and app drawer
  - Verify `AndroidManifest.xml` points to the correct launcher asset

## Must do before launch

### Product
- [ ] Build and install the latest APK
- [ ] Sanity-check the main loop on device:
  - [ ] tap overlay
  - [ ] record
  - [ ] transcribe
  - [ ] inject text into a normal app
  - [ ] clipboard fallback works when injection fails
- [ ] Verify cloud transcription path works
- [ ] Verify post-processing toggle works
- [ ] Verify local model selection/download path works enough for launch
- [ ] Verify overlay feedback states look OK:
  - [ ] idle
  - [ ] recording
  - [ ] transcribing
  - [ ] clipboard fallback
  - [ ] cleanup failure

### Compatibility spot checks
- [ ] Test at least 3 normal apps with standard text fields
- [ ] Test one known custom-input app and confirm clipboard fallback is acceptable
- [ ] Test Termux with the documented workaround:
  - [ ] swipe extra keys row to native input box
  - [ ] dictate there successfully

### Release assets
- [ ] Generate/install final debug or release APK intended for distribution
- [ ] Upload APK to GitHub Releases
- [ ] Confirm release link works
- [ ] Take 2–4 decent screenshots or screen recordings

### Repo / website
- [ ] README is accurate
- [ ] Landing page copy matches README
- [ ] Termux workaround is documented
- [ ] Privacy note is present and accurate
- [ ] Sponsor link is present

## Nice to have if time permits

- [ ] Better launcher icon variants / adaptive icon polish
- [ ] Short demo GIF/video for README or landing page
- [ ] Small compatibility notes section listing apps tested
- [ ] Short troubleshooting section for accessibility + clipboard fallback

## Launch copy

- [ ] Pick final HN title from `launch/SHOW_HN.md`
- [ ] Pick final HN body from `launch/SHOW_HN.md`
- [ ] Pick final Product Hunt tagline/description from `launch/PRODUCT_HUNT.md`
- [ ] Pick final social post from `launch/LAUNCH_TWEET.md`

## Launch sequence

1. [ ] Final APK built
2. [ ] App icon done
3. [ ] README/docs synced
4. [ ] GitHub release created
5. [ ] Repo public and links checked
6. [ ] Post to HN
7. [ ] Post to Product Hunt if desired
8. [ ] Share social post
9. [ ] Watch feedback and reply quickly

## Explicitly out of scope for today

- [ ] Backend / subscription billing
- [ ] Play Store submission
- [ ] Perfect compatibility across all apps
- [ ] Major architecture rewrites

## Rewind note

Later, after launch prep, rewind and revisit:

- [ ] app icon implementation if still incomplete
- [ ] any temporary copy changes that should be refined
- [ ] deeper Termux/compatibility investigation only if needed
