# Compact Device Screen Support

*Custom Compact Gramophone: an unofficial fork of [Gramophone](https://github.com/FoedusProgramme/Gramophone).*

> [!WARNING]
> **This change was written by Claude (an AI coding assistant from Anthropic) and has not been reviewed by the Gramophone maintainers.**
> It compiles and builds a debug APK. It has **not** been tested by automated tests, on an emulator, or on real devices yet.
> **Do not merge it upstream without a full human code review and testing on real hardware.**

## Why

Gramophone's now playing screen is designed for tall portrait phones. The album art is a full-width square with the controls stacked below it.

On screens that are not tall, such as 4:3, 3:2 and 1:1 (common on retro handhelds), the square art takes up most of the height. The controls get squashed or pushed off-screen.

The app already has a side-by-side layout (`res/layout-w600dp-land/full_player.xml`). Android only uses it when the screen is in landscape **and** at least 600dp wide. Many small handhelds never meet that. A 720×720 screen, for example, is usually about 360×360dp.

## What this adds

A **compact player**: a separate, simpler now playing screen. It has three layouts, picked by the "Album art" setting (they follow mockups provided by the user):

```
Large
┌────────────────────────────────────────┐
│ ┌────────────┐       Song title        │
│ │            │         Artist          │
│ │ Album art  │  ━━━━━━━━●━━━━━━━━━━━━  │
│ │  (square)  │  1:04             3:54  │
│ │            │       ⏮    ▶    ⏭       │
│ └────────────┘     🔁      🔀      ☰     │
│ ⌄                                    ⧉ │
└────────────────────────────────────────┘

Small
┌────────────────────────────────────────┐
│               ┌────────┐               │
│               │  art   │               │
│               └────────┘               │
│               Song title               │
│                 Artist                 │
│    ━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━    │
│    1:06                        3:54    │
│              ⏮    ⏸    ⏭               │
│ ⌄            🔁    🔀    ☰            ⧉ │
└────────────────────────────────────────┘

Hidden
┌────────────────────────────────────────┐
│               Song title               │
│                 Artist                 │
│    ━━━━━━━━━━●━━━━━━━━━━━━━━━━━━━━━    │
│    1:06                        3:54    │
│            ⏮      ⏸      ⏭             │
│ ⌄          🔁      🔀      ☰          ⧉ │
└────────────────────────────────────────┘

⌄ close  🔁 repeat  🔀 shuffle  ☰ queue  ⧉ switch layout
```

- **Large:** square art on the left (45% of the width). The right column, vertically centered, holds the title, artist, seek bar and times, transport buttons, and repeat/shuffle/queue spread across it.
- **Small:** smaller art centered on top, with the title, artist, full-width seek bar and transport buttons stacked below. Repeat/shuffle/queue are centered on the bottom row.
- **Hidden:** no art. Larger title and transport buttons fill the screen, and repeat/shuffle/queue are centered on the bottom row.
- **Corners:** in all three, **close** is in the bottom-left corner and **switch layout** in the bottom-right.
- **Songs without album art:** the compact player always uses the **Hidden** layout for these, whatever the "Album art" setting is, instead of showing a placeholder image. It switches back when a song with art plays.
- **Scaling:** buttons, the Small cover and the title/artist text scale with the screen size, so the layout fills small and large screens alike (see "Scaling" below).
- **Transport buttons:** previous, play/pause and next. Long-press previous/next to seek back/forward, the same as the full player.
- **Colors:** follow the album art the same way the full player's do, including its background.
- **Shared look settings:** these existing full player settings also apply to the compact player:
  - **Album round corner:** the cover's corner radius.
  - **Cookie cover:** the flower-shaped cover, which slowly spins while music plays.
  - **Default progress bar:** off shows the squiggly progress line, on shows the Material slider.
  - **Content based color:** colors that follow the album art.

### How to use it

- **Switch on the fly:** tap the new **Switch player layout** button. In the full player it's the first button in the bottom row; in the compact player it's the last one. This change only lasts until the app is restarted.
- **Compact-only settings:** in **Settings → Player → Compact player**:
  - **"Compact player by default"** (`compact_player_default`, off by default).
  - **"Album art"** (`compact_cover_size`): Large (default), Small, or Hidden (centered controls).

Features that are only in the full player (lyrics, sleep timer, playback speed, favorite, audio quality info) are one tap away with the switch button.

## How it works

### Files

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/ui/components/CompactPlayerView.kt` | **New.** The compact player view. |
| `app/src/main/res/layout/compact_player_large.xml` | **New.** Layout for "Large" album art (side by side). |
| `app/src/main/res/layout/compact_player_small.xml` | **New.** Layout for "Small" album art (stacked). |
| `app/src/main/res/layout/compact_player_hidden.xml` | **New.** Layout for "Hidden" album art (no cover, big controls). |
| `app/src/main/res/drawable/ic_player_layout.xml` | **New.** Icon for the switch button and the setting. |
| `app/src/main/java/org/akanework/gramophone/ui/components/FullBottomSheet.kt` | Hosts the compact player and adds the `compactMode` switch. |
| `app/src/main/java/org/akanework/gramophone/ui/components/TransformableImageView.kt` | Bug fix: the cookie clip shape is now recalculated whenever the view's size changes (`onSizeChanged`). Before, it was only sized on the first layout, so a cover that was resized (for example the compact "Small" size) kept a too-large cookie shape that cut the image off. This also affects the full player's cover. |
| `app/src/main/res/layout/full_player.xml` | Adds the switch button to the bottom row. |
| `app/src/main/res/layout-w600dp-land/full_player.xml` | Same as above, for the wide layout. |
| `app/src/main/res/xml/settings_player.xml` | Adds a "Compact player" section with `compact_player_default`, `compact_cover_size`, and a note that some full player settings are shared. |
| `app/src/main/res/values/arrays.xml` | Entries and values for the album art size dropdown. |
| `app/src/main/res/values/strings.xml` | New English strings. Other languages fall back to English. |

### Design: a cover on top of the existing player

The existing player (`FullBottomSheet`, about 1,600 lines) was **not** rewritten. Instead, `CompactPlayerView` is added as one extra child of `FullBottomSheet` that fills it completely:

```
PlayerBottomSheet
└── FullBottomSheet          (existing; owns insets, background color, show/hide, alpha)
    ├── …regular player views (unchanged; hidden in compact mode)
    └── CompactPlayerView     (new; shown in compact mode)
```

This keeps the change small:

- **Show/hide and animations are inherited.** The bottom sheet's expand/collapse visibility and fade animations apply to `FullBottomSheet`, so the compact player follows them with no extra code.
- **Insets are inherited.** `FullBottomSheet` already pads itself for the status bar, navigation bar and display cutouts, and the compact player sits inside that padding.
- **The background is inherited.** The compact player has a transparent background, so it shows the album-art-tinted color that `FullBottomSheet` draws.
- **Swapping is instant.** The regular player keeps running in compact mode; it just isn't drawn. Switching back needs no reloading.
- **Full player features are reused.** The compact queue button triggers the full player's own queue button (`bottomSheetPlaylistButton.performClick()`), so it opens the same queue sheet.

### Hiding the regular player in compact mode

The regular player's views are **not** made `GONE`/`INVISIBLE`. Existing code changes their visibility in many places, and changing it here could conflict with that. Instead, `FullBottomSheet` overrides four things while `compactMode` is true:

| Concern | How it's handled |
|---|---|
| Drawing | `drawChild()` skips every child except the compact player. |
| Touch | The compact player has `isClickable = true` and `translationZ = 100f`. That puts it ahead of raised children (such as the cover card) when touches are handed out, and it absorbs taps on empty areas. |
| Focus (D-pad/keyboard) | `addFocusables()` only reports the compact player's focusable views. When switching to compact mode, focus is cleared if it's on a hidden view. |
| Accessibility (TalkBack) | `addChildrenForAccessibility()` only exposes the compact player. |

### `CompactPlayerView` itself

- **Player events:** it listens for playback changes itself, through `controllerViewModel.addRecreationalPlayerListener` (the same mechanism `FullBottomSheet` uses). It handles track changes, play/pause, shuffle, repeat and seeking.
- **Settings:** it reads its own settings and listens for changes while attached (`refreshSettings()`), so changes made in Settings apply right away.
- **Progress bar:** it has both a `SeekBar` with the same `SquigglyProgress` drawable the full player uses, and a Material `Slider`. Only the one picked by `default_progress_bar` is shown. Both are updated every 100ms while music is playing, and only while the view is on screen. The squiggle only animates while playing.
- **D-pad seeking:** if either bar is moved without a touch (for example with the D-pad), it seeks right away.
- **Cover:** it uses `TransformableImageView`, the same view as the full player, so the cookie shape works the same way. The spin only runs while the cookie shape is on, the cover is visible, the compact player is on screen, and music is playing.
- **One layout per album art size:** `compact_player_large/small/hidden.xml` all use the same view IDs. Changing the "Album art" setting re-inflates the view with the matching layout (`inflateLayout()`): it re-binds the views and listeners, then re-applies the settings, the last colors and the player state. The Hidden layout still contains the cover views, set to `GONE`, so the code doesn't need special cases. Each layout can be edited on its own in Android Studio's layout editor.
- **Missing art detection:** on each track change, `checkHasArt()` loads the art at a small fixed size (64px), separately from the cover view. That works even while the cover view is `GONE` in the Hidden layout, where Coil would never get a size from the view. A missing art link or a failed load counts as "no art" and switches to the Hidden layout; a successful load switches back to the layout from the setting. The result is remembered per art link, so re-inflating the layout doesn't check again.
- **Scaling:** sizes in the layouts are base sizes for a screen of about 640×400dp (the mockups' size). After each size change (`onSizeChanged()`), the cover (Small only), title/artist text, transport buttons and repeat/shuffle/queue buttons are scaled by `min(height / 400dp, width / reference width)`, limited to 0.7–1.6. The reference width is 600dp for Large (narrow right column) and 440dp for Small/Hidden. Layout sizes, icon sizes, the play button's corner radius and text sizes are all scaled. The close and switch corner buttons stay 48dp. The Large cover is sized by constraints, so it doesn't need scaling.
- **Colors:** `FullBottomSheet` passes its album-art colors on through `applyColors(...)`, at the end of `applyColorScheme()`.

### State

- **`compactMode` at startup** comes from the `compact_player_default` setting.
- **Changing the setting** while the app runs updates the player immediately, through `refreshSettings()`.
- **Tapping the switch button** changes only the current session. It survives activity recreation (saved as `"Compact"` in `FullBottomSheet.onSaveInstanceState`) but not a full app restart.

## Also in this change: app-wide color themes

This one isn't specific to compact screens. It was asked for alongside the compact work.

Before this, the app's colors came from the system: wallpaper-based Material You colors on Android 12+ (only on devices whose maker Google has allowlisted for dynamic color), or a fixed built-in palette otherwise. There was a Light/Dark/System setting but no color choice.

- **Setting:** **Settings → Appearance → "Color theme"** (`app_accent_color`). Tapping it opens a menu of color swatches: **Device default** (the old behavior, also the default) plus 10 themes: Red, Pink, Purple, Indigo, Blue, Teal, Green, Yellow, Orange and Grey. The settings entry shows the current color as a swatch.
- **Light and dark:** every theme has a full Material 3 light *and* dark color scheme, and follows the existing Light/Dark/System setting. Pure dark still works on top.
- **Works on every device.** The palettes are generated ahead of time and compiled into the app as theme overlays, so they don't depend on the device's dynamic color support. That matters for devices like the ARBOR GT78 (Android 12, not on Material's dynamic color allowlist), where dynamic color is switched off.
- **How it's applied:** `AccentColors.applyTo()` applies the selected overlay to the activity theme before `super.onCreate()`, in `BaseActivity` (every View-based activity) and `BaseComposeActivity`. Changing the setting recreates open activities, so it applies right away.
- **Compose screens:** Compose UI (the queue sheet, licenses, contributors) used the device's dynamic colors directly. With a theme selected, `GramophoneTheme` now builds its color scheme from the activity theme's color attributes instead.
- **Full player:** when "Content based color" is on, and the device supports it, the player screen still takes its colors from the album art. The selected theme applies everywhere else.
- **Not covered:** home screen widgets and the media notification keep their own colors.

### Regenerating the palettes

The colors are generated with Material's own color utilities (`SchemeContent`, the same scheme Material uses for content-based dynamic colors). The generator is at `misc/accent_color_generator/Gen.java`, and it uses the Material library's `classes.jar` from the Gradle cache:

```sh
cd misc/accent_color_generator
unzip -o -q ~/.gradle/caches/modules-2/files-2.1/com.google.android.material/material/1.13.0/*/material-1.13.0.aar classes.jar
ANN=$(find ~/.gradle/caches/modules-2/files-2.1/androidx.annotation/annotation-jvm -name "*.jar" ! -name "*sources*" | head -1)
javac -proc:none -nowarn -cp "classes.jar:$ANN" Gen.java
java -cp "classes.jar:$ANN:." Gen ../../app/src/main/res
rm -f classes.jar *.class
```

To add or change a theme, edit the seed colors in `Gen.java`, add the matching entry in `AccentColors.kt` and a name string, then regenerate.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/logic/ui/AccentColors.kt` | **New.** The list of themes (key, name, seed color, overlay style) and `applyTo()`. |
| `app/src/main/java/org/akanework/gramophone/ui/components/AccentColorPreference.kt` | **New.** The "Color theme" setting and its swatch menu dialog. |
| `app/src/main/res/values/themes_accent_presets.xml` | **New, generated.** One theme overlay per color. |
| `app/src/main/res/values/colors_accent_presets.xml`, `values-night/colors_accent_presets.xml` | **New, generated.** Light and dark colors for each theme. |
| `app/src/main/res/layout/item_accent_color.xml`, `preference_accent_swatch.xml`, `drawable/accent_swatch.xml` | **New.** The swatch menu row and the swatch shown in settings. |
| `misc/accent_color_generator/Gen.java` | **New.** The palette generator. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/BaseActivity.kt` | Applies the theme and recreates the activity on change. |
| `app/src/main/java/org/akanework/gramophone/ui/Compose.kt` | `BaseComposeActivity` applies the theme; `GramophoneTheme` uses the theme's colors when one is selected. |
| `app/src/main/res/xml/settings_appearance.xml` | The "Color theme" entry. |
| `app/src/main/res/values/strings.xml` | Color names. |

## Building

1. Create `package.properties` in the repository root containing `releaseType=SelfBuilt`. This is a standard Gramophone build step, and the file is gitignored.
2. Run `./gradlew :app:assembleDebug`
3. The APK is written to `app/build/outputs/apk/debug/` as `CustomCompactGramophone-<version>-debug.apk`.

The debug build installs as `io.github.handyandii.compactgramophone.debug`.

### Release (minified) build

The `release` build type always needs a signing key; an unsigned `assembleRelease` fails with `SigningConfig "release" is missing required property "storeFile"`. That's existing upstream behavior. Pass your own key through the project's signing properties:

```sh
./gradlew :app:assembleRelease \
  -PAKANE_RELEASE_KEY_ALIAS=<alias> \
  -PAKANE_RELEASE_STORE_FILE=<path to keystore> \
  -PAKANE_RELEASE_STORE_PASSWORD=<store password> \
  -PAKANE_RELEASE_KEY_PASSWORD=<key password>
```

The output goes to `app/build/outputs/apk/release/` as `CustomCompactGramophone-<version>-release.apk`. It is shrunk with R8 (about 13 MB, compared with about 94 MB for the debug build).

- **Testing:** the local debug keystore works (`~/.android/debug.keystore`, alias `androiddebugkey`, password `android`).
- **Publishing:** use your own release keystore instead, because the debug key's password is public. Every update must be signed with the same key.
- **Version name:** it includes the current git commit hash, so commit before building a release.

## Fork identity

So it can't be confused with, or collide with, the official app, this fork uses its own name and application ID:

- **Display name:** "Custom Compact Gramophone" (`fork_app_name` in `values/strings.xml`). It is untranslated on purpose, so it's the same in every language. The upstream-managed translations of `app_name` are left untouched. The launcher label, home screen title, about dialog, about setting and search label use it.
- **Application ID:** `io.github.handyandii.compactgramophone` (debug: `.debug` suffix). Set as the default in `app/build.gradle.kts`; `-PappIdOverride` still works. Content provider authorities, shortcuts and `BuildConfig.APPLICATION_ID` all derive from it.
- **APK file name:** `CustomCompactGramophone-<version>-<type>.apk`.
- **Unchanged on purpose:** the Kotlin code namespace (`org.akanework.gramophone.*`), intent action names and class names. Renaming those would touch every file and make merging upstream updates much harder, with no benefit to users.

## Known limitations and things to check

- **Untested on hardware.** Check these on each target device:
  - expand/collapse animation and back gesture/button
  - switching layouts in both directions
  - the queue sheet opening from the compact player
  - colors updating when the track changes
  - screen rotation, if the device supports it
- **Layouts are approximations of the mockups.** Spacing and sizes were estimated from screenshots and may need tuning in the layout XMLs. In the Small and Hidden mockups the times sit beside the top of the transport buttons; here they sit on their own row directly under the seek bar, so they can't collide with the buttons on narrow (1:1) screens.
- **Scaling limits.** Very small or very large screens hit the 0.7–1.6 limits, so they might not look exactly like the mockups.
- **Shared settings, not separate ones.** Cookie cover, progress bar style and corner radius are shared with the full player rather than set separately for the compact player.
- **Crowded bottom row on narrow phones.** The full player's bottom row now has 6 buttons, so it is slightly squeezed on portrait phones around 360dp wide.
- **Hidden player still does work.** In compact mode the regular player keeps updating and animating (seek bar, marquee text) without being drawn. The cost should be small, but it isn't zero.
- **English only.** The new strings are English only.
- **No automatic switching.** The layout doesn't change automatically based on screen shape; it's chosen with the setting and the switch button.
- **No controller button support.** Play/pause and next/previous on gamepad buttons were looked at but deliberately left out of this change.

---

*Written by Claude (Anthropic) at the user's request. All code described here was AI-generated and needs human review before it is merged anywhere.*
