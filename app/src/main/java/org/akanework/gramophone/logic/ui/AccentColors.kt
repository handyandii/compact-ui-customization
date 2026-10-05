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

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatDelegate
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
        @StyleRes val overlay: Int,
        /** AppCompatDelegate night mode the preset needs, for fixed (non seed based) palettes. */
        val nightMode: Int = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED
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
        // Hand-made fixed palettes in res/values/themes_fixed_presets.xml, not from Gen.java.
        Preset(
            "black_white", R.string.accent_color_black_white, 0xFF000000.toInt(),
            R.style.ThemeOverlay_Gramophone_Accent_BlackWhite, AppCompatDelegate.MODE_NIGHT_YES
        ),
        Preset(
            "lcd", R.string.accent_color_lcd, 0xFFC4CCB3.toInt(),
            R.style.ThemeOverlay_Gramophone_Accent_Lcd, AppCompatDelegate.MODE_NIGHT_NO
        ),
        Preset(
            "dracula", R.string.accent_color_dracula, 0xFFBD93F9.toInt(),
            R.style.ThemeOverlay_Gramophone_Accent_Dracula, AppCompatDelegate.MODE_NIGHT_YES
        ),
    )

    /** The selected preset, or null for the device default colors (also for unknown values). */
    fun find(key: String?): Preset? = presets.firstOrNull { it.key == key }

    fun selected(prefs: SharedPreferences): Preset? =
        find(prefs.getStringStrict(PREF_KEY, DEVICE_DEFAULT))

    /**
     * Night mode the selected preset forces (light or dark only palettes), or
     * MODE_NIGHT_UNSPECIFIED to follow the app setting. Set it as the activity's local night
     * mode before the base context is attached.
     */
    fun nightMode(prefs: SharedPreferences): Int =
        selected(prefs)?.nightMode ?: AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

    /**
     * A player control color a theme can set (R.attr.player*Color, see
     * attrs_player_accents.xml), or null if the theme doesn't set it.
     */
    @ColorInt
    fun playerColor(context: Context, @AttrRes attr: Int): Int? {
        val value = TypedValue()
        if (!context.theme.resolveAttribute(attr, value, true)) return null
        return if (value.resourceId != 0) context.getColor(value.resourceId) else value.data
    }

    /** Repeat/shuffle button colors from the theme's player colors, or null if it sets none. */
    fun playerToggleColors(context: Context): ColorStateList? {
        val on = playerColor(context, R.attr.playerToggleOnColor) ?: return null
        val off = playerColor(context, R.attr.playerToggleOffColor) ?: return null
        return ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(on, off)
        )
    }

    /** True for the fixed palettes, which also replace the player's album art based colors. */
    fun isFixedPalette(prefs: SharedPreferences): Boolean =
        nightMode(prefs) != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED

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
