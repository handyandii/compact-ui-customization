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
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.ControllerButtons
import org.akanework.gramophone.logic.ui.ControllerButtons.Action

/**
 * One controller action (play/pause, previous, next) in settings, showing the button bound to it.
 * Clicking it opens a dialog: "Set" removes the current button and waits for the next button
 * press, which then takes over the action (and is taken away from any other action using it).
 * The preference key must be one of the [Action.prefKey]s.
 */
class ControllerButtonPreference(context: Context, attrs: AttributeSet?) :
    Preference(context, attrs) {

    private val action by lazy {
        requireNotNull(Action.forPrefKey(key)) { "unknown controller action $key" }
    }

    init {
        widgetLayoutResource = R.layout.preference_controller_edit
        summaryProvider = SummaryProvider<ControllerButtonPreference> {
            it.buttonName(it.keyCode)
        }
    }

    private val keyCode: Int
        get() = getPersistedInt(action.defaultKeyCode)

    private fun buttonName(keyCode: Int): String =
        ControllerButtons.keyName(keyCode) ?: context.getString(R.string.controller_button_not_set)

    fun refresh() = notifyChanged()

    override fun onClick() {
        val title = context.getString(action.title)
        var waiting = false
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(currentMessage())
            .setPositiveButton(R.string.controller_button_set, null)
            .setNeutralButton(R.string.controller_button_default) { _, _ ->
                assign(action.defaultKeyCode)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        dialog.setOnKeyListener { d, keyCode, event ->
            when {
                !waiting -> false
                // Back cancels, other system keys (volume...) keep working.
                !ControllerButtons.isBindable(keyCode) -> false
                event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 -> {
                    assign(keyCode)
                    d.dismiss()
                    true
                }
                else -> true
            }
        }
        dialog.setOnShowListener {
            // Replace the default click handling so "Set" doesn't close the dialog.
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                waiting = true
                persistInt(ControllerButtons.UNSET)
                notifyChanged()
                dialog.setMessage(context.getString(R.string.controller_button_waiting, title))
                dialog.getButton(DialogInterface.BUTTON_POSITIVE).visibility = View.GONE
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).visibility = View.GONE
                // Keep D-pad/A presses from landing on the remaining Cancel button.
                dialog.getButton(DialogInterface.BUTTON_NEGATIVE).isFocusable = false
                dialog.window?.decorView?.requestFocus()
            }
        }
        dialog.show()
    }

    private fun currentMessage(): String = ControllerButtons.keyName(keyCode)?.let {
        context.getString(R.string.controller_button_current, it)
    } ?: context.getString(R.string.controller_button_none)

    /** Bind [newKeyCode] to this action, taking it away from any other action that has it. */
    private fun assign(newKeyCode: Int) {
        val prefs = sharedPreferences ?: return
        for (other in Action.entries) {
            if (other == action || other.keyCode(prefs) != newKeyCode) continue
            prefs.edit { putInt(other.prefKey, ControllerButtons.UNSET) }
            (preferenceManager?.findPreference<Preference>(other.prefKey)
                    as? ControllerButtonPreference)?.refresh()
            Toast.makeText(
                context, context.getString(
                    R.string.controller_button_moved, ControllerButtons.keyName(newKeyCode),
                    context.getString(other.title)
                ), Toast.LENGTH_LONG
            ).show()
        }
        persistInt(newKeyCode)
        notifyChanged()
    }
}
