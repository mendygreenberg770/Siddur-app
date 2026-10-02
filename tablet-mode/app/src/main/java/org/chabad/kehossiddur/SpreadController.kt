package org.chabad.kehossiddur

import ru.mobigroup.bookreader.OnShowPageListener

/**
 * Drives the two-column "open book" spread on tablets.
 *
 * The app's reader ([PDFView] -> MuPDFReaderView -> ReaderView) is a *continuous
 * vertical scroller*: it stacks the prayer's pages in one tall column. For a tablet
 * spread we simply run a SECOND [PDFView] next to the first and keep it one page
 * ahead, so the user sees page N on the left and page N+1 on the right.
 *
 * This uses only the reader's public API (openFile / getDisplayedViewIndex /
 * setDisplayedViewIndex / ScrollerLastY / setOnShowPageListener), so it needs NO
 * changes to the vendored ReaderView. The left column stays the "source of truth"
 * that owns the toolbar title + alerts (via [PrayerFragment], which implements
 * [OnShowPageListener]); this controller just mirrors the left column into the right.
 *
 * Sync granularity is per-page: when the left column moves onto a new page, the
 * right column jumps to the following page. That is enough for a prayer book, where
 * each page is a discrete unit. If you want the two columns to track each other
 * pixel-for-pixel while scrolling mid-page, add the optional scroll hook described
 * in patches/ReaderView.scrollhook.md and call [syncScrollFromLeft] from it.
 */
class SpreadController(
    private val left: PDFView,
    private val right: PDFView
) {

    private var opened = false

    /**
     * Open the right-hand column. [path] and [pages] are exactly the values the
     * left column was opened with in PrayerFragment; [leftStartPage] is the index
     * the left column started on. The right column opens one page further along.
     */
    fun open(path: String, pages: ArrayList<Int>, leftStartPage: Int, scrollY: Int) {
        val rightStart = (leftStartPage + 1).coerceAtMost(pages.size - 1).coerceAtLeast(0)
        right.openFile(path, pages, rightStart, scrollY)
        right.checkHasSizes()
        opened = true
    }

    /**
     * Wrap the listener the fragment installs on the LEFT column so the fragment
     * still gets every onShowPage (title/alerts), and we also advance the right
     * column. Install the returned listener on the left PDFView instead of `this`.
     */
    fun wrap(delegate: OnShowPageListener): OnShowPageListener =
        OnShowPageListener { page ->
            delegate.onShowPage(page)
            onLeftPageChanged()
        }

    /** Advance the right column to one page after the left column's current page. */
    private fun onLeftPageChanged() {
        if (!opened) return
        val pages = left.pages ?: return
        val target = (left.displayedViewIndex + 1).coerceAtMost(pages.size - 1)
        if (target >= 0 && target != right.displayedViewIndex) {
            right.setDisplayedViewIndex(target, 0)
        }
    }

    /**
     * Optional: pixel-accurate scroll mirroring. Only works if you added the
     * one-line scroll hook from patches/ReaderView.scrollhook.md to ReaderView.
     */
    fun syncScrollFromLeft() {
        if (!opened) return
        right.setDisplayedViewIndex(
            (left.displayedViewIndex + 1).coerceAtMost((left.pages?.size ?: 1) - 1),
            left.ScrollerLastY()
        )
    }
}
