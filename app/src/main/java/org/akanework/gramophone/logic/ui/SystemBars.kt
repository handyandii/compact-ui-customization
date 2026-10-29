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
import android.content.SharedPreferences
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.akanework.gramophone.logic.getBooleanStrict

/**
 * "Hide navigation bar" setting: hides Android's navigation bar (back/home/recents) in immersive
 * mode, for more screen space on small handhelds. A swipe from the screen edge shows it for a
 * moment. Applied by BaseActivity to every activity, and again whenever the window regains focus,
 * since dialogs and the system can bring the bar back.
 */
object SystemBars {
    const val PREF_KEY = "hide_navigation_bar"

    fun apply(activity: Activity, prefs: SharedPreferences) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (prefs.getBooleanStrict(PREF_KEY, false)) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
