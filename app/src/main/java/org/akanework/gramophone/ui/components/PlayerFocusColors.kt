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

package org.akanework.gramophone.ui.components

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.core.view.children
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.AccentColors

/**
 * Now playing controls show D-pad focus by changing color, instead of FocusHighlight's outline:
 * a focused button is filled with the focus color (its icon in the matching "on" color), and a
 * focused seek bar's thumb takes the focus color. The colors come from the color theme's
 * playerFocusColor/playerOnFocusColor; with album art colors (or no theme) the player palette's
 * tertiary color is used, so focus always fits what the player shows.
 *
 * The player sets its tints in code (and again on every track change), so the normal tints are
 * saved when a control gains focus and put back when it loses it; call [refresh] after the player
 * recolors so a focused control picks up its new normal tints and keeps the focus look.
 */
class PlayerFocusColors(context: Context) {

    private class Saved(
        val background: ColorStateList?,
        val icon: ColorStateList?,
        val thumb: ColorStateList?,
        val halo: ColorStateList?
    )

    private var color = 0
    private var onColor = 0

    init {
        update(context, albumColors = false)
    }

    /** Picks the focus colors from [context]'s theme, see the class comment. */
    fun update(context: Context, albumColors: Boolean) {
        color = (if (albumColors) null
            else AccentColors.playerColor(context, R.attr.playerFocusColor))
            ?: MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorTertiary, 0
            )
        onColor = (if (albumColors) null
            else AccentColors.playerColor(context, R.attr.playerOnFocusColor))
            ?: MaterialColors.getColor(
                context, com.google.android.material.R.attr.colorOnTertiary, 0
            )
    }

    /** Sets up every button and seek bar under [root]. Call again after re-inflating. */
    fun attach(root: View) {
        when (root) {
            is MaterialButton, is Slider, is SeekBar -> {
                root.setTag(R.id.focus_highlight_skip, true)
                root.setOnFocusChangeListener { v, hasFocus ->
                    if (hasFocus) applyFocus(v) else restore(v)
                }
            }
            is ViewGroup -> root.children.forEach { attach(it) }
        }
    }

    /** Re-applies the focus look to the focused control under [root] after the player recolors. */
    fun refresh(root: View) {
        val focused = root.findFocus() ?: return
        if (focused.getTag(R.id.player_focus_saved) == null) return
        focused.setTag(R.id.player_focus_saved, null)
        applyFocus(focused)
    }

    private fun applyFocus(v: View) {
        val focus = ColorStateList.valueOf(color)
        val on = ColorStateList.valueOf(onColor)
        when (v) {
            is MaterialButton -> {
                save(v, Saved(v.backgroundTintList, v.iconTint, null, null))
                v.backgroundTintList = focus
                v.iconTint = on
            }
            is Slider -> {
                save(v, Saved(null, null, v.thumbTintList, v.haloTintList))
                v.thumbTintList = focus
                v.haloTintList = ColorStateList.valueOf(color and 0x00FFFFFF or 0x40000000)
            }
            is SeekBar -> {
                save(v, Saved(null, null, v.thumbTintList, null))
                v.thumbTintList = focus
            }
        }
    }

    private fun save(v: View, saved: Saved) {
        // Keep the first saved (normal) tints if focus is applied twice in a row.
        if (v.getTag(R.id.player_focus_saved) == null) v.setTag(R.id.player_focus_saved, saved)
    }

    private fun restore(v: View) {
        val saved = v.getTag(R.id.player_focus_saved) as? Saved ?: return
        v.setTag(R.id.player_focus_saved, null)
        when (v) {
            is MaterialButton -> {
                v.backgroundTintList = saved.background
                v.iconTint = saved.icon
            }
            is Slider -> {
                saved.thumb?.let { v.thumbTintList = it }
                saved.halo?.let { v.haloTintList = it }
            }
            is SeekBar -> v.thumbTintList = saved.thumb
        }
    }
}
