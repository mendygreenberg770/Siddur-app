# tablet-mode

Ready-to-apply **tablet layout** feature for the Siddur app: on tablets (sw600dp+) the
reader offers a **two-page spread** and an **outline-on-the-side** layout, with a toolbar
button to toggle between them. Phones are unchanged.

Start with **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)**.

Written against the decompiled `Siddur.apk` (the repo has no source project). Copy these
files into the real Android Studio project, apply the small patches in `patches/`, then
build and sign as usual.
