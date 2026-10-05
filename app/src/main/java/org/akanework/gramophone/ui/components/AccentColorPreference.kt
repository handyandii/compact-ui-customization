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
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.view.ContextThemeWrapper
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.AccentColors

/**
 * "Color theme" setting: shows the current color as a swatch and opens a menu of color swatches
 * to pick from. The value is a key from [AccentColors] (or [AccentColors.DEVICE_DEFAULT]).
 */
class AccentColorPreference(context: Context, attrs: AttributeSet?) : Preference(context, attrs) {

    private class Entry(val key: String, @StringRes val title: Int, @ColorInt val color: Int)

    init {
        summaryProvider = SummaryProvider<AccentColorPreference> {
            it.context.getString(it.entryFor(it.value).title)
        }
    }

    var value: String
        get() = getPersistedString(AccentColors.DEVICE_DEFAULT) ?: AccentColors.DEVICE_DEFAULT
        set(newValue) {
            if (newValue != value && callChangeListener(newValue)) {
                persistString(newValue)
                notifyChanged()
            }
        }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    private fun entries(): List<Entry> =
        listOf(Entry(AccentColors.DEVICE_DEFAULT, R.string.accent_color_system, deviceDefaultColor())) +
                AccentColors.presets.map { Entry(it.key, it.title, it.seed) }

    private fun entryFor(key: String): Entry = entries().firstOrNull { it.key == key } ?: entries().first()

    // Primary color of the plain app theme, i.e. without a preset applied on top.
    private fun deviceDefaultColor(): Int = MaterialColors.getColor(
        ContextThemeWrapper(context, R.style.Theme_Gramophone),
        androidx.appcompat.R.attr.colorPrimary,
        Color.GRAY
    )

    private fun setSwatchColor(swatch: ImageView, @ColorInt color: Int) {
        (swatch.drawable.mutate() as GradientDrawable).setColor(color)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(R.id.accent_swatch) as ImageView?)?.let {
            setSwatchColor(it, entryFor(value).color)
        }
    }

    override fun onClick() {
        val entries = entries()
        val current = value
        val adapter = object : BaseAdapter() {
            override fun getCount() = entries.size
            override fun getItem(position: Int) = entries[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_accent_color, parent, false)
                val entry = entries[position]
                setSwatchColor(view.findViewById(R.id.accent_swatch), entry.color)
                view.findViewById<TextView>(R.id.accent_name).setText(entry.title)
                view.findViewById<View>(R.id.accent_check).visibility =
                    if (entry.key == current) View.VISIBLE else View.INVISIBLE
                return view
            }
        }
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.settings_accent_color)
            .setAdapter(adapter) { _, which -> value = entries[which].key }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
