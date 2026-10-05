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

import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.getStringStrict

/**
 * App wide color theme presets ("Color theme" in appearance settings). Each preset is a theme
 * overlay with full Material 3 light + dark color schemes, generated ahead of time from [seed] by
 * misc/accent_color_generator/Gen.java. That keeps it independent of the device's dynamic color
 * support, so it works on every Android version and vendor.
 */
object AccentColors {
    const val PREF_KEY = "app_accent_color"
    const val DEVICE_DEFAULT = "system"

    class Preset(
        val key: String,
        @StringRes val title: Int,
        @ColorInt val seed: Int,
        @StyleRes val overlay: Int
    )

    // Keep seeds in sync with misc/accent_color_generator/Gen.java.
    val presets = listOf(
        Preset("red", R.string.accent_color_red, 0xFFE53935.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Red),
        Preset("pink", R.string.accent_color_pink, 0xFFEC407A.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Pink),
        Preset("purple", R.string.accent_color_purple, 0xFF8E24AA.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Purple),
        Preset("indigo", R.string.accent_color_indigo, 0xFF3F51B5.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Indigo),
        Preset("blue", R.string.accent_color_blue, 0xFF1E88E5.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Blue),
        Preset("teal", R.string.accent_color_teal, 0xFF00897B.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Teal),
        Preset("green", R.string.accent_color_green, 0xFF43A047.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Green),
        Preset("yellow", R.string.accent_color_yellow, 0xFFFDD835.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Yellow),
        Preset("orange", R.string.accent_color_orange, 0xFFFB8C00.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Orange),
        Preset("grey", R.string.accent_color_grey, 0xFF9E9E9E.toInt(), R.style.ThemeOverlay_Gramophone_Accent_Grey),
    )

    /** The selected preset, or null for the device default colors (also for unknown values). */
    fun find(key: String?): Preset? = presets.firstOrNull { it.key == key }

    fun selected(prefs: SharedPreferences): Preset? =
        find(prefs.getStringStrict(PREF_KEY, DEVICE_DEFAULT))

    /**
     * Apply the selected preset (if any) on top of an activity theme. Call before
     * super.onCreate(). Light/dark colors resolve from the activity's night mode.
     */
    fun applyTo(theme: Resources.Theme, resources: Resources, prefs: SharedPreferences) {
        val preset = selected(prefs) ?: return
        theme.applyStyle(preset.overlay, true)
        // The preset replaces surface colors too, so put pure dark back on top of it.
        if (prefs.getBooleanStrict("pureDark", false) &&
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        ) {
            theme.applyStyle(R.style.ThemeOverlay_PureDark, true)
        }
    }
}
