package org.chabad.kehossiddur

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.util.concurrent.Executors

/**
 * Tablet "page-flip spread": two PDF pages shown side by side (an open-book spread),
 * swipe SIDEWAYS to go to the next spread — like flipping pages / swiping a gallery.
 *
 * This is the proper source-level implementation of the feature that cannot be done
 * reliably by patching the compiled APK. It is self-contained: it renders with the
 * framework's [PdfRenderer] straight from the bundled asset PDF, so it does not depend
 * on the app's MuPDF internals. (If you prefer to render with MuPDF for identical
 * fidelity, swap [renderPage] to use the existing MuPDFCore.)
 *
 * Wire it up in PrayerFragment's tablet SPREAD branch:
 *
 *     val spread = TabletSpreadView(requireContext())
 *     spread.load(assetPdfPath, pagesList, startPageIndex)
 *     readerContainer.removeAllViews()
 *     readerContainer.addView(
 *         spread,
 *         ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
 *     )
 *
 * Right-to-left (Hebrew): call [setRtl] (or set layoutDirection = RTL) so the later
 * page sits on the left, matching how a siddur opens.
 */
class TabletSpreadView(context: Context) : ViewPager2(context) {

    private val io = Executors.newSingleThreadExecutor()
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null

    fun setRtl(rtl: Boolean) {
        layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
    }

    /**
     * @param assetPath  assets-relative path of the prayer's PDF (same value passed to PDFView.openFile)
     * @param pages      the prayer's page numbers (1-based), in reading order
     * @param startPageIndex index into [pages] of the page to open on
     */
    fun load(assetPath: String, pages: List<Int>, startPageIndex: Int) {
        orientation = ORIENTATION_HORIZONTAL
        offscreenPageLimit = 1

        // PdfRenderer needs a seekable file descriptor, so copy the asset to cache once.
        val file = File(context.cacheDir, "siddur_render_${assetPath.hashCode()}.pdf")
        if (!file.exists() || file.length() == 0L) {
            context.assets.open(assetPath).use { input ->
                file.outputStream().use { out -> input.copyTo(out) }
            }
        }
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd = descriptor
        val r = PdfRenderer(descriptor)
        renderer = r

        val spreads = pages.chunked(2)          // [ [p0,p1], [p2,p3], ... ]
        adapter = SpreadAdapter(r, spreads)
        setCurrentItem((startPageIndex / 2).coerceIn(0, (spreads.size - 1).coerceAtLeast(0)), false)
    }

    fun release() {
        try { renderer?.close() } catch (_: Throwable) {}
        try { pfd?.close() } catch (_: Throwable) {}
        io.shutdownNow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    private inner class SpreadAdapter(
        private val renderer: PdfRenderer,
        private val spreads: List<List<Int>>
    ) : RecyclerView.Adapter<SpreadVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpreadVH {
            val row = LinearLayout(parent.context).apply {
                orientation = LinearLayout.HORIZONTAL
                // ViewPager2 requires each page's root to be MATCH_PARENT x MATCH_PARENT.
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            val left = pageImageView(parent.context)
            val right = pageImageView(parent.context)
            row.addView(left)
            row.addView(right)
            return SpreadVH(row, left, right)
        }

        override fun getItemCount() = spreads.size

        override fun onBindViewHolder(holder: SpreadVH, position: Int) {
            val pair = spreads[position]
            bindPage(holder.left, pair.getOrNull(0))
            bindPage(holder.right, pair.getOrNull(1))
        }

        private fun bindPage(iv: ImageView, pageNumber1Based: Int?) {
            iv.setImageBitmap(null)
            if (pageNumber1Based == null) {
                iv.visibility = View.INVISIBLE
                return
            }
            iv.visibility = View.VISIBLE
            val index = pageNumber1Based - 1
            val token = index
            iv.tag = token
            io.execute {
                val bmp = renderPage(index) ?: return@execute
                iv.post { if (iv.tag == token) iv.setImageBitmap(bmp) }
            }
        }

        @Synchronized
        private fun renderPage(index: Int): Bitmap? {
            if (index < 0 || index >= renderer.pageCount) return null
            renderer.openPage(index).use { page ->
                val targetW = 1400
                val scale = targetW.toFloat() / page.width
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(targetW, h, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(Color.WHITE)   // PDFs render with transparency; fill white first
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bmp
            }
        }

        private fun pageImageView(ctx: Context) = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
    }

    private inner class SpreadVH(row: View, val left: ImageView, val right: ImageView) :
        RecyclerView.ViewHolder(row)
}
