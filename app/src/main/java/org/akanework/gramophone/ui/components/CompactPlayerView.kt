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
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.preference.PreferenceManager
import android.net.Uri
import coil3.dispose
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.error
import coil3.size.Scale
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.slider.Slider
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.dpToPx
import org.akanework.gramophone.logic.getBooleanStrict
import org.akanework.gramophone.logic.getIntStrict
import org.akanework.gramophone.logic.getStringStrict
import org.akanework.gramophone.logic.playOrPause
import org.akanework.gramophone.logic.setTextAnimation
import org.akanework.gramophone.logic.utils.CalculationUtils
import org.akanework.gramophone.ui.MainActivity
import kotlin.math.abs
import kotlin.math.min

/**
 * Simplified now playing screen for screens that aren't tall (4:3, 3:2, 1:1).
 * It is hosted on top of [FullBottomSheet], which owns insets, background color and visibility.
 *
 * There is one layout per album art size (compact_player_large/small/hidden.xml), all using the
 * same view ids. Switching size re-inflates the layout. Button, cover and title sizes in the
 * layouts are base sizes for a ~640x400dp screen and get scaled to the actual screen size.
 */
class CompactPlayerView(context: Context, attrs: AttributeSet?) :
    ConstraintLayout(context, attrs), Player.Listener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val POSITION_UPDATE_INTERVAL: Long = 100

        // Screen size (in dp, insets excluded) the base sizes in the layouts are designed for.
        private const val REFERENCE_HEIGHT_DP = 400f
        private const val REFERENCE_WIDTH_DP_SIDE_BY_SIDE = 600f
        private const val REFERENCE_WIDTH_DP_STACKED = 440f
        private const val MIN_SCALE = 0.7f
        private const val MAX_SCALE = 1.6f

        const val PREF_COVER_SIZE = "compact_cover_size"
        const val COVER_SIZE_LARGE = "large"
        const val COVER_SIZE_SMALL = "small"
        const val COVER_SIZE_HIDDEN = "hidden"
    }

    private val activity
        get() = context as MainActivity
    private val instance: MediaBrowser?
        get() = activity.getPlayer()
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    var onCollapse: (() -> Unit)? = null
    var onShowQueue: (() -> Unit)? = null
    var onSwitchLayout: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isUserTracking = false
    private var runnableRunning = false
    private var firstTime = false
    private var cookieCover = false

    @LayoutRes
    private var layoutRes = 0

    // Songs without album art always use the "hidden" layout instead of showing a placeholder.
    private var hasArt = true
    private var artKnown = false // hasArt was determined for probedArtUri
    private var probedArtUri: Uri? = null
    private var artProbe: Disposable? = null
    private var colors: Colors? = null
    private var scalables = emptyList<Scalable>()
    private var appliedScale = 0f

    // Declared before init {} because the first layout inflation can already start it.
    private val positionRunnable = object : Runnable {
        override fun run() {
            if (isShown) updatePosition()
            if (instance?.isPlaying == true && isAttachedToWindow) {
                handler.postDelayed(this, POSITION_UPDATE_INTERVAL)
            } else {
                runnableRunning = false
            }
        }
    }

    private lateinit var coverFrame: MaterialCardView
    private lateinit var cover: TransformableImageView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var slider: Slider
    private lateinit var seekBar: SeekBar
    private lateinit var progressDrawable: SquigglyProgress
    private lateinit var position: TextView
    private lateinit var duration: TextView
    private lateinit var previousButton: MaterialButton
    private lateinit var playButton: MaterialButton
    private lateinit var nextButton: MaterialButton
    private lateinit var collapseButton: MaterialButton
    private lateinit var loopButton: MaterialButton
    private lateinit var shuffleButton: MaterialButton
    private lateinit var playlistButton: MaterialButton
    private lateinit var switchLayoutButton: MaterialButton

    private class Colors(
        val primary: Int,
        val secondary: Int,
        val onSurface: Int,
        val onSurfaceVariant: Int,
        val secondaryContainer: Int,
        val onSecondaryContainer: Int,
        val trackInactive: Int,
        val checkSelector: ColorStateList
    )

    /** A view whose size scales with the screen, with its base sizes from the layout. */
    private class Scalable(
        val view: View,
        val width: Int,
        val height: Int,
        val iconSize: Int,
        val cornerRadius: Int,
        val textSizePx: Float
    )

    init {
        // Swallow touches so they never reach the regular player hidden underneath.
        isClickable = true
        refreshSettings(null)

        activity.controllerViewModel.addRecreationalPlayerListener(activity.lifecycle, this) {
            refreshFromPlayer()
        }
    }

    private fun layoutFor(size: String?) = when (size) {
        COVER_SIZE_SMALL -> R.layout.compact_player_small
        COVER_SIZE_HIDDEN -> R.layout.compact_player_hidden
        else -> R.layout.compact_player_large
    }

    private fun inflateLayout(@LayoutRes res: Int) {
        if (layoutRes != 0) {
            cover.dispose()
            cover.stopRotation()
            removeAllViews()
        }
        layoutRes = res
        inflate(context, res, this)
        bindViews()
        scalables = listOf(
            coverFrame, title, subtitle, previousButton, playButton, nextButton,
            loopButton, shuffleButton, playlistButton
        ).map {
            val lp = it.layoutParams
            Scalable(
                it, lp.width, lp.height,
                (it as? MaterialButton)?.iconSize ?: 0,
                (it as? MaterialButton)?.cornerRadius ?: 0,
                if (it !is MaterialButton && it is TextView) it.textSize else 0f
            )
        }
        appliedScale = 0f
        applyVisualSettings(null)
        colors?.let { applyColors(it) }
        refreshFromPlayer()
        if (width > 0 && height > 0) applyScale(width, height)
    }

    private fun bindViews() {
        coverFrame = findViewById(R.id.compact_cover_frame)
        cover = findViewById(R.id.compact_cover)
        title = findViewById(R.id.compact_song_name)
        subtitle = findViewById(R.id.compact_song_artist)
        slider = findViewById(R.id.compact_slider)
        seekBar = findViewById(R.id.compact_slider_squiggly)
        position = findViewById(R.id.compact_position)
        duration = findViewById(R.id.compact_duration)
        previousButton = findViewById(R.id.compact_previous)
        playButton = findViewById(R.id.compact_play)
        nextButton = findViewById(R.id.compact_next)
        collapseButton = findViewById(R.id.compact_collapse)
        loopButton = findViewById(R.id.compact_loop)
        shuffleButton = findViewById(R.id.compact_shuffle)
        playlistButton = findViewById(R.id.compact_playlist)
        switchLayoutButton = findViewById(R.id.compact_switch_layout)

        // Same squiggly line as the full player (see FullBottomSheet).
        seekBar.progressDrawable = SquigglyProgress().also {
            progressDrawable = it
            it.waveLength = context.resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_wavelength).toFloat()
            it.lineAmplitude = context.resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_amplitude).toFloat()
            it.phaseSpeed = context.resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_phase).toFloat()
            it.strokeWidth = context.resources
                .getDimensionPixelSize(R.dimen.media_seekbar_progress_stroke_width).toFloat()
            it.transitionEnabled = true
            it.animate = false
        }
        seekBar.progressTintList = ColorStateList.valueOf(
            MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary)
        )

        playButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            instance?.playOrPause()
        }
        previousButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            instance?.seekToPrevious()
        }
        previousButton.setOnLongClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.LONG_PRESS)
            instance?.seekBack()
            true
        }
        nextButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            instance?.seekToNext()
        }
        nextButton.setOnLongClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.LONG_PRESS)
            instance?.seekForward()
            true
        }
        loopButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            val player = instance ?: return@setOnClickListener
            player.repeatMode = when (player.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
        shuffleButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            val player = instance ?: return@setOnClickListener
            player.shuffleModeEnabled = !player.shuffleModeEnabled
        }
        collapseButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            onCollapse?.invoke()
        }
        playlistButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            onShowQueue?.invoke()
        }
        switchLayoutButton.setOnClickListener {
            ViewCompat.performHapticFeedback(it, HapticFeedbackConstantsCompat.CONTEXT_CLICK)
            onSwitchLayout?.invoke()
        }

        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isUserTracking = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                if (instance?.currentMediaItem != null) {
                    instance?.seekTo(slider.value.toLong())
                }
                isUserTracking = false
            }
        })
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) onUserSeek(value.toLong())
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) onUserSeek(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserTracking = true
                progressDrawable.animate = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (instance?.currentMediaItem != null) {
                    instance?.seekTo(seekBar.progress.toLong())
                }
                isUserTracking = false
                progressDrawable.animate = instance?.isPlaying == true
            }
        })
    }

    private fun refreshFromPlayer() {
        val player = instance ?: return
        firstTime = true
        onRepeatModeChanged(player.repeatMode)
        onShuffleModeEnabledChanged(player.shuffleModeEnabled)
        onMediaItemTransition(
            player.currentMediaItem,
            Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED
        )
        onIsPlayingChanged(player.isPlaying)
        firstTime = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Changing child layout params during layout is not allowed, do it right after.
        post { applyScale(width, height) }
    }

    private fun applyScale(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val density = resources.displayMetrics.density
        val referenceWidth = if (layoutRes == R.layout.compact_player_large)
            REFERENCE_WIDTH_DP_SIDE_BY_SIDE else REFERENCE_WIDTH_DP_STACKED
        val scale = min(h / density / REFERENCE_HEIGHT_DP, w / density / referenceWidth)
            .coerceIn(MIN_SCALE, MAX_SCALE)
        if (abs(scale - appliedScale) < 0.01f) return
        appliedScale = scale
        for (s in scalables) {
            // Views sized by constraints (0dp, like the large cover) scale with the layout already.
            if (s.width > 0 || s.height > 0) {
                s.view.updateLayoutParams {
                    if (s.width > 0) width = (s.width * scale).toInt()
                    if (s.height > 0) height = (s.height * scale).toInt()
                }
            }
            val v = s.view
            if (v is MaterialButton) {
                v.iconSize = (s.iconSize * scale).toInt()
                if (s.cornerRadius > 0) v.cornerRadius = (s.cornerRadius * scale).toInt()
            } else if (v is TextView && s.textSizePx > 0f) {
                v.setTextSize(TypedValue.COMPLEX_UNIT_PX, s.textSizePx * scale)
            }
        }
    }

    private fun onUserSeek(positionMs: Long) {
        position.text = CalculationUtils.convertDurationToTimeStamp(positionMs)
        // Changes without a touch gesture come from keys (d-pad), seek right away for those.
        if (!isUserTracking && instance?.currentMediaItem != null) {
            instance?.seekTo(positionMs)
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        refreshSettings(key)
    }

    private fun refreshSettings(key: String?) {
        if (key == null || key == PREF_COVER_SIZE) {
            val res = if (hasArt) layoutFor(prefs.getStringStrict(PREF_COVER_SIZE, COVER_SIZE_LARGE))
            else R.layout.compact_player_hidden
            if (res != layoutRes) {
                // Applies all the other settings to the new views too.
                inflateLayout(res)
                return
            }
        }
        applyVisualSettings(key)
    }

    private fun applyVisualSettings(key: String?) {
        if (key == null || key == "default_progress_bar") {
            // Same setting as the full player: on = material slider, off = squiggly line.
            val materialSlider = prefs.getBooleanStrict("default_progress_bar", false)
            slider.isVisible = materialSlider
            seekBar.isVisible = !materialSlider
        }
        if (key == null || key == "album_round_corner") {
            coverFrame.radius = prefs.getIntStrict(
                "album_round_corner",
                context.resources.getInteger(R.integer.round_corner_radius)
            ).dpToPx(context).toFloat()
        }
        if (key == null || key == "cookie_cover") {
            cookieCover = prefs.getBooleanStrict("cookie_cover", false)
            cover.setClip(cookieCover)
            updateCoverRotation()
        }
    }

    // The cookie shape slowly spins while playing, like in the full player.
    private fun updateCoverRotation() {
        if (cookieCover && coverFrame.isVisible && isShown && instance?.isPlaying == true) {
            cover.startRotation()
        } else {
            cover.stopRotation()
        }
    }

    fun applyColors(
        colorPrimary: Int,
        colorSecondary: Int,
        colorOnSurface: Int,
        colorOnSurfaceVariant: Int,
        colorSecondaryContainer: Int,
        colorOnSecondaryContainer: Int,
        colorTrackInactive: Int,
        checkSelector: ColorStateList
    ) {
        // Kept so the colors survive re-inflating for a different album art size.
        val c = Colors(
            colorPrimary, colorSecondary, colorOnSurface, colorOnSurfaceVariant,
            colorSecondaryContainer, colorOnSecondaryContainer, colorTrackInactive, checkSelector
        )
        colors = c
        applyColors(c)
    }

    private fun applyColors(c: Colors) {
        title.setTextColor(c.primary)
        subtitle.setTextColor(c.secondary)
        position.setTextColor(c.onSurfaceVariant)
        duration.setTextColor(c.onSurfaceVariant)
        slider.thumbTintList = ColorStateList.valueOf(c.primary)
        slider.trackActiveTintList = ColorStateList.valueOf(c.primary)
        slider.trackInactiveTintList = ColorStateList.valueOf(c.trackInactive)
        seekBar.progressTintList = ColorStateList.valueOf(c.primary)
        seekBar.thumbTintList = ColorStateList.valueOf(c.primary)
        playButton.backgroundTintList = ColorStateList.valueOf(c.secondaryContainer)
        playButton.iconTint = ColorStateList.valueOf(c.onSecondaryContainer)
        val onSurface = ColorStateList.valueOf(c.onSurface)
        previousButton.iconTint = onSurface
        nextButton.iconTint = onSurface
        collapseButton.iconTint = onSurface
        playlistButton.iconTint = onSurface
        switchLayoutButton.iconTint = onSurface
        loopButton.iconTint = c.checkSelector
        shuffleButton.iconTint = c.checkSelector
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        cover.dispose()
        if ((instance?.mediaItemCount ?: 0) == 0) return
        val artUri = mediaItem?.mediaMetadata?.artworkUri
        checkHasArt(artUri)
        // Coil waits for a size from the view, which a GONE cover (hidden layout) never gets.
        if (coverFrame.isVisible) {
            cover.loadNoPlaceholder(artUri) {
                scale(Scale.FILL)
                error(R.drawable.ic_default_cover)
            }
        }
        title.setTextAnimation(
            mediaItem?.mediaMetadata?.title ?: "",
            skipAnimation = firstTime
        )
        subtitle.setTextAnimation(
            mediaItem?.mediaMetadata?.artist ?: context.getString(R.string.unknown_artist),
            skipAnimation = firstTime
        )
        updatePosition()
    }

    /**
     * Find out if the song has album art with a tiny, fixed size load (works while the cover view is
     * GONE) and switch to/from the hidden layout accordingly.
     */
    private fun checkHasArt(artUri: Uri?) {
        if (artKnown && artUri == probedArtUri) return
        artProbe?.dispose()
        artProbe = null
        artKnown = false
        probedArtUri = artUri
        if (artUri == null) {
            artKnown = true
            setHasArt(false)
            return
        }
        artProbe = context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(artUri)
                .size(64)
                .listener(
                    onSuccess = { _, _ ->
                        artProbe = null
                        artKnown = true
                        setHasArt(true)
                    },
                    onError = { _, _ ->
                        artProbe = null
                        artKnown = true
                        setHasArt(false)
                    }
                )
                .build()
        )
    }

    private fun setHasArt(value: Boolean) {
        if (hasArt == value) return
        hasArt = value
        refreshSettings(PREF_COVER_SIZE)
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        shuffleButton.isChecked = shuffleModeEnabled
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        loopButton.isChecked = repeatMode != Player.REPEAT_MODE_OFF
        loopButton.icon = AppCompatResources.getDrawable(
            context,
            if (repeatMode == Player.REPEAT_MODE_ONE) R.drawable.ic_repeat_one
            else R.drawable.ic_repeat
        )
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        playButton.icon = AppCompatResources.getDrawable(
            context,
            if (isPlaying) R.drawable.ic_pause_filled else R.drawable.ic_play_arrow_filled
        )
        if (!isUserTracking) progressDrawable.animate = isPlaying
        updateCoverRotation()
        updatePosition()
        if (isPlaying && !runnableRunning) {
            runnableRunning = true
            handler.postDelayed(positionRunnable, POSITION_UPDATE_INTERVAL)
        }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        updatePosition()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        updatePosition()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prefs.registerOnSharedPreferenceChangeListener(this)
        refreshSettings(null)
        onIsPlayingChanged(instance?.isPlaying == true)
        // A check cancelled by an earlier detach needs to run again.
        instance?.currentMediaItem?.let { checkHasArt(it.mediaMetadata.artworkUri) }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
        handler.removeCallbacks(positionRunnable)
        runnableRunning = false
        cover.stopRotation()
        artProbe?.dispose()
        artProbe = null
        artKnown = false
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Don't spin the cookie cover while the compact player isn't on screen.
        if (layoutRes != 0) updateCoverRotation()
    }

    private fun updatePosition() {
        if (isUserTracking) return
        val player = instance
        val durationMs = player?.contentDuration?.takeIf { it != C.TIME_UNSET && it > 0 }
            ?: player?.currentMediaItem?.mediaMetadata?.durationMs?.takeIf { it > 0 }
        val positionMs = player?.currentPosition ?: 0
        if (durationMs == null) {
            slider.value = 0f
            slider.valueTo = 1f
            slider.isEnabled = false
            seekBar.max = 1
            seekBar.progress = 0
            seekBar.isEnabled = false
            duration.text = context.getString(R.string.default_time)
        } else {
            // Set value first when shrinking valueTo, Slider throws if value > valueTo.
            val newValueTo = durationMs.toFloat()
            if (newValueTo < slider.valueTo) slider.value = 0f
            slider.valueTo = newValueTo
            slider.value = positionMs.toFloat().coerceIn(0f, newValueTo)
            slider.isEnabled = true
            seekBar.max = durationMs.toInt()
            seekBar.progress = positionMs.toInt()
            seekBar.isEnabled = true
            duration.text = CalculationUtils.convertDurationToTimeStamp(durationMs)
        }
        position.text = CalculationUtils.convertDurationToTimeStamp(positionMs)
    }
}
