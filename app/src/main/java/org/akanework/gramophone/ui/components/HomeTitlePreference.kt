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
import android.content.SharedPreferences
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AlertDialog
import androidx.core.app.DialogCompat
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getStringStrict

/**
 * "Home screen title" setting: the text shown as the big title on the home screen, instead of the
 * app name. The value is the text; empty means the app name. Read by [HomeMenu].
 */
class HomeTitlePreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    companion object {
        const val PREF_KEY = "home_title"

        /** The title to show: the user's text, or the app name if none is set. */
        fun title(context: Context, prefs: SharedPreferences): String =
            prefs.getStringStrict(PREF_KEY, "")?.trim()?.takeIf { it.isNotEmpty() }
                ?: context.getString(R.string.fork_app_name)
    }

    init {
        summaryProvider = SummaryProvider<HomeTitlePreference> {
            it.value.ifEmpty { context.getString(R.string.fork_app_name) }
        }
    }

    var value: String
        get() = getPersistedString("")?.trim() ?: ""
        set(newValue) {
            val trimmed = newValue.trim()
            if (trimmed != value && callChangeListener(trimmed)) {
                persistString(trimmed)
                notifyChanged()
            }
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    override fun onClick() {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_home_title)
            .setView(R.layout.dialog_home_title)
            .setPositiveButton(android.R.string.ok) { d, _ ->
                value = DialogCompat.requireViewById(d as AlertDialog, R.id.editText)
                    .let { it as TextInputEditText }.editableText.toString()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .setNeutralButton(R.string.controller_button_default) { _, _ -> value = "" }
            .show()
        val editText = DialogCompat.requireViewById(dialog, R.id.editText) as TextInputEditText
        editText.hint = context.getString(R.string.fork_app_name)
        editText.setText(value)
        editText.setSelection(editText.length())
        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId != EditorInfo.IME_ACTION_DONE) return@setOnEditorActionListener false
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            true
        }
        editText.requestFocus()
    }
}
