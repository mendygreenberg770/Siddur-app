package org.chabad.kehossiddur

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Central place for everything "tablet mode". Keeps the rest of the app from
 * sprinkling `resources.getBoolean(...)` and SharedPreferences string keys around.
 *
 * Two layouts are offered on tablets, and the user flips between them with the
 * toolbar toggle added in menu_main.xml:
 *
 *  - [ViewMode.SPREAD]  : two reading columns side by side (an open-book spread).
 *                         Left column shows page N, right column shows page N+1.
 *  - [ViewMode.OUTLINE] : the table-of-contents pinned to the side, with a single
 *                         reading column next to it (master / detail).
 *
 * Phones never see either: [isTablet] is false below sw600dp, so callers fall
 * straight back to the existing single-column reader.
 */
object TabletMode {

    private const val PREF_VIEW_MODE = "tabletViewMode"

    enum class ViewMode {
        SPREAD,
        OUTLINE;

        companion object {
            fun from(name: String?): ViewMode =
                entries.firstOrNull { it.name == name } ?: SPREAD
        }
    }

    /** True on sw600dp+ devices (see res/values-sw600dp/bools.xml). */
    fun isTablet(context: Context): Boolean =
        context.resources.getBoolean(R.bool.isTablet)

    fun getViewMode(context: Context): ViewMode =
        ViewMode.from(
            PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_VIEW_MODE, ViewMode.SPREAD.name)
        )

    fun setViewMode(context: Context, mode: ViewMode) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PREF_VIEW_MODE, mode.name)
            .apply()
    }

    /** Flip SPREAD <-> OUTLINE and return the new value. */
    fun toggleViewMode(context: Context): ViewMode {
        val next = when (getViewMode(context)) {
            ViewMode.SPREAD -> ViewMode.OUTLINE
            ViewMode.OUTLINE -> ViewMode.SPREAD
        }
        setViewMode(context, next)
        return next
    }
}
