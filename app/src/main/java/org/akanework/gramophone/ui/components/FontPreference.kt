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
import android.content.res.TypedArray
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.AppFonts

/**
 * "Font" setting: opens a menu of the built-in fonts and the user's imported fonts, each shown in
 * its own font, plus an entry to import a font file. The value is a key from [AppFonts]. The
 * hosting fragment does the file picking, see [onImportRequest].
 */
class FontPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private class Entry(val key: String, val name: String, val typeface: Typeface?)

    /** Called when the user picks "Import font file…". */
    var onImportRequest: (() -> Unit)? = null

    init {
        summaryProvider = SummaryProvider<FontPreference> { it.nameFor(it.value) }
    }

    var value: String
        get() = getPersistedString(AppFonts.DEVICE_DEFAULT) ?: AppFonts.DEVICE_DEFAULT
        set(newValue) {
            if (newValue != value && callChangeListener(newValue)) {
                persistString(newValue)
                notifyChanged()
            }
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    private fun nameFor(key: String): String = when {
        AppFonts.isImported(key) && key in AppFonts.importedKeys(context) -> AppFonts.importedName(key)
        else -> AppFonts.presets.firstOrNull { it.key == key }?.let { context.getString(it.title) }
            ?: context.getString(R.string.app_font_system)
    }

    private fun entries(): List<Entry> =
        listOf(Entry(AppFonts.DEVICE_DEFAULT, context.getString(R.string.app_font_system), null)) +
                AppFonts.presets.map {
                    Entry(it.key, context.getString(it.title), AppFonts.load(context, it.key))
                } +
                AppFonts.importedKeys(context).map {
                    Entry(it, AppFonts.importedName(it), AppFonts.load(context, it))
                }

    override fun onClick() {
        val entries = entries()
        val current = value
        lateinit var dialog: AlertDialog
        val adapter = object : BaseAdapter() {
            // The last row is "Import font file…".
            override fun getCount() = entries.size + 1
            override fun getItem(position: Int) = entries.getOrNull(position)
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_app_font, parent, false)
                val preview = view.findViewById<TextView>(R.id.font_preview)
                val icon = view.findViewById<View>(R.id.font_icon)
                val name = view.findViewById<TextView>(R.id.font_name)
                val check = view.findViewById<View>(R.id.font_check)
                val remove = view.findViewById<View>(R.id.font_remove)
                val entry = entries.getOrNull(position)
                if (entry == null) {
                    preview.visibility = View.INVISIBLE
                    icon.visibility = View.VISIBLE
                    name.setText(R.string.app_font_import)
                    name.typeface = Typeface.DEFAULT
                    check.visibility = View.GONE
                    remove.visibility = View.GONE
                    return view
                }
                // Explicitly set (also for the device default), the row was inflated in the app font.
                val typeface = entry.typeface ?: Typeface.DEFAULT
                preview.visibility = View.VISIBLE
                preview.typeface = typeface
                icon.visibility = View.GONE
                name.text = entry.name
                name.typeface = typeface
                check.visibility = if (entry.key == current) View.VISIBLE else View.INVISIBLE
                if (AppFonts.isImported(entry.key)) {
                    remove.visibility = View.VISIBLE
                    remove.setOnClickListener { confirmRemove(entry) { dialog.dismiss() } }
                } else {
                    remove.visibility = View.GONE
                    remove.setOnClickListener(null)
                }
                return view
            }
        }
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_app_font)
            .setAdapter(adapter) { _, which ->
                val entry = entries.getOrNull(which)
                if (entry == null) onImportRequest?.invoke() else value = entry.key
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmRemove(entry: Entry, onRemoved: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.app_font_remove)
            .setMessage(context.getString(R.string.app_font_remove_confirm, entry.name))
            .setPositiveButton(R.string.app_font_remove_action) { _, _ ->
                onRemoved()
                AppFonts.delete(context, entry.key)
                if (value == entry.key) {
                    value = AppFonts.DEVICE_DEFAULT // recreates the activities
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
