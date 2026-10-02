# Tablet mode for the Siddur app — implementation guide

This folder contains a ready-to-apply tablet layout feature for the Kehot "Classic"
Siddur app (`org.chabad.kehossiddur`). It adds, **on tablets only**, two layouts the
user flips between with a toolbar button:

1. **Two-page spread** — two reading columns side by side (open-book style).
2. **Outline on the side** — the table of contents pinned to the left, one reading
   column on the right.

Phones are completely unaffected: everything is gated behind an `isTablet` resource
that is only `true` at `sw600dp` and above.

> **Why this is a set of files + patches and not a rebuilt APK.**
> The GitHub repo contains only a README and the prebuilt `Siddur.apk` — there is no
> source project in it, and the build was done elsewhere. This feature was written by
> reverse-engineering the shipped APK, so it targets the **real Android Studio project**
> (the one that produced the APK). Drop these files into that project, apply the small
> patches, then build and sign as you normally do. See *Building* at the end.

---

## How the app works today (so the design makes sense)

- Each prayer opens in **`PrayerFragment`**, which inflates `prayer_fragment.xml`
  containing one `PDFView` (`R.id.pdfview`).
- `PDFView → MuPDFReaderView → ReaderView` renders the bundled PDF
  (`assets/classic-rev02.pdf`) with **MuPDF**.
- `ReaderView` is a **continuous vertical scroller** (`HORIZONTAL_SCROLLING = false`):
  it stacks the prayer's pages in one tall column and you scroll down through them.
  *(This is the "it scrolls down" behaviour you described.)*
- Navigation funnels through **`MainActivity.showPrayer(pages, displayPage, titles,
  subtitles, alertsForPages, scrollY, overrideTitle)`**, which builds a bundle and calls
  `setFragment(8, bundle)` → a fresh `PrayerFragment` replacing `R.id.content_frame`.
- The outline is **`TOCFragment`** (an `ExpandableListView`); tapping an entry already
  calls `MainActivity.showPrayer(...)`.

Because the reader is a single continuous column, a multi-column tablet view really is
"a different system" — which is the approach taken here.

---

## The design (and why)

Two design rules kept this low-risk:

1. **Treat `ReaderView` as a black box.** Its core layout method (`onLayout3`) is
   complex and does the continuous-scroll math. Rather than rewrite it, the spread runs
   **two `PDFView` instances side by side** and keeps the right one one page ahead of the
   left, using only public reader methods. No change to the vendored reader is required.
2. **Reuse the existing navigation.** The side outline just hosts the existing
   `TOCFragment`; its taps already rebuild the prayer via `MainActivity.showPrayer(...)`.
   The toggle re-opens the current prayer the same way the language toggle already does.

### Mode A — Two-page spread
- `layout-sw600dp/prayer_fragment.xml` puts two `PDFView`s (`pdfview`, `pdfview_right`)
  in a horizontal `LinearLayout`, weight 1 each.
- `SpreadController` opens the right column with the same page list, starting one page
  later, and advances it whenever the left column changes page (`OnShowPageListener`).
- Left column stays the source of truth for the toolbar title and alert bar.
- *Optional:* for pixel-accurate mid-scroll syncing, add the one-line hook in
  `patches/ReaderView.scrollhook.md`.

### Mode B — Outline on the side
- The same tablet layout has a `toc_side_panel` (`FrameLayout`, 320dp) that
  `PrayerFragment` fills with a `TOCFragment` via `childFragmentManager`.
- `pdfview_right` is hidden; the single left column fills the rest.
- Tapping an outline entry calls `MainActivity.showPrayer(...)` → the fragment rebuilds
  with the side panel intact.

### The toggle
- A toolbar item (`R.id.tablet_view_toggle`) visible only on tablets.
- Tapping flips the persisted mode (`TabletMode`) and re-opens the current prayer so the
  fragment rebuilds in the new layout. The icon reflects the current mode.

---

## Files in this folder

