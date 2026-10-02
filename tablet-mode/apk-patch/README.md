# APK patch — two-column tablet spread (no source needed)

Because there is no source project, this patches the **shipped `Siddur.apk`** directly
(decompile to smali → edit → rebuild → re-sign) to add a **two-page spread on tablets**.

This is the conservative, low-risk first version of the tablet feature:

- **On tablets (sw600dp+):** the prayer screen shows **two reading columns** side by side.
  The right column shows the page after the left one. Each column still scrolls.
- **On phones:** completely unchanged (the patch is a structural no-op there).

The toolbar **toggle** and the **outline-on-the-side** mode are *not* in this APK patch —
they need new classes/resources that are much riskier to inject blind into smali. They are
fully written for the real source in `../patches/` and `../app/`. Get the base spread
working on your tablet first, then we add those.

## What the patch changes

1. **Adds** `res/layout-sw600dp/prayer_fragment.xml` — the normal prayer layout but with the
   `PDFView` wrapped in a horizontal `LinearLayout` next to a second `PDFView`
   (equal weight). No new resource IDs are introduced.
2. **Edits** `PrayerFragment.onViewCreated` (smali): right after the left reader's
   `openFile(...)`, it finds the sibling `PDFView` (child index 1 of the reader's parent)
   and opens it one page ahead. On phones the sibling is the alert bar, not a `PDFView`, so
   an `instanceof` guard skips it. See `PrayerFragment.onViewCreated.smali.patch`.

## Reproduce the build

```bash
# needs: java 11+, internet (to fetch apktool + uber-apk-signer on first run)
./build.sh /path/to/Siddur.apk
# -> out/Siddur-tablet-aligned-debugSigned.apk
```

## Installing it  ⚠️ read this

The rebuilt APK is **re-signed with a different key** than the original (the original
signing key isn't available). Android therefore treats it as a different app:

1. **Uninstall** the current Siddur app first. *(This clears its local settings — language
   choice, last page, etc.)*
2. Enable **Install unknown apps** for your browser/file manager.
3. Install `Siddur-tablet-aligned-debugSigned.apk`.

It will **not** update-over the Play/release version — it must replace it. If you later get
the original source and build with your real keystore, that limitation goes away.

## Caveats (because this can't be tested on a device here)

- Verified to decode, rebuild, sign (v3) and to contain the changes. Runtime behaviour must
  be confirmed on a real tablet.
- The spread renders two pages at once → ~2x the reader's memory. Test the longest prayers.
- Hebrew reads right-to-left; if the columns feel "backwards," we swap column order + offset
  (one-line change). Tell me and I'll flip it.
