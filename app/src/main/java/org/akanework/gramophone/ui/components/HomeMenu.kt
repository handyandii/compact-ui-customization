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

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.children
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.MaterialColors
import com.google.android.material.tabs.TabLayout
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.enableEdgeToEdgePaddingListener
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.ui.adapters.ViewPager2Adapter
import org.akanework.gramophone.ui.fragments.ViewPagerFragment

/**
 * Home menu: an alternative to the library tabs that is easier to use with a game controller.
 * It shows big shortcut tiles (Artists, Albums, Songs, Playlists); picking one opens that
 * category's usual list (the same page the tab would show) with the tabs hidden, and Back (or
 * the controller's B button, which Android maps to Back) returns to the tiles.
 *
 * The "home_menu" setting picks whether the app starts with it; the home screen's overflow menu
 * and the controller's switch layout button ([toggle]) switch for the current session only,
 * like the compact player's default setting and switch button. Lives inside ViewPagerFragment and covers its ViewPager2, so all the regular list
 * fragments (which need ViewPagerFragment as their parent) are reused unchanged.
 */
class HomeMenu(
    private val fragment: ViewPagerFragment,
    rootView: View,
    private val viewPager2: ViewPager2,
    private val adapter: ViewPager2Adapter,
    savedInstanceState: Bundle?
) : SharedPreferences.OnSharedPreferenceChangeListener, DefaultLifecycleObserver {

    companion object {
        const val PREF_KEY = "home_menu"
        private const val STATE_KEY = "home_menu_shown"
        private const val STATE_ENABLED_KEY = "home_menu_enabled"
    }

    private enum class Shortcut(
        @IdRes val tileId: Int,
        @IdRes val tabId: Int,
        @DrawableRes val icon: Int,
        @StringRes val label: Int
    ) {
        ARTISTS(R.id.home_menu_artists, R.id.artists, R.drawable.ic_groups,
            R.string.category_artists),
        ALBUMS(R.id.home_menu_albums, R.id.albums, R.drawable.ic_album,
            R.string.category_albums),
        SONGS(R.id.home_menu_songs, R.id.songs, R.drawable.ic_music_note,
            R.string.category_songs),
        PLAYLISTS(R.id.home_menu_playlists, R.id.playlists, R.drawable.ic_playlist_play,
            R.string.category_playlists)
    }

    private val context = rootView.context
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    private val menuView: View = rootView.findViewById(R.id.home_menu)
    private val grid: LinearLayout = rootView.findViewById(R.id.home_menu_grid)
    private val rows = listOf<LinearLayout>(
        rootView.findViewById(R.id.home_menu_row_1),
        rootView.findViewById(R.id.home_menu_row_2)
    )
    private val tabLayout: TabLayout = rootView.findViewById(R.id.tab_layout)
    private val toolbar: MaterialToolbar = rootView.findViewById(R.id.topAppBar)
    private val collapsingToolbar: CollapsingToolbarLayout =
        rootView.findViewById(R.id.collapsingtoolbar)
    private val appBarLayout: AppBarLayout = rootView.findViewById(R.id.appbarlayout)
    private val tiles = Shortcut.entries.associateWith { rootView.findViewById<View>(it.tileId) }

    /** Whether the home menu interface is on (as opposed to the regular tabs). */
    private var enabled = savedInstanceState?.getBoolean(STATE_ENABLED_KEY)
        ?: prefs.getBooleanStrict(PREF_KEY, false)

    /** In home menu mode: whether the tiles are showing, rather than one category's list. */
    private var menuShown = savedInstanceState?.getBoolean(STATE_KEY, true) ?: true

    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            menuShown = true
            update(requestFocus = true)
        }
    }

    init {
        tiles.forEach { (shortcut, tile) ->
            tile.findViewById<ImageView>(R.id.home_menu_tile_icon).setImageResource(shortcut.icon)
            tile.findViewById<TextView>(R.id.home_menu_tile_label).setText(shortcut.label)
            tile.contentDescription = context.getString(shortcut.label)
            tile.setOnClickListener { open(shortcut) }
            // Tiles show focus themselves (styleTile), more strongly than FocusHighlight would.
            tile.setTag(R.id.focus_highlight_skip, true)
            tile.setOnFocusChangeListener { v, hasFocus -> styleTile(v, hasFocus) }
            styleTile(tile, false)
        }
        menuView.enableEdgeToEdgePaddingListener()
        // 4 x 1 on wide screens, 2 x 2 on tall ones.
        menuView.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, or, ob ->
            if (r - l != or - ol || b - t != ob - ot) menuView.post { updateGrid() }
        }
        toolbar.setNavigationOnClickListener { backCallback.handleOnBackPressed() }
        // Keeps the title right when ViewPager2 restores its page after recreation.
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (enabled && !menuShown) update(requestFocus = false)
            }
        })
        fragment.requireActivity().onBackPressedDispatcher
            .addCallback(fragment.viewLifecycleOwner, backCallback)
        prefs.registerOnSharedPreferenceChangeListener(this)
        fragment.viewLifecycleOwner.lifecycle.addObserver(this)
        update(requestFocus = true)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == PREF_KEY) {
            enabled = prefs.getBooleanStrict(PREF_KEY, false)
            menuShown = true
            update(requestFocus = true)
        } else if (key == HomeTitlePreference.PREF_KEY) {
            update(requestFocus = false)
        } else if (key == "tabs") {
            // Posted so it runs after ViewPager2Adapter has applied the new tab list.
            menuView.post { update(requestFocus = false) }
        }
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY, menuShown)
        outState.putBoolean(STATE_ENABLED_KEY, enabled)
    }

    /** Call from the fragment's onHiddenChanged, so Back goes to the right place. */
    fun onHiddenChanged() {
        updateBackCallback()
    }

    /** Switches between the home menu and the tabs, without changing the default setting. */
    fun toggle() {
        enabled = !enabled
        menuShown = true
        update(requestFocus = true)
    }

    private fun open(shortcut: Shortcut) {
        val position = adapter.positionOf(shortcut.tabId)
        if (position < 0) return
        viewPager2.setCurrentItem(position, false)
        menuShown = false
        update(requestFocus = false)
        viewPager2.requestFocus()
    }

    private fun update(requestFocus: Boolean) {
        val enabled = enabled
        val showTiles = enabled && menuShown
        // Only offer categories that have a tab; hidden tabs have no page to show.
        tiles.forEach { (shortcut, tile) ->
            tile.visibility = if (adapter.positionOf(shortcut.tabId) >= 0) View.VISIBLE else View.GONE
        }
        rows.forEach { row ->
            row.visibility = if (row.children.any { it.visibility == View.VISIBLE })
                View.VISIBLE else View.GONE
        }
        // So the first D-pad press lands on a tile even if nothing has focus yet (touch mode).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val first = firstVisibleTile()
            tiles.values.forEach { it.isFocusedByDefault = it == first }
        }
        menuView.visibility = if (showTiles) View.VISIBLE else View.GONE
        // INVISIBLE rather than GONE, so the lists keep loading and keep their scroll position.
        viewPager2.visibility = if (showTiles) View.INVISIBLE else View.VISIBLE
        val multipleTabs = adapter.itemCount >= 2
        tabLayout.visibility = if (!enabled && multipleTabs) View.VISIBLE else View.GONE
        viewPager2.isUserInputEnabled = !enabled && multipleTabs
        if (enabled && !showTiles) {
            collapsingToolbar.title =
                context.getString(adapter.getLabelResId(viewPager2.currentItem))
            toolbar.navigationIcon =
                AppCompatResources.getDrawable(context, R.drawable.ic_arrow_back)
            toolbar.setNavigationContentDescription(R.string.home_menu_back)
        } else {
            collapsingToolbar.title = HomeTitlePreference.title(context, prefs)
            toolbar.navigationIcon = null
        }
        toolbar.menu.findItem(R.id.home_menu_toggle)?.setTitle(
            if (enabled) R.string.home_menu_use_tabs else R.string.home_menu_use_menu
        )
        if (showTiles) {
            // Collapse the big title, it takes too much room from the tiles on small screens.
            appBarLayout.setExpanded(false, false)
            if (requestFocus) firstVisibleTile()?.requestFocus()
        }
        updateBackCallback()
    }

    private fun updateBackCallback() {
        backCallback.isEnabled = enabled && !menuShown && !fragment.isHidden
    }

    /** The focused tile is filled with the primary color, outlined and slightly enlarged. */
    private fun styleTile(tile: View, focused: Boolean) {
        tile.background = AppCompatResources.getDrawable(context,
            if (focused) R.drawable.home_menu_tile_background_focused
            else R.drawable.home_menu_tile_background)
        val contentColor = MaterialColors.getColor(tile,
            if (focused) com.google.android.material.R.attr.colorOnPrimary
            else com.google.android.material.R.attr.colorOnSecondaryContainer)
        tile.findViewById<ImageView>(R.id.home_menu_tile_icon).imageTintList =
            ColorStateList.valueOf(contentColor)
        tile.findViewById<TextView>(R.id.home_menu_tile_label).setTextColor(contentColor)
        val scale = if (focused) 1.05f else 1f
        tile.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
    }

    private fun firstVisibleTile() = tiles.values.firstOrNull { it.visibility == View.VISIBLE }

    private fun updateGrid() {
        val wide = menuView.width > menuView.height
        val orientation = if (wide) LinearLayout.HORIZONTAL else LinearLayout.VERTICAL
        if (grid.orientation == orientation) return
        grid.orientation = orientation
        rows.forEach { row ->
            row.layoutParams = (row.layoutParams as LinearLayout.LayoutParams).apply {
                width = if (wide) 0 else LinearLayout.LayoutParams.MATCH_PARENT
                height = if (wide) LinearLayout.LayoutParams.MATCH_PARENT else 0
            }
        }
    }
}