| File | Goes to | What it is |
|---|---|---|
| `app/src/main/res/values/bools.xml` | `res/values/bools.xml` | `isTablet = false` (phones) |
| `app/src/main/res/values-sw600dp/bools.xml` | `res/values-sw600dp/bools.xml` | `isTablet = true` (tablets) |
| `app/src/main/res/values/strings_tablet.xml` | merge into `res/values/strings.xml` | toggle strings |
| `app/src/main/res/layout-sw600dp/prayer_fragment.xml` | `res/layout-sw600dp/prayer_fragment.xml` | tablet prayer layout |
| `app/src/main/res/drawable/ic_view_spread.xml` | `res/drawable/` | toggle icon (spread) |
| `app/src/main/res/drawable/ic_view_outline.xml` | `res/drawable/` | toggle icon (outline) |
| `app/src/main/res/menu/menu_main_tablet_additions.xml` | merge into `res/menu/menu_main.xml` | toggle menu item |
| `app/src/main/java/org/chabad/kehossiddur/TabletMode.kt` | `.../kehossiddur/` | tablet detection + mode persistence |
| `app/src/main/java/org/chabad/kehossiddur/SpreadController.kt` | `.../kehossiddur/` | drives the 2nd reading column |
| `patches/PrayerFragment.additions.kt` | apply into `PrayerFragment.kt` | the integration points |
| `patches/ReaderView.scrollhook.md` | optional | pixel-accurate scroll sync |

---

## Step-by-step

1. **Copy the new files** into the matching paths of your project (new `TabletMode.kt`,
   `SpreadController.kt`, the two `bools.xml`, the tablet layout, both drawables).
2. **Build the tablet layout from your real phone layout.** Copy your existing
   `res/layout/prayer_fragment.xml` to `res/layout-sw600dp/prayer_fragment.xml`, then fold
   in the three additions marked `<!-- TABLET -->` in the provided file (the side panel,
   the divider, and the `pdfview_right` column). This keeps your real alert-bar styling.
3. **Merge the strings** from `strings_tablet.xml` into `res/values/strings.xml` (and any
   localized `values-*/strings.xml`).
4. **Add the menu item** from `menu_main_tablet_additions.xml` into your real
   `res/menu/menu_main.xml`.
5. **Apply `patches/PrayerFragment.additions.kt`** — five small edits (new fields; a
   `setUpTabletLayout(...)` call replacing the single `pdfView.setOnShowPageListener(this)`
   line; menu visibility; the toggle branch; bitmap release). Each block names its method.
6. *(Optional)* apply `patches/ReaderView.scrollhook.md` for pixel-accurate spread syncing.
7. **Build, sign with your existing keystore, install on a tablet.** Flip the toolbar
   toggle to switch spread ↔ outline.

---

## Verify on a device/emulator

- **Phone (e.g. Pixel 7):** identical to today — single scroll-down column, no toggle icon.
- **Tablet (e.g. Pixel Tablet / Nexus 9, or any emulator ≥ 600dp):**
  - Toggle icon appears in the toolbar.
  - **Spread:** two pages side by side; scrolling the left column advances the right by one
    page; title/alerts still update from the left column.
  - **Outline:** TOC on the left, one reading column on the right; tapping an entry loads
    that prayer and the outline stays put.
  - Rotate the device: layout holds (sw600dp applies in both orientations — see the comment
    in `values-sw600dp/bools.xml` if you want landscape-only).

---

## Notes, limits, and follow-ups

- **Memory:** the spread renders two pages at once, so it roughly doubles the reader's
  bitmap use. MuPDF is efficient and `ReaderView` already has an out-of-memory guard, but
  test the longest prayers on a low-end tablet. If needed, cap the spread to landscape only
  (move `values-sw600dp/bools.xml` → `values-sw600dp-land/`).
- **Scroll sync granularity:** default sync is per page (robust, no reader edits). Pixel
  sync is the optional hook. For a page-based siddur, per-page is usually what you want.
- **Right-to-left:** Hebrew siddurim read right-to-left, so a true book spread may want the
  *later* page on the left. If so, swap the two columns in the layout (put `pdfview_right`
  first) and open the left column at `start` and the right at `start-1`. Left over to you,
  since it depends on how your PDF is paginated.
- **This was written against the decompiled APK.** Public method/field names and signatures
  were read directly from the shipped classes and are reliable; a few private method *bodies*
  (`onLayout3`, `TOCFragment.search`) were not recoverable but are **not touched** by the
  default feature. Review each patch against your real source before building.

## Building (reminder)

This environment has no Android SDK and no signing key, so the APK can't be built here.
In Android Studio (or Gradle): `./gradlew assembleRelease`, then sign with the **same
keystore** used for the published APK so the new build installs as an update. If you can
share the source project (push it to this repo), I can finish the integration end-to-end
and resolve anything that differs from the decompiled view.
