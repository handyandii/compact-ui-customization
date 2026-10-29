/*
 *     Copyright (C) 2026 The Gramophone contributors
 *
 *     Gramophone is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Gramophone is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.gramophone.logic.ui

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx

/**
 * Draws an outline on whichever view has D-pad/keyboard focus, so controller navigation can be
 * seen. Most of the app's views (library rows, mini player, toolbar buttons, settings rows) have
 * no visible focused state of their own. Installed for every activity by BaseActivity. Only shown outside touch mode, i.e. after a D-pad or key press, so
 * touch use looks unchanged. Drawn in the focused view's overlay, so no layouts need changing.
 *
 * Views that draw their own focus state can opt out by setting the [R.id.focus_highlight_skip]
 * tag to true.
 */
object FocusHighlight {

    fun install(activity: Activity) {
        val decorView = activity.window.decorView
        val color = MaterialColors.getColor(
            activity, com.google.android.material.R.attr.colorOnSurface, 0
        )
        val inset = 2.dpToPx(activity)
        val outline = GradientDrawable().apply {
            setStroke(3.dpToPx(activity), color)
            setColor(ColorUtils.setAlphaComponent(color, 0x1F))
            cornerRadius = 12.dpToPx(activity).toFloat()
        }
        var current: View? = null
        val resizeListener = View.OnLayoutChangeListener { _, l, t, r, b, _, _, _, _ ->
            outline.setBounds(inset, inset, r - l - inset, b - t - inset)
        }

        fun show(view: View?) {
            current?.let {
                it.overlay.remove(outline)
                it.removeOnLayoutChangeListener(resizeListener)
            }
            current = null
            if (view == null || view.isInTouchMode
                || view.getTag(R.id.focus_highlight_skip) == true
            ) return
            outline.setBounds(inset, inset, view.width - inset, view.height - inset)
            view.overlay.add(outline)
            view.addOnLayoutChangeListener(resizeListener)
            current = view
        }

        decorView.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus -> show(newFocus) }
        // The first D-pad press leaves touch mode without moving focus; a tap re-enters it.
        decorView.viewTreeObserver.addOnTouchModeChangeListener { inTouchMode ->
            show(if (inTouchMode) null else decorView.findFocus())
        }
    }
}
