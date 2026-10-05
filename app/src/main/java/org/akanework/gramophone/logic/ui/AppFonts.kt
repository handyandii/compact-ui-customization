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
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.widget.TextView
import androidx.annotation.FontRes
import androidx.annotation.StringRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import androidx.preference.PreferenceManager
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getStringStrict
import java.io.File

/**
 * App wide font ("Font" in appearance settings). Either a font bundled in res/font, or a font
 * file the user imported (copied into filesDir/fonts). Unlike [AccentColors] this isn't a theme
 * overlay, because a theme can't point at a file on disk. Instead [ViewCompatInflater] puts
 * [typeface] on every inflated TextView, keeping that view's weight and italic style.
 */
object AppFonts {
    private const val TAG = "AppFonts"
    const val PREF_KEY = "app_font"
    const val DEVICE_DEFAULT = "system"
    private const val IMPORTED_PREFIX = "file:"
    private const val FONT_DIR = "fonts"
    private const val MAX_IMPORT_SIZE = 32L * 1024 * 1024

    class Preset(val key: String, @StringRes val title: Int, @FontRes val font: Int)

    // Fonts and their OFL license files are listed in misc/font_licenses.
    val presets = listOf(
        Preset("atkinson_hyperlegible", R.string.app_font_atkinson_hyperlegible, R.font.font_atkinson_hyperlegible),
        Preset("nunito", R.string.app_font_nunito, R.font.font_nunito),
        Preset("space_grotesk", R.string.app_font_space_grotesk, R.font.font_space_grotesk),
        Preset("jetbrains_mono", R.string.app_font_jetbrains_mono, R.font.font_jetbrains_mono),
        Preset("pixelify_sans", R.string.app_font_pixelify_sans, R.font.font_pixelify_sans),
        Preset("vt323", R.string.app_font_vt323, R.font.font_vt323),
    )

    @Volatile
    private var cache: Pair<String, Typeface?>? = null

    fun selectedKey(context: Context): String =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
            .getStringStrict(PREF_KEY, DEVICE_DEFAULT) ?: DEVICE_DEFAULT

    /** The selected font, or null for the device default font (also when it can't be loaded). */
    fun typeface(context: Context): Typeface? {
        val key = selectedKey(context)
        cache?.let { if (it.first == key) return it.second }
        return load(context, key).also { cache = key to it }
    }

    /** Loads the font for [key] (a preset key or an imported font key). Null = device default. */
    fun load(context: Context, key: String): Typeface? {
        if (key == DEVICE_DEFAULT) return null
        return try {
            if (key.startsWith(IMPORTED_PREFIX)) {
                readFontFile(File(fontDir(context), key.removePrefix(IMPORTED_PREFIX)))
            } else {
                presets.firstOrNull { it.key == key }?.let {
                    ResourcesCompat.getFont(context.applicationContext, it.font)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "failed to load font $key", e)
            null
        }
    }

    /** Put the selected font on [view], keeping the weight and italic style it already has. */
    fun applyTo(view: TextView) {
        val family = typeface(view.context) ?: return
        view.typeface = withStyleOf(view.context, family, view.typeface)
    }

    /** [family] with the weight and italic style of [current] (default 400, upright). */
    fun withStyleOf(context: Context, family: Typeface, current: Typeface?): Typeface {
        val weight = when {
            current == null -> 400
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> current.weight
            current.isBold -> 700
            else -> 400
        }
        return TypefaceCompat.create(context, family, weight, current?.isItalic == true)
    }

    /**
     * Replacement for TypefaceCompat.create(context, null, weight, false): the selected font at
     * [weight], or the device default font if none is selected.
     */
    fun create(context: Context, weight: Int): Typeface =
        TypefaceCompat.create(context, typeface(context), weight, false)

    // ---- Imported fonts ----

    private fun fontDir(context: Context) = File(context.filesDir, FONT_DIR)

    fun isImported(key: String) = key.startsWith(IMPORTED_PREFIX)

    /** Keys of all imported fonts, sorted by name. */
    fun importedKeys(context: Context): List<String> =
        fontDir(context).listFiles()?.filter { it.isFile }
            ?.sortedBy { it.name.lowercase() }
            ?.map { IMPORTED_PREFIX + it.name } ?: emptyList()

    /** Name to show for an imported font: its file name without the extension. */
    fun importedName(key: String): String =
        key.removePrefix(IMPORTED_PREFIX).substringBeforeLast('.')

    /**
     * Copies the font file at [uri] into the app and returns its key, or null if it isn't a
     * readable font. Does disk IO, don't call on the main thread.
     */
    fun import(context: Context, uri: Uri): String? {
        val resolver = context.contentResolver
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?: uri.lastPathSegment?.substringAfterLast('/') ?: "font"
        val dir = fontDir(context).apply { mkdirs() }
        val tmp = File(dir, ".import.tmp")
        try {
            val input = resolver.openInputStream(uri) ?: return null
            var size = 0L
            input.use { inp ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = inp.read(buf)
                        if (n < 0) break
                        size += n
                        if (size > MAX_IMPORT_SIZE) return null
                        out.write(buf, 0, n)
                    }
                }
            }
            if (readFontFile(tmp) == null) return null
            val target = uniqueFile(dir, sanitize(displayName))
            if (!tmp.renameTo(target)) return null
            return IMPORTED_PREFIX + target.name
        } catch (e: Exception) {
            Log.w(TAG, "failed to import font $uri", e)
            return null
        } finally {
            tmp.delete()
        }
    }

    /** Deletes an imported font. The caller should switch away from it if it's selected. */
    fun delete(context: Context, key: String) {
        if (!isImported(key)) return
        File(fontDir(context), key.removePrefix(IMPORTED_PREFIX)).delete()
        cache = null
    }

    private fun readFontFile(file: File): Typeface? {
        if (!file.isFile) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Typeface.Builder(file).build() // null if the file isn't a font
        } else {
            try {
                Typeface.createFromFile(file).takeIf { it != Typeface.DEFAULT }
            } catch (e: RuntimeException) {
                null
            }
        }
    }

    private fun sanitize(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
            .takeIf { it in setOf("ttf", "otf", "ttc", "otc") } ?: "ttf"
        val base = name.substringBeforeLast('.')
            .replace(Regex("[^\\p{L}\\p{N} ._()-]"), "_").trim().trimStart('.')
            .take(64).ifEmpty { "font" }
        return "$base.$ext"
    }

    private fun uniqueFile(dir: File, name: String): File {
        var file = File(dir, name)
        var i = 2
        while (file.exists()) {
            file = File(dir, "${name.substringBeforeLast('.')} ($i).${name.substringAfterLast('.')}")
            i++
        }
        return file
    }
}
