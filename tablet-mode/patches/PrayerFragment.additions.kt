/*
 * ============================================================================
 *  PrayerFragment.kt  —  tablet-mode additions (apply to your real Kotlin source)
 * ============================================================================
 *
 * This is NOT a drop-in replacement file. It shows the exact pieces to add to your
 * existing PrayerFragment.kt. Each block names the method it belongs in. All of it
 * is gated on TabletMode.isTablet(...), so phone behaviour is byte-for-byte unchanged.
 *
 * Summary of what it wires up:
 *   - SPREAD  mode: opens the second column (R.id.pdfview_right) via SpreadController
 *                   and keeps it one page ahead of the left column.
 *   - OUTLINE mode: shows the side panel (R.id.toc_side_panel) and hosts a TOCFragment
 *                   in it; tapping an entry calls MainActivity.showPrayer() exactly as
 *                   the drawer TOC already does.
 *   - Toolbar toggle (R.id.tablet_view_toggle): flips the mode and re-opens the
 *                   current prayer so the fragment is rebuilt in the new layout.
 */

// ---------------------------------------------------------------------------
// 1) NEW FIELDS  (add alongside `private var pdfView: PDFView? = null`)
// ---------------------------------------------------------------------------
private var pdfViewRight: PDFView? = null
private var spread: SpreadController? = null


// ---------------------------------------------------------------------------
// 2) onViewCreated(...)  — add at the END, after the existing
//    `pdfView.openFile(string, integerArrayList, i, requireArguments().getInt(PARAM_SCROLLY))`
//    block has run and `string` / `integerArrayList` / `i` are still in scope.
//
//    Replace the single line:
//        pdfView.setOnShowPageListener(this)
//    with the tablet-aware wiring below (it still installs `this` as the listener
//    on phones).
// ---------------------------------------------------------------------------
private fun setUpTabletLayout(
    view: View,
    path: String,
    pages: ArrayList<Int>,
    startPage: Int,
    scrollY: Int
) {
    val left = pdfView ?: return

    if (!TabletMode.isTablet(requireContext())) {
        left.setOnShowPageListener(this)      // phones: unchanged
        return
    }

    val tocPanel = view.findViewById<View?>(R.id.toc_side_panel)
    val tocDivider = view.findViewById<View?>(R.id.toc_divider)
    pdfViewRight = view.findViewById(R.id.pdfview_right)

    when (TabletMode.getViewMode(requireContext())) {
        TabletMode.ViewMode.SPREAD -> {
            tocPanel?.visibility = View.GONE
            tocDivider?.visibility = View.GONE
            val right = pdfViewRight
            if (right != null) {
                right.visibility = View.VISIBLE
                val controller = SpreadController(left, right)
                controller.open(path, pages, startPage, scrollY)
                spread = controller
                // left column still owns title/alerts; we just piggy-back on it.
                left.setOnShowPageListener(controller.wrap(this))
            } else {
                left.setOnShowPageListener(this)
            }
        }

        TabletMode.ViewMode.OUTLINE -> {
            pdfViewRight?.visibility = View.GONE
            tocPanel?.visibility = View.VISIBLE
            tocDivider?.visibility = View.VISIBLE
            left.setOnShowPageListener(this)
            // Host the existing TOC in the side panel. Its item clicks already call
            // MainActivity.showPrayer(), which rebuilds this fragment (side panel and
            // all) with the newly chosen prayer — so no extra navigation code needed.
            if (childFragmentManager.findFragmentById(R.id.toc_side_panel) == null) {
                childFragmentManager.beginTransaction()
                    .replace(R.id.toc_side_panel, TOCFragment())
                    .commit()
            }
        }
    }
}
//    ...and at the call site in onViewCreated, instead of `pdfView.setOnShowPageListener(this)`:
//        setUpTabletLayout(view, string, integerArrayList, i,
//                          requireArguments().getInt(PARAM_SCROLLY))


// ---------------------------------------------------------------------------
// 3) onCreateOptionsMenu(menu, inflater) — add AFTER `inflater.inflate(R.menu.menu_main, menu)`
// ---------------------------------------------------------------------------
if (TabletMode.isTablet(requireContext())) {
    menu.findItem(R.id.tablet_view_toggle)?.apply {
        isVisible = true
        // Icon shows the mode you are currently IN.
        setIcon(
            when (TabletMode.getViewMode(requireContext())) {
                TabletMode.ViewMode.SPREAD -> R.drawable.ic_view_spread
                TabletMode.ViewMode.OUTLINE -> R.drawable.ic_view_outline
            }
        )
    }
}


// ---------------------------------------------------------------------------
// 4) onOptionsItemSelected(item) — add this branch at the TOP of the method
// ---------------------------------------------------------------------------
if (item.itemId == R.id.tablet_view_toggle) {
    TabletMode.toggleViewMode(requireContext())
    reopenCurrentPrayerInNewMode()
    return true
}

// Helper — mirrors the existing language-toggle re-open path. Rebuilds the prayer
// fragment so it picks up the new view mode in onViewCreated.
private fun reopenCurrentPrayerInNewMode() {
    val view = pdfView ?: return
    val mainActivity = requireActivity() as MainActivity
    val scrollY = view.ScrollerLastY()
    val index = view.displayedViewIndex
    val pages = view.pages ?: return
    @Suppress("UNCHECKED_CAST")
    val alerts = (requireArguments().getSerializable(PARAM_ALERTS)
        as? HashMap<Int, Array<String>>) ?: HashMap()
    mainActivity.showPrayer(
        pages,
        index,
        titles ?: HashMap(),
        subtitles ?: HashMap(),
        alerts,
        scrollY,
        ""
    )
}


// ---------------------------------------------------------------------------
// 5) onDestroy() — release the right column's bitmaps too (next to the existing
//    pdfView.applyToChildren { releaseBitmaps() } block).
// ---------------------------------------------------------------------------
pdfViewRight?.applyToChildren(ViewMapper { v ->
    if (v is MuPDFView) v.releaseBitmaps()
})
spread = null
pdfViewRight = null
