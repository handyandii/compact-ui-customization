# Custom Compact Gramophone

An unofficial fork of [Gramophone](https://github.com/FoedusProgramme/Gramophone), the lightweight Material 3 music player. It adds a **compact now playing screen** for square and wide screens (4:3, 3:2, 1:1, common on retro handhelds) and **app-wide color themes** that work on every device.

> [!WARNING]
> **Unofficial, AI-assisted fork.** The changes in this fork were written with the help of Claude (an AI coding assistant from Anthropic) and have **not** been reviewed by the Gramophone maintainers. Please don't report bugs from this fork to the upstream Gramophone project. Open an issue here instead.

## What's different from Gramophone

### Compact player
Gramophone's now playing screen is designed for tall portrait phones. On square or wide screens the full-width album art pushes the controls off-screen. The compact player is a separate now playing screen made for those screens:

- **Three layouts**, picked in *Settings → Player → Compact player → Album art*:
  - **Large:** album art on the left, controls on the right
  - **Small:** smaller art on top, controls stacked below
  - **Hidden:** no art, with big centered controls
- **Scales to the screen:** buttons, art and text grow or shrink with the screen size.
- **Songs without album art** automatically use the Hidden layout instead of a placeholder image.
- **Switch anytime:** use the layout button in the corner of either player screen, or set *Compact player by default*.
- **Works with existing settings:** cookie-shaped cover, squiggly progress bar, round corners and album-art based colors.

### Color themes
Pick the app's colors yourself in *Settings → Appearance → Color theme*:

- **10 themes:** Red, Pink, Purple, Indigo, Blue, Teal, Green, Yellow, Orange and Grey, or **Device default** to keep the original behavior.
- **Light and dark:** every theme has a full light and dark version, and follows the Light/Dark/System setting and Pure dark.
- **Works on any device and Android version**, including devices where Android's own dynamic colors (Material You) aren't available.

### Fixes
- The cookie-shaped album cover no longer cuts off the image when the cover changes size.

## Installation
Download the latest APK from this repository's [Releases](../../releases) page and open it on your device. You may need to allow installing apps from unknown sources.

- **Installs separately:** this fork is a separate app (`io.github.handyandii.compactgramophone`), named **Custom Compact Gramophone**. It installs next to the official Gramophone and does not replace or update it, and each app keeps its own settings.
- **Different signing key:** releases are signed with this fork's own key, not Gramophone's.
- **Want the official app?** Get it from [Gramophone's releases](https://github.com/FoedusProgramme/Gramophone/releases/latest), [F-Droid](https://f-droid.org/packages/org.akanework.gramophone/) or [IzzyOnDroid](https://apt.izzysoft.de/fdroid/index/apk/org.akanework.gramophone).

## Building
You need a recent [Android Studio](https://developer.android.com/studio) (or just the Android SDK and JDK 21 for command-line builds).

1. **Get the submodules:**
   ```sh
   git submodule update --init --recursive
   ```
2. **Set the package type:** create `package.properties` in the repository root containing:
   ```
   releaseType=SelfBuilt
   ```
3. **Build a debug APK:**
   ```sh
   ./gradlew :app:assembleDebug
   ```
   Or build and install straight to a USB-connected device with `./gradlew :app:installDebug`.
4. **Build a release APK.** Release builds must be signed, so pass your own keystore:
   ```sh
   ./gradlew :app:assembleRelease \
     -PAKANE_RELEASE_KEY_ALIAS=<alias> \
     -PAKANE_RELEASE_STORE_FILE=<path to keystore> \
     -PAKANE_RELEASE_STORE_PASSWORD=<store password> \
     -PAKANE_RELEASE_KEY_PASSWORD=<key password>
   ```

APKs are written to `app/build/outputs/apk/<debug|release>/CustomCompactGramophone-<version>-<type>.apk`.

For how the compact player and color themes work, the full list of changed files, and known limitations, see [compact_device_screen_support.md](compact_device_screen_support.md).

## FAQ
These come from upstream Gramophone and apply to this fork too.

**Why can't I see songs shorter than 60 seconds?**
Songs shorter than 60 seconds are hidden by default. You can change this in _Three dots > Settings > Behaviour_ (set it to 0 to show all songs).

**I changed the minimum song length setting, but some songs are still missing!**
Make sure you haven't excluded the folder in _Behaviour > Folder blacklist_. Then try rebooting your device and waiting a few minutes, which rescans the system media database the app uses to find songs. If a file is still missing, your Android version may not scan that file type. This is most common with .opus, which is only scanned on Android 10 and later; renaming it to .ogg makes it detectable on Android 6 and later.

**My song isn't playing, or it plays silently with the volume up!**
The app uses the system's media codecs to stay small, so:
- int32 (32-bit) FLAC needs Android 14 or later
- FLAC is officially supported on Android 8 or later, though it often works on older versions
- xHE-AAC needs Android 9 or later
- Dolby Digital (AC-3), Dolby Digital Plus (E-AC-3) and AC-4 need a device with licensed decoders

The exception is ALAC: it plays even without a system decoder, using a small built-in Java decoder.

## Credits
- **[Gramophone](https://github.com/FoedusProgramme/Gramophone)** by the Akane Foundation / FoedusProgramme and all its contributors. Almost all of this app is their work. If you like it, support and star the original project.
- **Fork changes** written with Claude (Anthropic), directed and tested by [handyandii](https://github.com/handyandii).

## License
Like Gramophone, this fork is licensed under the **GNU General Public License v3.0**. See [LICENSE](LICENSE). The complete source code for every release is available in this repository.
