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

- **Setting:** **Settings → Appearance → "Color theme"** (`app_accent_color`). Tapping it opens a menu of color swatches: **Device default** (the old behavior, also the default) plus 10 seed-based themes (Red, Pink, Purple, Indigo, Blue, Teal, Green, Yellow, Orange and Grey) and 3 fixed themes (Black & white, LCD and Dracula, see below). The settings entry shows the current color as a swatch.
- **Light and dark:** every seed-based theme has a full Material 3 light *and* dark color scheme, and follows the existing Light/Dark/System setting. Pure dark still works on top.
- **Works on every device.** The palettes are generated ahead of time and compiled into the app as theme overlays, so they don't depend on the device's dynamic color support. That matters for devices like the ARBOR GT78 (Android 12, not on Material's dynamic color allowlist), where dynamic color is switched off.
- **How it's applied:** `AccentColors.applyTo()` applies the selected overlay to the activity theme before `super.onCreate()`, in `BaseActivity` (every View-based activity) and `BaseComposeActivity`. Changing the setting recreates open activities, so it applies right away.
- **Compose screens:** Compose UI (the queue sheet, licenses, contributors) used the device's dynamic colors directly. With a theme selected, `GramophoneTheme` now builds its color scheme from the activity theme's color attributes instead.
- **Full player:** when "Content based color" is on, and the device supports it, the player screen still takes its colors from the album art. The selected theme applies everywhere else. Fixed themes are the exception: they turn album-art colors off (`AccentColors.isFixedPalette()`, checked in `FullBottomSheet`).
- **Not covered:** home screen widgets and the media notification keep their own colors.

### Fixed themes: Black & white, LCD, Dracula

These three are hand-picked palettes rather than generated from a seed color. They live in `res/values/themes_fixed_presets.xml`, which is **not** generated and can be edited directly.

- **Black & white** (`black_white`): pure black background and surfaces, white text, icons and controls. Always dark.
- **LCD** (`lcd`): grey-green background (`#C4CCB3`) with dark grey text (`#1E211A`), like an old monochrome LCD screen. Selected items use a dark bar with light text. Always light.
- **Dracula** (`dracula`): the [Dracula](https://draculatheme.com) palette, with a `#282A36` background, `#F8F8F2` text and purple, pink and cyan accents. Always dark.

Each one has only one set of colors, so its `Preset.nightMode` forces the activity into dark or light mode. `BaseActivity` and `BaseComposeActivity` set it as the AppCompat local night mode in `attachBaseContext()`, so the app's Light/Dark/System setting is ignored while one of these themes is selected. The overlays also set `android:textColorPrimary`, `textColorSecondary` and `textColorTertiary`, because parts of the app (the seek bar, for example) use those instead of the Material color roles.

**Player control colors.** A theme can also set optional player-only colors, declared in `res/values/attrs_player_accents.xml`:

| Attribute | Colors |
|---|---|
| `playerPlayButtonColor`, `playerOnPlayButtonColor` | Play button background and icon |
| `playerIconColor` | Previous/next and the other player icons |
| `playerToggleOnColor`, `playerToggleOffColor` | Repeat/shuffle when on and off |

- **How they're read:** `FullBottomSheet.applyColorScheme()` reads them with `AccentColors.playerColor()` and passes them on to the compact player.
- **Default:** anything a theme doesn't set keeps the normal Material color.
- **Only Dracula sets them so far:** green play button, cyan icons, and green repeat/shuffle when on (comment blue when off).
- **Why separate attributes:** these icons otherwise use `colorOnSurface`, the same color as all the app's text, so the player can't be colored through the normal color roles.

To add another fixed theme, add a style to `themes_fixed_presets.xml` with the same attributes as the existing ones, then add a `Preset` with a `nightMode` in `AccentColors.kt` and a name string.

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
| `app/src/main/res/values/themes_fixed_presets.xml` | **New, hand-written.** The Black & white, LCD and Dracula overlays. |
| `app/src/main/res/values/attrs_player_accents.xml` | **New.** Optional player control color attributes for themes. |
| `app/src/main/res/layout/item_accent_color.xml`, `preference_accent_swatch.xml`, `drawable/accent_swatch.xml` | **New.** The swatch menu row and the swatch shown in settings. |
| `misc/accent_color_generator/Gen.java` | **New.** The palette generator. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/BaseActivity.kt` | Applies the theme (and a fixed theme's night mode) and recreates the activity on change. |
| `app/src/main/java/org/akanework/gramophone/ui/Compose.kt` | `BaseComposeActivity` applies the theme; `GramophoneTheme` uses the theme's colors when one is selected. |
| `app/src/main/res/xml/settings_appearance.xml` | The "Color theme" entry. |
| `app/src/main/res/values/strings.xml` | Color names. |

## Also in this change: app-wide fonts

**Settings → Appearance → "Font"** (`app_font`), right under Color theme. The menu shows each font name in that font, and the settings entry shows an "Aa" sample in the current font.

- **Device default** (`system`): the old behavior, and the default.
- **Built-in fonts:** Atkinson Hyperlegible, Nunito, Space Grotesk, JetBrains Mono, Pixelify Sans (pixel) and VT323 (terminal). They're bundled in `res/font/` as static Regular (400) and Bold (700) TTFs; VT323 only has Regular. Other weights use the closest one, and Android fakes bold where needed. All six are under the SIL Open Font License, and their license files are in `misc/font_licenses/`. The TTFs were downloaded from Google Fonts unmodified.
- **Imported fonts:** the last menu entry, "Import font file…", opens the system file picker. The chosen file is copied into the app's private storage (`filesDir/fonts/`) and listed in the menu by file name, with an X button to remove it again. The file must parse as a font (`Typeface.Builder` on Android 8+, `Typeface.createFromFile` before that) and be at most 32MB. The picker accepts any file type, because font files don't have a reliable MIME type across devices. Removing a font only deletes the app's copy. If the selected font is removed, the app switches back to Device default.
- **Missing characters** (CJK titles in a pixel font, for example) fall back to the system font automatically.

### How it's applied

A theme overlay can't point at a file on disk, so fonts don't use theme overlays the way colors do. Instead `AppFonts.typeface()` returns the selected `Typeface` (cached per setting value, `null` for Device default), and it's applied in these places:

- **`ViewCompatInflater`** (the app's existing custom view inflater, set with `viewInflaterClass` in the theme). After it applies `textFontWeight`, it calls `AppFonts.applyTo()` on every inflated `TextView`, keeping that view's weight and italic style. This covers the 61 `android:fontFamily="sans-serif"` attributes in upstream layouts without editing them. `TypefaceCompatTextView.setTextAppearance()` re-applies it, because tabs and other views change their text appearance after inflation.
- **Toolbar titles:** `Toolbar` creates its title views itself, so the inflater adds a hierarchy listener to toolbars and sets the font on title views when they're added.
- **Collapsing toolbar titles:** these are drawn by `CollapsingToolbarLayout`, which gets the font through `setCollapsedTitleTypeface` and `setExpandedTitleTypeface`.
- **Fonts set in code:** the bold title switch in `FullBottomSheet` and both lyrics views used `TypefaceCompat.create(context, null, weight, false)`. They now use `AppFonts.create(context, weight)`.
- **Compose:** `GramophoneTheme` passes a Material 3 `Typography` with every style in the app font.
- **Changing the font** recreates open activities (`BaseActivity`), the same as Color theme.
- **Not covered:** home screen widgets and the media notification (RemoteViews can't use custom fonts), and any text view created in code that isn't one of the cases above.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/logic/ui/AppFonts.kt` | **New.** Built-in font list, loading and caching, applying a font while keeping the weight, and importing and removing font files. |
| `app/src/main/java/org/akanework/gramophone/ui/components/FontPreference.kt` | **New.** The "Font" setting and its menu. |
| `app/src/main/res/font/` | **New.** The built-in TTFs and a `font_<name>.xml` family file for each. |
| `misc/font_licenses/` | **New.** OFL license file for each built-in font. |
| `app/src/main/res/layout/item_app_font.xml`, `preference_font_preview.xml` | **New.** The font menu row and the sample shown in settings. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/ViewCompatInflater.kt` | Applies the font to inflated text views, toolbars and collapsing toolbars. |
| `FullBottomSheet.kt`, `NewLyricsView.kt`, `LegacyLyricsAdapter.kt` | Use `AppFonts.create()` where they built typefaces in code. |
| `ui/Compose.kt` | App font `Typography` in `GramophoneTheme`. |
| `ui/fragments/settings/AppearanceSettingsFragment.kt` | The file picker for importing fonts. |

To add a built-in font, put its Regular and Bold TTFs in `res/font/` as `<name>_400.ttf` and `<name>_700.ttf`, add a `font_<name>.xml` family file like the existing ones, add a `Preset` in `AppFonts.kt` and a name string, and add its license to `misc/font_licenses/`.

## Also in this change: game controller buttons

For handhelds with built-in gamepads. While the app is open, controller buttons can play/pause, go to the previous track, go to the next track, open/close now playing, switch layout, change the repeat mode and toggle shuffle. By default these are **Start**, **L1**, **R1**, **X** and **Select**; repeat and shuffle have no button until the user sets one. They work on every screen of the main window, whichever view has focus.

- **Settings:** **Settings → Behavior → Game controller**:
  - **"Controller playback buttons"** (`controller_buttons`) turns the feature on or off. It's on by default.
  - Below it is a list with one row per action (Play/pause, Previous track, Next track, Open/close now playing, Switch layout, Repeat mode, Shuffle). Each row shows its current button and an edit icon. The rows are disabled while the switch is off.
- **Changing a button:** tap a row to open a popup showing the current button.
  - **Set** removes the current button immediately, then waits for a button press. The next button pressed (on key down, repeats ignored) takes over the action. If another action used that button, it loses it and is left without a button, and a toast says so.
  - **Cancel** stops waiting. The action stays without a button until one is set.
  - **Default** puts back the action's default button.
  - Back, Home, Power, app switch and the volume keys can't be bound. Back still cancels the popup, and volume keeps working.
- **Open/close now playing:** expands the player sheet if it's collapsed (a song is loaded), or collapses it if it's open. **B** (Back) also closes it.
- **Repeat mode / Shuffle:** cycle repeat off → all → one (like the player's repeat button) and toggle shuffle. A short toast says the new state, unless the player is open (it shows the state itself). Unbound by default (`UNSET`); **Default** in their popup puts them back to no button, and doesn't count as a conflict with other unbound actions.
- **Switch layout:** with the now playing screen open, it swaps between the compact and the full player. Anywhere else on the home screen, it swaps between the home menu and the tabs. Behind other screens (search, an album, and so on) it does nothing. Like the on-screen switch button, it doesn't change the "by default" settings.
- **Prefs:** `controller_key_play_pause`, `controller_key_previous`, `controller_key_next`, `controller_key_now_playing`, `controller_key_switch_layout`, `controller_key_repeat` and `controller_key_shuffle` are ints holding Android key codes, with `KEYCODE_UNKNOWN` (0) meaning no button. Button names come from `ControllerButtons.keyName()`: gamepad buttons get short names (A, B, X, Y, L1, R1, L2, R2, L3, R3, Start, Select), and any other key uses its Android key name.
- **How it works:** `MainActivity.dispatchKeyEvent()` passes every key event to `ControllerButtons.handle()` first, along with a `ControllerButtons.Host` that gives the actions the player, `switchLayout()`, `toggleNowPlaying()` and `notify()` (the toast). If the key is bound to an action, it runs the action once on key down and swallows the key up and repeats, so nothing else reacts to that button. Previous uses `seekToPrevious()`, the same as the on-screen previous button, so it restarts the current track if it's a few seconds in, and it follows the "Always skip to previous" setting.
- **Limits:**
  - Bound buttons only work while the app is in the foreground, and only in the main window (not in settings, or while a dialog is open).
  - Binding a D-pad or A button takes it away from normal navigation in the main window. To fix that, rebind it or turn the feature off.
  - On some handhelds the B button reaches the app as Back, so it can't be bound there.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/logic/ui/ControllerButtons.kt` | **New.** The actions with their default buttons and pref keys, button names, and key handling. |
| `app/src/main/java/org/akanework/gramophone/ui/components/ControllerButtonPreference.kt` | **New.** One action row in settings and the popup for changing its button. |
| `app/src/main/res/layout/preference_controller_edit.xml` | **New.** The edit icon on each row. |
| `app/src/main/java/org/akanework/gramophone/ui/MainActivity.kt` | `dispatchKeyEvent()` hook and the `ControllerButtons.Host` (player, switch layout). |
| `app/src/main/res/xml/settings_behavior.xml` | The "Game controller" category: the switch and the seven action rows. |

## Also in this change: home menu

An alternative to the library tabs that is easier to use with a game controller. Instead of a row of tabs, the home screen shows four big tiles with icons: **Artists**, **Albums**, **Songs** and **Playlists**. Picking a tile opens that category's usual list, and Back (or **B**, which Android turns into Back for gamepads) returns to the tiles. The D-pad moves between tiles and **A** opens one.

- **Settings:** **Settings → Appearance → Home screen → "Home menu by default"** (`home_menu`, boolean, off by default) picks what the app starts with. The home screen's overflow menu (**Show home menu** / **Show tabs**) and the controller's **Switch layout** button switch for the current session only, the same way the compact player's switch button works alongside "Compact player by default". Changing the setting applies it right away.
- **Layout:** the tiles are 4 × 1 on screens wider than tall (like the ARBOR's 3:2) and 2 × 2 on tall screens. The big title collapses while the tiles show, to make room for them. A focused tile gets a thick outline in `colorOnSurface`, so it stands out in every color theme.
- **Inside a category:** the tabs are hidden and swiping between pages is off, the title shows the category name, and the toolbar shows a back arrow for touch users.
- **Hidden tabs:** tiles only show for categories whose tab is enabled in *Tab order*, because a hidden tab has no page to open.
- **How it works:** `HomeMenu` lives inside `ViewPagerFragment` and covers its `ViewPager2`. Picking a tile selects that tab's page and shows the pager again, so the regular list fragments are reused unchanged (`AdapterFragment` needs `ViewPagerFragment` as its parent, so it can't be opened on its own). The pager is made `INVISIBLE`, not `GONE`, while the tiles show, so the lists keep loading and keep their scroll position.
- **Back handling:** `HomeMenu` adds an `OnBackPressedCallback` that is enabled only in a category, in home menu mode, and while `ViewPagerFragment` isn't hidden behind another fragment (search, an album, and so on). The player's own back callback is added later (when the bottom sheet is first laid out), so Back still collapses an expanded player first.
- **Focus:** the focused tile is filled with `colorPrimary`, outlined in `colorOnSurface` and enlarged 5% (`HomeMenu.styleTile()`, swapping `home_menu_tile_background` and `home_menu_tile_background_focused`). This is set from a focus listener rather than a drawable state selector, which didn't show on the ARBOR.
- **Title:** **Settings → Appearance → Home screen → "Home screen title"** (`home_title`, string, empty = the app name) sets the big title on the home screen. `HomeTitlePreference` is a text dialog with OK, Cancel and Default; `HomeMenu` applies it (and the category names in home menu mode).
- **State:** whether the home menu is on (`home_menu_enabled`) and whether the tiles or a category is showing (`home_menu_shown`) are saved in the fragment's instance state, so a session switch survives rotation and theme changes.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/ui/components/HomeMenu.kt` | **New.** Tiles, switching between tiles and lists, back handling, grid orientation. |
| `app/src/main/res/layout/home_menu.xml`, `home_menu_tile.xml` | **New.** The tile grid and one tile. |
| `app/src/main/res/drawable/home_menu_tile_background.xml` | **New.** Tile background with ripple and focus outline. |
| `app/src/main/java/org/akanework/gramophone/ui/components/HomeTitlePreference.kt`, `res/layout/dialog_home_title.xml` | **New.** The "Home screen title" setting and its dialog. |
| `app/src/main/res/drawable/home_menu_tile_background_focused.xml` | **New.** Focused tile background. |
| `app/src/main/res/drawable/ic_music_note.xml` | **New.** Songs icon (Material Symbols). The other tiles reuse `ic_groups`, `ic_album` and `ic_playlist_play`. |
| `app/src/main/java/org/akanework/gramophone/ui/fragments/ViewPagerFragment.kt` | Creates `HomeMenu`; overflow menu item; `toggleHomeMenu()` for the controller button; `onHiddenChanged` and `onSaveInstanceState` hooks. |
| `app/src/main/java/org/akanework/gramophone/ui/adapters/ViewPager2Adapter.kt` | `positionOf(id)`, the page of a tab or -1 if it's hidden. |
| `app/src/main/res/layout/fragment_viewpager.xml` | Includes `home_menu.xml`. |
| `app/src/main/res/menu/home_menu.xml` | The **Show home menu** / **Show tabs** item. |
| `app/src/main/res/xml/settings_appearance.xml` | The "Home menu" switch. |

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

## Also in this change: D-pad focus

Controller navigation outside the home menu needed a few fixes, since most of the app's views have no visible focused state:

- **Focus outline:** `logic/ui/FocusHighlight.kt`, installed for every activity in `BaseActivity.onPostCreate()` (the main screen and all settings pages), draws a rounded `colorOnSurface` outline with a light fill in the overlay of whichever view has focus. It only shows outside touch mode (after a D-pad or key press), so touch use looks unchanged. **Settings → Behavior → Game controller → "High contrast focus"** (`focus_high_contrast`, boolean, off by default) controls the outline: with it off, only the light shade marks the focused view; views whose background or foreground is a `RippleDrawable` (toolbar buttons, Material buttons) get no shade at all, since their ripple already shows focus (otherwise there were two highlights, a circle and a rounded square); and focused home menu tiles keep their fill and enlargement but lose their outline (`home_menu_tile_background_focused_subtle.xml`). It's read on every focus change, so it applies right away. Views that draw their own focus state set the `R.id.focus_highlight_skip` tag (`res/values/ids_focus_highlight.xml`), as the home menu tiles do.
- **List rows:** in `BaseAdapter`, the ⋮ button on each row is no longer focusable, so the D-pad moves between whole rows (in the 2-column grid, focus used to stay in the ⋮ column). The row's menu is now on a long press in every layout, not only the grid, so holding **A** opens it. Touch can still use the ⋮ button.
- **Toolbars:** Material's `AppBarLayout` style (`Widget.Design.AppBarLayout`) and AppCompat's Toolbar style on API 26+ (`Base.V26.Widget.AppCompat.Toolbar`) both set `touchscreenBlocksFocus` with `keyboardNavigationCluster`. On touchscreen devices that makes each one a trap for D-pad navigation: the app bar kept focus out of the toolbar, and the toolbar kept it in once there (only keyboard cluster navigation crosses them). `ViewCompatInflater` turns `touchscreenBlocksFocus` off for every inflated `AppBarLayout` and `Toolbar`, so the home screen's Search/Settings/⋮ and the settings pages' back arrow can be reached with the D-pad.
- **Getting to settings:** the Settings item in the home screen toolbar is now an always-visible icon (`showAsAction="ifRoom"` in `res/menu/home_menu.xml`) instead of an overflow entry, so it's one D-pad press up from the tiles or the tabs. Settings pages are preference lists, which already move by D-pad; A opens or toggles a row, B goes back. Dialogs with lists (color theme, font, tab order) use Android's list highlight. The Contributors and Open source licenses pages are Compose and don't get the outline.
- **Now playing focus colors:** instead of the outline, a focused control in the full and compact player changes color: buttons are filled with the focus color with the icon in the matching "on" color, and a focused seek bar's thumb (and the slider's halo) takes the focus color. `ui/components/PlayerFocusColors.kt` saves the control's normal tints on focus and restores them on blur; `FullBottomSheet.applyColorScheme()` calls `update()` and `refresh()` after recoloring (track changes), and `CompactPlayerView` re-attaches after re-inflating. The colors come from the theme attributes `playerFocusColor`/`playerOnFocusColor` (`attrs_player_accents.xml`):
  - **Generated themes:** the theme's own `tertiary`/`onTertiary`, added to every overlay by `Gen.java`.
  - **Black & white:** white with a black icon (inverted). **LCD:** `#7D8568` with a `#1E211A` icon, a half-lit segment (inverting wouldn't show on the already-darkest play button). **Dracula:** pink `#FF79C6` with a `#282A36` icon, apart from the green play button and cyan icons.
  - **Device default, or album art colors on:** the player palette's `colorTertiary`/`colorOnTertiary`, so focus matches what the player shows.
- **Now playing:** while the player sheet is expanded, `PlayerBottomSheet.updateFocusForState()` sets the fragment container (the library behind the player, which upstream leaves visible) to `FOCUS_BLOCK_DESCENDANTS`, so the D-pad stays inside the player, and moves focus to play/pause. Collapsing restores the container and puts focus back on the view that had it before. `FullBottomSheet.onRequestFocusInDescendants()` sends focus to play/pause of whichever layout is showing (`CompactPlayerView.focusPlayButton()` in compact mode), so the hidden regular player's views never get focus. Switching layout with the controller re-focuses play/pause too.
- **Scrolling titles:** `MarqueeTextView` is no longer focusable, so the D-pad doesn't stop on title text in the mini player and the compact player. The marquee still runs, because the view's `isFocused()` always returns true.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/logic/ui/FocusHighlight.kt` | **New.** The focus outline. |
| `app/src/main/res/values/ids_focus_highlight.xml` | **New.** The opt-out tag key. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/BaseActivity.kt` | Installs `FocusHighlight` in `onPostCreate()`. |
| `app/src/main/res/menu/home_menu.xml` | Settings shown as a toolbar icon. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/ViewCompatInflater.kt` | `touchscreenBlocksFocus = false` on app bars and toolbars. |
| `app/src/main/java/org/akanework/gramophone/ui/adapters/BaseAdapter.kt` | ⋮ not focusable; long press menu in every layout. |
| `app/src/main/java/org/akanework/gramophone/ui/components/MarqueeTextView.kt` | Not focusable. |
| `app/src/main/java/org/akanework/gramophone/ui/components/PlayerBottomSheet.kt` | `updateFocusForState()`: focus stays in the open player and returns afterwards. |
| `app/src/main/java/org/akanework/gramophone/ui/components/FullBottomSheet.kt` | `onRequestFocusInDescendants()`: start on play/pause; focus colors attached and refreshed in `applyColorScheme()`. |
| `app/src/main/java/org/akanework/gramophone/ui/components/PlayerFocusColors.kt` | **New.** Focus shown by color in the player. |
| `app/src/main/res/values/attrs_player_accents.xml`, `themes_fixed_presets.xml`, `themes_accent_presets.xml` (generated), `misc/accent_color_generator/Gen.java` | `playerFocusColor`/`playerOnFocusColor` for every theme. |

## Also in this change: hide navigation bar

**Settings → Appearance → "Hide navigation bar"** (`hide_navigation_bar`, boolean, off by default) hides Android's navigation bar (back/home/recents) in immersive mode, for more screen space on small handhelds. A swipe from the screen edge shows it for a moment (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`). The status bar stays. `logic/ui/SystemBars.kt` applies it from `BaseActivity` (after `onCreate`, whenever the window regains focus, since dialogs and the system can bring the bar back, and when the setting changes), so it covers the main screen and all settings pages. The app is already edge-to-edge and pads by the current insets, so layouts use the freed space. The Compose pages (Contributors, Open source licenses) don't apply it.

| File | Change |
|---|---|
| `app/src/main/java/org/akanework/gramophone/logic/ui/SystemBars.kt` | **New.** Shows or hides the navigation bar. |
| `app/src/main/java/org/akanework/gramophone/logic/ui/BaseActivity.kt` | Applies it on create, on window focus and on setting change. |
| `app/src/main/res/xml/settings_appearance.xml` | The "Hide navigation bar" switch. |

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
- **Controller buttons on other devices.** The defaults (Start, L1, R1, X, Select) use the standard Android gamepad key codes. Some handhelds send different codes, so check the defaults on each device. Buttons can be rebound in settings.
- **Home menu.** Check on each device: D-pad focus lands on a tile, B/Back returns from a category to the tiles (and collapses an expanded player first), and the tiles fit next to the mini player. Only the four shortcut categories have tiles; Genres, Dates and Folders are only reachable from the tabs.
- **Fonts with unusual sizes.** Wide or tall fonts (VT323, Pixelify Sans, imported fonts) can overflow tight rows, especially in the compact player. Check the compact layouts with each font.

---

*Written by Claude (Anthropic) at the user's request. All code described here was AI-generated and needs human review before it is merged anywhere.*
