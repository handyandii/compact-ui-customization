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
 * "Full screen" setting: hides Android's status bar and navigation bar (immersive mode), for more
 * screen space on small handhelds. A swipe from the top or bottom edge shows them for a moment.
 * Both bars, because with gesture navigation (as on the ARBOR) the navigation bar is only a thin
 * handle and the status bar is the bar that takes the space. Applied by BaseActivity to every
 * activity, and again whenever the window regains focus, since dialogs and the system can bring
 * the bars back.
 */
object SystemBars {
    const val PREF_KEY = "full_screen"

    fun apply(activity: Activity, prefs: SharedPreferences) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (prefs.getBooleanStrict(PREF_KEY, false)) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
