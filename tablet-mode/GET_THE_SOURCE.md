# Getting the source (to build the page-flip spread properly)

The sideways **page-flip spread** is a from-scratch reading view. It can't be done
reliably by patching the compiled APK, but it's straightforward with the app's source.
Everything else (two-column spread, outline-on-the-side, the toggle, origin-aware panel)
is already shipping in the patched APK; this last piece needs the project.

## What "the source" is

The **Android Studio project** that builds this app — the folder with `build.gradle`,
`app/src/main/java/...`, `app/src/main/res/...`, and (ideally) the signing keystore. Or,
at minimum, the **app bundle (`.aab`)** the developer uploaded to Google Play.

The app is **"Siddur – Classic"** (package `org.chabad.siddur.classic`, built by Kehot /
Chabad.org). Version 1.9.0.

## How to get it (in order of preference)

1. **From whoever built/commissioned it.** If you, your organization, or a developer you
   hired built this, ask them for the Android Studio project (a zip is fine) or the `.aab`.
2. **From the publisher.** If it's the official Kehot / Chabad.org app, contact their app
   team and either request the source or propose the tablet feature as a contribution.
   Mention this repo — the whole tablet implementation is already written and waiting.
3. **If you have Play Console access** for this app, you can download the original `.aab`
   from the App Bundle Explorer (Release → App bundle explorer → Downloads). That plus the
   signing key lets a proper update be built.

## What to do once you have it

- Push the project (or the `.aab`) to this repo, on a new branch, and tell me.
- I'll drop in the already-written tablet code from `tablet-mode/` — including
  `TabletSpreadView.kt` (the real page-flip spread) — wire it into `PrayerFragment`,
  build it, and we can actually test it on a tablet.

## What's already written for the source (in `tablet-mode/`)

- `TabletMode.kt` — tablet detection + mode persistence
- `TabletSpreadView.kt` — **the page-flip spread** (ViewPager2, two pages per screen, swipe sideways; RTL-aware)
- `SpreadController.kt` — alternate dual-reader spread
- `app/src/main/res/layout-sw600dp/prayer_fragment.xml` — tablet layout
- `patches/PrayerFragment.additions.kt` — integration points
- `IMPLEMENTATION_GUIDE.md` — how it all fits together

So the source path is "obtain project → I apply the ready code → build + test," not
"start from zero."
