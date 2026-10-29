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
import android.view.KeyEvent
import androidx.annotation.StringRes
import androidx.media3.common.Player
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.getIntStrict
import org.akanework.gramophone.logic.playOrPause

/**
 * Game controller buttons (for retro handhelds). Each [Action] is bound to one key code,
 * changeable in settings (ControllerButtonPreference); by default Start plays/pauses, L1 goes to
 * the previous track, R1 to the next one, X opens/closes now playing and Select switches layout
 * (see [Host.switchLayout]). Repeat and shuffle have no button until the user sets one. Toggled by the "controller_buttons" setting. Called
 * from MainActivity.dispatchKeyEvent, so it works no matter which view has focus.
 */
object ControllerButtons {
    const val PREF_KEY = "controller_buttons"

    /** Key code value for an action without a button. */
    const val UNSET = KeyEvent.KEYCODE_UNKNOWN

    /** What the actions act on; implemented by MainActivity. */
    interface Host {
        val player: Player?

        /** Compact ↔ full player if the player is open, otherwise home menu ↔ tabs. */
        fun switchLayout()

        /** Opens the now playing screen, or closes it if it's open. */
        fun toggleNowPlaying()

        /** Short feedback for actions whose result isn't otherwise visible. */
        fun notify(@StringRes message: Int)
    }

    enum class Action(
        val prefKey: String,
        val defaultKeyCode: Int,
        @StringRes val title: Int,
        val run: Host.() -> Unit
    ) {
        PLAY_PAUSE("controller_key_play_pause", KeyEvent.KEYCODE_BUTTON_START,
            R.string.controller_action_play_pause, { player?.playOrPause() }),
        PREVIOUS("controller_key_previous", KeyEvent.KEYCODE_BUTTON_L1,
            R.string.controller_action_previous, { player?.seekToPrevious() }),
        NEXT("controller_key_next", KeyEvent.KEYCODE_BUTTON_R1,
            R.string.controller_action_next, { player?.seekToNext() }),
        NOW_PLAYING("controller_key_now_playing", KeyEvent.KEYCODE_BUTTON_X,
            R.string.controller_action_now_playing, { toggleNowPlaying() }),
        SWITCH_LAYOUT("controller_key_switch_layout", KeyEvent.KEYCODE_BUTTON_SELECT,
            R.string.controller_action_switch_layout, { switchLayout() }),
        REPEAT("controller_key_repeat", UNSET, R.string.controller_action_repeat, {
            player?.let {
                // Same order as the player's repeat button.
                it.repeatMode = when (it.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
                notify(when (it.repeatMode) {
                    Player.REPEAT_MODE_ALL -> R.string.controller_repeat_all
                    Player.REPEAT_MODE_ONE -> R.string.controller_repeat_one
                    else -> R.string.controller_repeat_off
                })
            }
        }),
        SHUFFLE("controller_key_shuffle", UNSET, R.string.controller_action_shuffle, {
            player?.let {
                it.shuffleModeEnabled = !it.shuffleModeEnabled
                notify(if (it.shuffleModeEnabled) R.string.controller_shuffle_on
                    else R.string.controller_shuffle_off)
            }
        });

        fun keyCode(prefs: SharedPreferences): Int = prefs.getIntStrict(prefKey, defaultKeyCode)

        companion object {
            fun forPrefKey(key: String?): Action? = entries.firstOrNull { it.prefKey == key }
        }
    }

    /** Keys that can't be bound, because the system or the user needs them for other things. */
    fun isBindable(keyCode: Int): Boolean = keyCode !in setOf(
        KeyEvent.KEYCODE_UNKNOWN, KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME,
        KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_APP_SWITCH, KeyEvent.KEYCODE_WAKEUP,
        KeyEvent.KEYCODE_SLEEP, KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN,
        KeyEvent.KEYCODE_VOLUME_MUTE
    )

    /** Short name of a button for the settings screen, null for [UNSET]. */
    fun keyName(keyCode: Int): String? = when (keyCode) {
        UNSET -> null
        KeyEvent.KEYCODE_BUTTON_A -> "A"
        KeyEvent.KEYCODE_BUTTON_B -> "B"
        KeyEvent.KEYCODE_BUTTON_C -> "C"
        KeyEvent.KEYCODE_BUTTON_X -> "X"
        KeyEvent.KEYCODE_BUTTON_Y -> "Y"
        KeyEvent.KEYCODE_BUTTON_Z -> "Z"
        KeyEvent.KEYCODE_BUTTON_L1 -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1 -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2 -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2 -> "R2"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "L3"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "R3"
        KeyEvent.KEYCODE_BUTTON_START -> "Start"
        KeyEvent.KEYCODE_BUTTON_SELECT -> "Select"
        KeyEvent.KEYCODE_BUTTON_MODE -> "Mode"
        else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
            .replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    /** Returns true if [event] was a bound controller button and has been handled. */
    fun handle(event: KeyEvent, prefs: SharedPreferences, host: Host): Boolean {
        if (event.keyCode == UNSET || !prefs.getBooleanStrict(PREF_KEY, true)) return false
        val action = Action.entries.firstOrNull { it.keyCode(prefs) == event.keyCode }
            ?: return false
        // Act once per press, but swallow the key up and repeats too so nothing else sees them.
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            action.run(host)
        }
        return true
    }
}
